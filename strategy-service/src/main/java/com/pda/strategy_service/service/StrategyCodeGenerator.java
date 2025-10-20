package com.pda.strategy_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 전략 코드 생성기 (Strategy Code Generator)
 * MongoDB의 전략 JSON을 독립 실행 가능한 Python 코드로 변환
 */
@Slf4j
@Service
public class StrategyCodeGenerator {

  private Set<String> timeframes;
  private Long memberId;
  private String symbol;

  /**
   * 안전한 숫자 변환 (String/Number → int)
   */
  private int safeToInt(Object value, int defaultValue) {
    if (value == null)
      return defaultValue;
    if (value instanceof Number)
      return ((Number) value).intValue();
    if (value instanceof String) {
      try {
        return Integer.parseInt((String) value);
      } catch (NumberFormatException e) {
        return defaultValue;
      }
    }
    return defaultValue;
  }

  /**
   * 안전한 숫자 변환 (String/Number → double)
   */
  private double safeToDouble(Object value, double defaultValue) {
    if (value == null)
      return defaultValue;
    if (value instanceof Number)
      return ((Number) value).doubleValue();
    if (value instanceof String) {
      try {
        return Double.parseDouble((String) value);
      } catch (NumberFormatException e) {
        return defaultValue;
      }
    }
    return defaultValue;
  }

  /**
   * 전략 JSON → Python 코드 생성
   */
  public String generateCode(Map<String, Object> strategyJson, Long memberId, String symbol) {
    this.timeframes = analyzeTimeframes(strategyJson);
    this.memberId = memberId;
    this.symbol = symbol;

    String strategyName = (String) strategyJson.getOrDefault("strategy_name", "unknown");
    @SuppressWarnings("unchecked")
    Map<String, Object> buyConfig = (Map<String, Object>) strategyJson.get("buy");
    @SuppressWarnings("unchecked")
    Map<String, Object> sellConfig = (Map<String, Object>) strategyJson.get("sell");

    Map<String, Object> buyNode = null;
    int buyQuantity = 0;
    if (buyConfig != null) {
      @SuppressWarnings("unchecked")
      Map<String, Object> node = (Map<String, Object>) buyConfig.get("node");
      buyNode = node;
      buyQuantity = buyConfig.containsKey("orderQuantity")
          ? safeToInt(buyConfig.get("orderQuantity"), 0)
          : 0;
    }

    Map<String, Object> sellNode = null;
    int sellQuantity = 0;
    if (sellConfig != null) {
      @SuppressWarnings("unchecked")
      Map<String, Object> node = (Map<String, Object>) sellConfig.get("node");
      sellNode = node;
      sellQuantity = sellConfig.containsKey("orderQuantity")
          ? safeToInt(sellConfig.get("orderQuantity"), 0)
          : 0;
    }

    // 코드 조합
    StringBuilder code = new StringBuilder();
    code.append(genHeader(strategyName)).append("\n\n");
    code.append(genDataSource()).append("\n\n");
    code.append(genHelpers()).append("\n\n");

    if (buyNode != null) {
      code.append(genConditionFunc("check_buy_signal", buyNode)).append("\n\n");
    }
    if (sellNode != null) {
      code.append(genConditionFunc("check_sell_signal", sellNode)).append("\n\n");
    }

    code.append(genOrderSender()).append("\n\n");
    code.append(genMain(buyNode != null, sellNode != null, buyQuantity, sellQuantity));

    return code.toString();
  }

  /**
   * JSON에서 timeframe 추출 (재귀)
   */
  private Set<String> analyzeTimeframes(Object obj) {
    Set<String> result = new HashSet<>();

    if (obj instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) obj;
      if (map.containsKey("timeframe")) {
        result.add((String) map.get("timeframe"));
      }
      for (Object value : map.values()) {
        result.addAll(analyzeTimeframes(value));
      }
    } else if (obj instanceof List) {
      for (Object item : (List<?>) obj) {
        result.addAll(analyzeTimeframes(item));
      }
    }

    return result;
  }

  /**
   * 헤더 생성
   */
  private String genHeader(String name) {
    return String.format("""
        import os
        import json
        import time
        import redis
        import requests
        from datetime import datetime
        from confluent_kafka import Consumer, KafkaError

        STRATEGY_ID = int(os.getenv("STRATEGY_ID", "0"))
        SYMBOL = os.getenv("SYMBOL", "%s")
        MEMBER_ID = int(os.getenv("MEMBER_ID", "%d"))
        REDIS_HOST = os.getenv("REDIS_HOST", "redis-master.redis")
        REDIS_PORT = int(os.getenv("REDIS_PORT", "6379"))
        REDIS_PASSWORD = os.getenv("REDIS_PASSWORD", "")
        KAFKA_BROKERS = os.getenv("KAFKA_BROKERS", "my-cluster-kafka-bootstrap.kafka:9092")
        TRADING_SERVICE_URL = os.getenv("TRADING_SERVICE_URL", "http://trading-service.backend:8082")
        STRATEGY_SERVICE_URL = os.getenv("STRATEGY_SERVICE_URL", "http://strategy-service.backend:8081")

        print("=" * 60)
        print(f"전략: %s")
        print(f"종목: {SYMBOL}, 회원: {MEMBER_ID}")
        print(f"Kafka: {KAFKA_BROKERS}")
        print("=" * 60)""", symbol, memberId, name);
  }

  /**
   * 데이터 소스 생성 (Redis, Kafka)
   */
  private String genDataSource() {
    boolean hasDaily = timeframes.contains("1d");
    List<String> kafkaTfs = timeframes.stream()
        .filter(tf -> !"1d".equals(tf))
        .sorted()
        .toList();

    StringBuilder code = new StringBuilder("latest_data = {}\nprev_data = {}\n");

    if (hasDaily) {
      code.append("""

          try:
              redis_client = redis.Redis(
                  host=REDIS_HOST,
                  port=REDIS_PORT,
                  password=REDIS_PASSWORD if REDIS_PASSWORD else None,
                  db=0,
                  decode_responses=True
              )
              redis_client.ping()
              print("✅ Redis 연결")
          except Exception as e:
              print(f"❌ Redis 실패: {e}")
              redis_client = None""");
    } else {
      code.append("\nredis_client = None");
    }

    if (!kafkaTfs.isEmpty()) {
      List<String> topics = new ArrayList<>();
      for (String tf : kafkaTfs) {
        // candles 토픽 (모든 타임프레임)
        topics.add(String.format("'candles.%s.%s'", symbol, tf));

        // indicators 토픽 (tick 제외)
        if (!"tick".equals(tf)) {
          topics.add(String.format("'indicators.%s.%s'", symbol, tf));
        }
      }
      String topicsStr = "[" + String.join(", ", topics) + "]";
      code.append(String.format("""


          try:
              topics = %s
              kafka_consumer = Consumer({
                  'bootstrap.servers': KAFKA_BROKERS,
                  'group.id': f'strategy-{SYMBOL}',
                  'auto.offset.reset': 'latest',
                  'enable.auto.commit': True
              })
              kafka_consumer.subscribe(topics)
              print(f"✅ Kafka 구독: {topics}")
          except Exception as e:
              print(f"❌ Kafka 실패: {e}")
              kafka_consumer = None""", topicsStr));
    } else {
      code.append("\nkafka_consumer = None");
    }

    return code.toString();
  }

  /**
   * 헬퍼 함수 생성
   */
  private String genHelpers() {
    return """
        def get_price(field, timeframe, lookback=0):
            field_map = {'open': 'o', 'high': 'h', 'low': 'l', 'close': 'c', 'volume': 'v'}
            key = field_map.get(field, field)

            if timeframe == "1d":
                if not redis_client: return None
                try:
                    data = redis_client.lindex(f"C:{SYMBOL}:1d", -(lookback + 1))
                    return float(json.loads(data).get(key, 0)) if data else None
                except: return None
            elif timeframe == "tick":
                # tick 타임프레임은 OHLC가 아닌 price 필드 사용
                candles_topic = f"candles.{SYMBOL}.tick"
                cache = prev_data.get(f"prev_tick", {}) if lookback > 0 else latest_data.get(candles_topic, {})

                if not cache:
                    return None

                # tick 데이터는 open/high/low/close 모두 price로 매핑
                if field in ['open', 'high', 'low', 'close']:
                    return float(cache.get('price', 0)) if cache.get('price') else None
                elif field == 'volume':
                    return float(cache.get('volume', 0)) if cache.get('volume') else None
                else:
                    return float(cache.get(field, 0)) if cache.get(field) else None
            else:
                # 먼저 candles 토픽에서 시도
                candles_topic = f"candles.{SYMBOL}.{timeframe}"
                indicators_topic = f"indicators.{SYMBOL}.{timeframe}"

                cache = None
                if lookback > 0:
                    cache = prev_data.get(f"prev_{timeframe}", {})
                else:
                    # candles 토픽에서 데이터 찾기
                    candles_data = latest_data.get(candles_topic, {})
                    if candles_data and key in candles_data:
                        cache = candles_data
                    else:
                        # candles에 없으면 indicators 토픽에서 찾기
                        cache = latest_data.get(indicators_topic, {})

                return float(cache.get(key, 0)) if cache and cache.get(key) else None

        def get_indicator(name, period, timeframe, subfield=None, lookback=0):
            # tick 타임프레임에서는 지표 계산 불가
            if timeframe == "tick":
                return None

            if name == "BOLLINGER_BANDS":
                ind_key = f"bb_{period}_{subfield}" if subfield else f"bb_{period}_middle"
            elif name == "BOLLINGER_BANDWIDTH" or name == "BOLLINGER_BANDSWIDTH":
                ind_key = f"bb_width_{period}"
            elif name == "EMA": ind_key = f"ema{period}"
            elif name == "SMA": ind_key = f"sma{period}"
            elif name == "RSI":
                ind_key = f"rsi{period}_{subfield}" if subfield else f"rsi{period}"
            elif name == "MACD":
                ind_key = {"line": "macd_line", "signal": "macd_signal", "histogram": "macd_histogram"}.get(subfield, "macd_line")
            elif name == "VWAP":
                ind_key = f"vwap_{subfield}" if subfield else "vwap"
            elif name == "RELATIVE_VOLUME" or name == "RVOL":
                ind_key = f"rvol_{period}"
            elif name == "OPENING_RANGE":
                ind_key = f"or_{period}_{subfield}" if subfield else f"or_{period}_high"
            else: ind_key = name.lower()

            if timeframe == "1d":
                if not redis_client: return None
                try:
                    data = redis_client.lindex(f"I:{SYMBOL}:1d", -(lookback + 1))
                    if data:
                        indicators = json.loads(data)
                        val = indicators.get(ind_key)
                        return float(val) if val else None
                except: return None
            else:
                # indicators 토픽에서 데이터 찾기
                indicators_topic = f"indicators.{SYMBOL}.{timeframe}"

                cache = None
                if lookback > 0:
                    cache = prev_data.get(f"prev_{timeframe}", {})
                else:
                    cache = latest_data.get(indicators_topic, {})

                if name == "BOLLINGER_BANDS" and subfield:
                    kafka_key = f"bb_{subfield}"
                    return float(cache.get(kafka_key, 0)) if cache and cache.get(kafka_key) else None
                return float(cache.get(ind_key, 0)) if cache and cache.get(ind_key) else None

        def get_profit_loss(field="percent"):
            try:
                resp = requests.get(f"{STRATEGY_SERVICE_URL}/api/v1/accounts/stocks/profit-rate", params={"memberId": MEMBER_ID}, timeout=5)
                if resp.status_code == 200:
                    for stock in resp.json().get('data', {}).get('stockData', []):
                        if stock.get('stockCode') == SYMBOL:
                            return float(stock.get('profitRate', 0)) if field == "percent" else None
            except: pass
            return None

        def get_level(level_name, timeframe, lookback=0):
            if level_name in ['52_WEEK_HIGH', '52_WEEK_LOW']:
                if not redis_client: return None
                try:
                    data = redis_client.lindex(f"I:{SYMBOL}:1d", -(lookback + 1))
                    if data:
                        indicators = json.loads(data)
                        field_map = {
                            '52_WEEK_HIGH': 'week52_high',
                            '52_WEEK_LOW': 'week52_low'
                        }
                        field = field_map.get(level_name)
                        return float(indicators.get(field, 0)) if field else None
                except: return None

            field_map = {
                'PREVIOUS_OPEN': 'open',
                'PREVIOUS_HIGH': 'high',
                'PREVIOUS_LOW': 'low',
                'PREVIOUS_CLOSE': 'close'
            }
            field = field_map.get(level_name, level_name.lower())

            try:
                resp = requests.get(f"{STRATEGY_SERVICE_URL}/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice",
                                  params={"fid_cond_mrkt_div_code": "J", "fid_input_iscd": SYMBOL},
                                  timeout=5)
                if resp.status_code == 200:
                    data = resp.json().get('output2', [])
                    if data and len(data) > lookback:
                        return float(data[-(lookback + 1)].get(field, 0))
            except: pass
            return None""";
  }

  /**
   * 조건 함수 생성
   */
  private String genConditionFunc(String name, Map<String, Object> node) {
    String condition = nodeToExpr(node);
    String guardCheck = genGuardCheck(node);

    return String.format("""
        def %s():
            try:
                %s
                return %s
            except Exception as e:
                print(f"⚠️ %s 실패: {e}")
                return False""", name, guardCheck, condition, name);
  }

  /**
   * Guard 조건 체크
   */
  private String genGuardCheck(Map<String, Object> node) {
    @SuppressWarnings("unchecked")
    Map<String, Object> guard = (Map<String, Object>) node.get("guard");
    if (guard == null || guard.isEmpty()) {
      return "";
    }

    List<String> checks = new ArrayList<>();
    if (Boolean.TRUE.equals(guard.get("opening_range_locked"))) {
      checks.add("""
          now = datetime.now()
                  if now.hour < 9 or (now.hour == 9 and now.minute < 15):
                      return False""");
    }

    return String.join("\n        ", checks) + (checks.isEmpty() ? "" : "\n        ");
  }

  /**
   * 노드 → Python 표현식
   */
  private String nodeToExpr(Map<String, Object> node) {
    String ntype = (String) node.get("type");

    if ("GROUP".equals(ntype)) {
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
      if (children == null || children.isEmpty())
        return "True";

      List<String> exprs = children.stream().map(this::nodeToExpr).toList();
      String op = "ALL".equals(node.get("logic")) ? " and " : " or ";
      return "(" + String.join(op, exprs) + ")";
    } else if ("COMPARE".equals(ntype)) {
      @SuppressWarnings("unchecked")
      Map<String, Object> left = (Map<String, Object>) node.get("left");
      @SuppressWarnings("unchecked")
      Map<String, Object> right = (Map<String, Object>) node.get("right");
      String leftExpr = valToExpr(left, 0);
      String rightExpr = valToExpr(right, 0);
      String operator = (String) node.getOrDefault("operator", ">");

      // CONSTANT는 항상 값이 있으므로 None 체크 제외
      boolean leftIsConstant = "CONSTANT".equals(left.get("kind"));
      boolean rightIsConstant = "CONSTANT".equals(right.get("kind"));

      if (leftIsConstant && rightIsConstant) {
        // 둘 다 상수면 None 체크 불필요
        return String.format("(%s %s %s)", leftExpr, operator, rightExpr);
      } else if (leftIsConstant) {
        // 왼쪽만 상수
        return String.format("(%s is not None and (%s %s %s))", rightExpr, leftExpr, operator, rightExpr);
      } else if (rightIsConstant) {
        // 오른쪽만 상수
        return String.format("(%s is not None and (%s %s %s))", leftExpr, leftExpr, operator, rightExpr);
      } else {
        // 둘 다 변수/함수 호출이면 둘 다 체크
        return String.format("((%s is not None and %s is not None) and (%s %s %s))",
            leftExpr, rightExpr, leftExpr, operator, rightExpr);
      }
    } else if ("CROSS".equals(ntype)) {
      String direction = (String) node.get("direction");
      @SuppressWarnings("unchecked")
      Map<String, Object> leftNode = (Map<String, Object>) node.get("left");
      @SuppressWarnings("unchecked")
      Map<String, Object> rightNode = (Map<String, Object>) node.get("right");

      String l0 = valToExpr(leftNode, 0);
      String l1 = valToExpr(leftNode, 1);
      String r0 = valToExpr(rightNode, 0);
      String r1 = valToExpr(rightNode, 1);

      // None 체크 추가: 모든 값이 None이 아닐 때만 비교
      String noneCheck = String.format("(%s is not None and %s is not None and %s is not None and %s is not None)",
          l0, l1, r0, r1);

      if ("UP".equals(direction)) {
        return String.format("(%s and (%s <= %s) and (%s > %s))", noneCheck, l1, r1, l0, r0);
      } else {
        return String.format("(%s and (%s >= %s) and (%s < %s))", noneCheck, l1, r1, l0, r0);
      }
    }

    return "False";
  }

  /**
   * 값 노드 → Python 표현식
   */
  private String valToExpr(Map<String, Object> node, int lookback) {
    String kind = (String) node.get("kind");

    if ("PRICE".equals(kind)) {
      String field = (String) node.get("field");
      String tf = (String) node.getOrDefault("timeframe", "1d");
      int lb = lookback + safeToInt(node.getOrDefault("lookback", 0), 0);
      return String.format("get_price('%s', '%s', %d)", field, tf, lb);
    } else if ("INDICATOR".equals(kind)) {
      String name = (String) node.get("name");
      @SuppressWarnings("unchecked")
      Map<String, Object> args = (Map<String, Object>) node.getOrDefault("args", new HashMap<>());
      int period = safeToInt(args.getOrDefault("period", 20), 20);
      String tf = (String) node.getOrDefault("timeframe", "1d");
      String sub = (String) node.get("subfield");
      int lb = lookback + safeToInt(node.getOrDefault("lookback", 0), 0);
      String subStr = sub != null ? String.format("'%s'", sub) : "None";
      return String.format("get_indicator('%s', %d, '%s', %s, %d)", name, period, tf, subStr, lb);
    } else if ("CONSTANT".equals(kind)) {
      @SuppressWarnings("unchecked")
      Map<String, Object> constant = (Map<String, Object>) node.get("constant");
      double val = safeToDouble(constant.get("value"), 0.0);
      String unit = (String) constant.get("unit");
      double value = "percent".equals(unit) ? val / 100.0 : val;
      return String.valueOf(value);
    } else if ("PROFIT_AND_LOSS".equals(kind)) {
      String field = (String) node.getOrDefault("profit_and_loss_field", "percent");
      return String.format("get_profit_loss('%s')", field);
    } else if ("LEVEL".equals(kind)) {
      String levelName = (String) node.get("level_name");
      String tf = (String) node.getOrDefault("timeframe", "1d");
      int lb = lookback + safeToInt(node.getOrDefault("lookback", 0), 0);
      return String.format("get_level('%s', '%s', %d)", levelName, tf, lb);
    } else if ("EXPRESSION".equals(kind) || node.containsKey("operator")) {
      @SuppressWarnings("unchecked")
      Map<String, Object> expr = node.containsKey("expression")
          ? (Map<String, Object>) node.get("expression")
          : node;
      @SuppressWarnings("unchecked")
      Map<String, Object> left = (Map<String, Object>) expr.get("left");
      @SuppressWarnings("unchecked")
      Map<String, Object> right = (Map<String, Object>) expr.get("right");
      String leftExpr = valToExpr(left, lookback);
      String rightExpr = valToExpr(right, lookback);
      String op = (String) expr.get("operator");

      // None 체크 추가: 산술 연산 시 None이면 0으로 처리
      return String.format("((%s if %s is not None else 0) %s (%s if %s is not None else 0))",
          leftExpr, leftExpr, op, rightExpr, rightExpr);
    }

    return "0";
  }

  /**
   * 주문 전송 함수
   */
  private String genOrderSender() {
    return """
        def stop_strategy():
            try:
                resp = requests.post(
                    f"{STRATEGY_SERVICE_URL}/api/v1/strategies/{STRATEGY_ID}/stop",
                    params={"memberId": MEMBER_ID},
                    timeout=10
                )
                if resp.status_code in [200, 201]:
                    print(f"✅ 전략 중지 API 호출 성공")
                    return True
                print(f"⚠️ 전략 중지 API 실패: {resp.status_code}")
                return False
            except Exception as e:
                print(f"⚠️ 전략 중지 API 오류: {e}")
                return False

        def send_buy_order(qty, price=0):
            try:
                resp = requests.post(
                    f"{TRADING_SERVICE_URL}/api/v1/order",
                    json={
                        "strategyId": STRATEGY_ID,
                        "orderQuantity": qty,
                        "orderPrice": price,
                        "stockCode": SYMBOL,
                        "orderType": "BUY"
                    },
                    headers={"Content-Type": "application/json"},
                    timeout=10
                )
                if resp.status_code in [200, 201]:
                    print(f"✅ 매수 주문: {SYMBOL} x{qty}")
                    return True
                print(f"❌ 매수 실패: {resp.status_code}")
                return False
            except Exception as e:
                print(f"❌ 매수 오류: {e}")
                return False

        def send_sell_order(qty, price=0):
            try:
                resp = requests.post(
                    f"{TRADING_SERVICE_URL}/api/v1/order",
                    json={
                        "strategyId": STRATEGY_ID,
                        "orderQuantity": qty,
                        "orderPrice": price,
                        "stockCode": SYMBOL,
                        "orderType": "SELL"
                    },
                    headers={"Content-Type": "application/json"},
                    timeout=10
                )
                if resp.status_code in [200, 201]:
                    print(f"✅ 매도 주문: {SYMBOL} x{qty}")
                    return True
                print(f"❌ 매도 실패: {resp.status_code}")
                return False
            except Exception as e:
                print(f"❌ 매도 오류: {e}")
                return False""";
  }

  /**
   * 메인 함수
   */
  private String genMain(boolean hasBuy, boolean hasSell, int buyQty, int sellQty) {
    boolean hasKafka = timeframes.stream().anyMatch(tf -> !"1d".equals(tf));

    StringBuilder code = new StringBuilder("def main():\n");

    if (hasKafka) {
      code.append("""
              if not kafka_consumer:
                  print("❌ Kafka Consumer 없음")
                  return

              print("\\n🚀 Kafka Streaming 시작\\n")
              try:
                  while True:
                      msg = kafka_consumer.poll(1.0)
                      if msg is None:
                          continue
                      if msg.error():
                          if msg.error().code() == KafkaError._PARTITION_EOF:
                              continue
                          else:
                              print(f"Kafka 에러: {msg.error()}")
                              break

                      topic = msg.topic()
                      data = json.loads(msg.value().decode('utf-8'))

                      # 토픽 파싱: candles.005930.1m 또는 indicators.005930.1m
                      topic_parts = topic.split('.')
                      topic_type = topic_parts[0]  # candles 또는 indicators
                      tf = topic_parts[-1]         # 타임프레임

                      # 이전 데이터 백업
                      prev_data[f"prev_{tf}"] = latest_data.get(topic, {})
                      latest_data[topic] = data

                      # 메시지 타입에 따른 처리
                      if topic_type == "candles":
                          # tick 타임프레임은 'tick_time', 나머지는 't' 필드 사용
                          time_field = data.get('tick_time' if tf == 'tick' else 't', 'N/A')
                          print(f"📊 [{topic}] 봉 데이터: {time_field}")
                      elif topic_type == "indicators":
                          print(f"📈 [{topic}] 지표 데이터: {data.get('t', 'N/A')}")
                      else:
                          print(f"📨 [{topic}] {data.get('t', 'N/A')}")

          """);

      if (hasBuy) {
        code.append(String.format("""
                        if check_buy_signal():
                            print("🟢 매수 신호!")
                            if send_buy_order(%d):
                                print("✅ 매수 완료")
                                stop_strategy()
                                break

            """, buyQty));
      }

      if (hasSell) {
        code.append(String.format("""
                        if check_sell_signal():
                            print("🔴 매도 신호!")
                            if send_sell_order(%d):
                                print("✅ 매도 완료")
                                stop_strategy()
                                break

            """, sellQty));
      }
    } else {
      code.append("""
              print("\\n🚀 Polling (60초)\\n")
              try:
                  while True:
          """);

      if (hasBuy) {
        code.append(String.format("""
                        if check_buy_signal():
                            print("🟢 매수 신호!")
                            if send_buy_order(%d):
                                print("✅ 매수 완료")
                                stop_strategy()
                                break
            """, buyQty));
      }

      if (hasSell) {
        code.append(String.format("""
                        if check_sell_signal():
                            print("🔴 매도 신호!")
                            if send_sell_order(%d):
                                print("✅ 매도 완료")
                                stop_strategy()
                                break
            """, sellQty));
      }

      code.append("            time.sleep(60)\n");
    }

    code.append("""
            except KeyboardInterrupt:
                print("\\n⏹️  중단")
            except Exception as e:
                print(f"\\n❌ 오류: {e}")
            finally:
                if kafka_consumer:
                    kafka_consumer.close()
                if redis_client:
                    redis_client.close()

        if __name__ == "__main__":
            main()""");

    return code.toString();
  }
}
