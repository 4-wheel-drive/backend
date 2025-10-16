#!/usr/bin/env python3
"""
전략 코드 생성기 (Strategy Code Generator)
MongoDB의 전략 JSON을 독립 실행 가능한 Python 코드로 변환
"""
from typing import Dict, Any, Set
import json


class StrategyCodeGenerator:
    """전략 JSON → Python 코드 생성기"""
    
    def __init__(self):
        self.timeframes = set()
        self.member_id = 1
        self.symbol = "005930"
    
    def generate_code(self, strategy_json: Dict[str, Any], member_id: int, symbol: str) -> str:
        self.timeframes = self._analyze_timeframes(strategy_json)
        self.member_id = member_id
        self.symbol = symbol
        
        strategy_name = strategy_json.get("strategy_name", "unknown")
        buy_config = strategy_json.get("buy", {})
        sell_config = strategy_json.get("sell", {})
        
        buy_node = buy_config.get("node")
        sell_node = sell_config.get("node")
        
        buy_quantity = buy_config.get("orderQuantity", 0)
        sell_quantity = sell_config.get("orderQuantity", 0)
        
        # 코드 조합
        parts = []
        parts.append(self._gen_header(strategy_name))
        parts.append(self._gen_data_source())
        parts.append(self._gen_helpers())
        
        if buy_node:
            parts.append(self._gen_condition_func("check_buy_signal", buy_node))
        if sell_node:
            parts.append(self._gen_condition_func("check_sell_signal", sell_node))
        
        parts.append(self._gen_order_sender())
        parts.append(self._gen_main(bool(buy_node), bool(sell_node), buy_quantity, sell_quantity))
        
        return "\n\n".join(parts)
    
    def _analyze_timeframes(self, obj: Any) -> Set[str]:
        """JSON에서 timeframe 추출"""
        timeframes = set()
        
        if isinstance(obj, dict):
            if "timeframe" in obj:
                timeframes.add(obj["timeframe"])
            for v in obj.values():
                timeframes.update(self._analyze_timeframes(v))
        elif isinstance(obj, list):
            for item in obj:
                timeframes.update(self._analyze_timeframes(item))
        
        return timeframes
    
    def _gen_header(self, name: str) -> str:
        return f'''import os
import json
import time
import redis
import requests
from datetime import datetime
from kafka import KafkaConsumer

STRATEGY_ID = int(os.getenv("STRATEGY_ID", "0"))
SYMBOL = os.getenv("SYMBOL", "{self.symbol}")
MEMBER_ID = int(os.getenv("MEMBER_ID", "{self.member_id}"))
REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
REDIS_PORT = int(os.getenv("REDIS_PORT", "6379"))
KAFKA_BROKERS = os.getenv("KAFKA_BROKERS", "localhost:9092").split(",")
TRADING_SERVICE_URL = os.getenv("TRADING_SERVICE_URL", "http://trading-service:8080")
STRATEGY_SERVICE_URL = os.getenv("STRATEGY_SERVICE_URL", "http://strategy-service:8081")

print("=" * 60)
print(f"전략: {name}")
print(f"종목: {{SYMBOL}}, 회원: {{MEMBER_ID}}")
print("=" * 60)'''
    
    def _gen_data_source(self) -> str:
        has_daily = "1d" in self.timeframes
        kafka_tfs = sorted([tf for tf in self.timeframes if tf != "1d"])
        
        code = "latest_data = {}\nprev_data = {}\n"
        
        if has_daily:
            code += f'''
try:
    redis_client = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, db=0, decode_responses=True)
    redis_client.ping()
    print("✅ Redis 연결")
except Exception as e:
    print(f"❌ Redis 실패: {{e}}")
    redis_client = None'''
        else:
            code += "\nredis_client = None"
        
        if kafka_tfs:
            topics = [f"indicators.{self.symbol}.{tf}" for tf in kafka_tfs]
            code += f'''

try:
    topics = {topics}
    kafka_consumer = KafkaConsumer(
        *topics,
        bootstrap_servers=KAFKA_BROKERS,
        value_deserializer=lambda m: json.loads(m.decode('utf-8')),
        auto_offset_reset='latest',
        group_id=f'strategy-{{SYMBOL}}'
    )
    print(f"✅ Kafka 구독: {{topics}}")
except Exception as e:
    print(f"❌ Kafka 실패: {{e}}")
    kafka_consumer = None'''
        else:
            code += "\nkafka_consumer = None"
        
        return code
    
    def _gen_helpers(self) -> str:
        return '''def get_price(field, timeframe, lookback=0):
    field_map = {'open': 'o', 'high': 'h', 'low': 'l', 'close': 'c', 'volume': 'v'}
    key = field_map.get(field, field)
    
    if timeframe == "1d":
        if not redis_client: return None
        try:
            data = redis_client.lindex(f"C:{SYMBOL}:1d", -(lookback + 1))
            return float(json.loads(data).get(key, 0)) if data else None
        except: return None
    else:
        cache = prev_data.get(f"prev_{timeframe}", {}) if lookback > 0 else latest_data.get(f"indicators.{SYMBOL}.{timeframe}", {})
        return float(cache.get(key, 0)) if cache.get(key) else None

def get_indicator(name, period, timeframe, subfield=None, lookback=0):
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
        cache = prev_data.get(f"prev_{timeframe}", {}) if lookback > 0 else latest_data.get(f"indicators.{SYMBOL}.{timeframe}", {})
        if name == "BOLLINGER_BANDS" and subfield:
            kafka_key = f"bb_{subfield}"
            return float(cache.get(kafka_key, 0)) if cache.get(kafka_key) else None
        return float(cache.get(ind_key, 0)) if cache.get(ind_key) else None

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
    # 52주 지표는 Redis에서 가져오기
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
    
    # PREVIOUS_* 지표는 API에서 가져오기
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
    return None'''
    
    def _gen_condition_func(self, name: str, node: Dict) -> str:
        condition = self._node_to_expr(node)
        guard_check = self._gen_guard_check(node)
        return f'''def {name}():
    try:
        {guard_check}
        return {condition}
    except Exception as e:
        print(f"⚠️ {name} 실패: {{e}}")
        return False'''
    
    def _node_to_expr(self, node: Dict) -> str:
        """노드 → Python 표현식"""
        ntype = node.get("type")
        
        if ntype == "GROUP":
            children = node.get("children", [])
            if not children: return "True"
            
            exprs = [self._node_to_expr(c) for c in children]
            op = " and " if node.get("logic") == "ALL" else " or "
            return f"({op.join(exprs)})"
        
        elif ntype == "COMPARE":
            left = self._val_to_expr(node.get("left"))
            right = self._val_to_expr(node.get("right"))
            op = node.get("operator", ">")
            return f"({left} {op} {right})"
        
        elif ntype == "CROSS":
            direction = node.get("direction")
            l_node = node.get("left")
            r_node = node.get("right")
            
            l0 = self._val_to_expr(l_node, 0)
            l1 = self._val_to_expr(l_node, 1)
            r0 = self._val_to_expr(r_node, 0)
            r1 = self._val_to_expr(r_node, 1)
            
            if direction == "UP":
                return f"(({l1} <= {r1}) and ({l0} > {r0}))"
            else:  # DOWN
                return f"(({l1} >= {r1}) and ({l0} < {r0}))"
        
        return "False"
    
    def _gen_guard_check(self, node: Dict) -> str:
        """guard 조건 체크 코드 생성"""
        guard = node.get("guard", {})
        if not guard:
            return ""
        
        checks = []
        
        # opening_range_locked 체크
        if guard.get("opening_range_locked"):
            checks.append("""now = datetime.now()
        if now.hour < 9 or (now.hour == 9 and now.minute < 15):
            return False""")
        
        return "\n        ".join(checks) + ("\n        " if checks else "")
    
    def _val_to_expr(self, node: Dict, lookback: int = 0) -> str:
        """값 노드 → Python 표현식"""
        kind = node.get("kind")
        
        if kind == "PRICE":
            field = node.get("field")
            tf = node.get("timeframe", "1d")
            lb = lookback + node.get("lookback", 0)
            return f"get_price('{field}', '{tf}', {lb})"
        
        elif kind == "INDICATOR":
            name = node.get("name")
            period = node.get("args", {}).get("period", 20)
            tf = node.get("timeframe", "1d")
            sub = node.get("subfield")
            lb = lookback + node.get("lookback", 0)
            sub_str = f"'{sub}'" if sub else "None"
            return f"get_indicator('{name}', {period}, '{tf}', {sub_str}, {lb})"
        
        elif kind == "CONSTANT":
            val = node.get("constant", {}).get("value")
            unit = node.get("constant", {}).get("unit")
            return str(float(val) / 100.0 if unit == "percent" else float(val))
        
        elif kind == "PROFIT_AND_LOSS":
            field = node.get("profit_and_loss_field", "percent")
            return f"get_profit_loss('{field}')"
        
        elif kind == "LEVEL":
            level_name = node.get("level_name")
            tf = node.get("timeframe", "1d")
            lb = lookback + node.get("lookback", 0)
            return f"get_level('{level_name}', '{tf}', {lb})"
        
        elif kind == "EXPRESSION" or node.get("operator"):
            expr = node.get("expression", node)
            left = self._val_to_expr(expr.get("left"), lookback)
            right = self._val_to_expr(expr.get("right"), lookback)
            op = expr.get("operator")
            return f"({left} {op} {right})"
        
        return "0"
    
    def _gen_order_sender(self) -> str:
        return '''def send_buy_order(qty, price=0):
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
        return False'''
    
    def _gen_main(self, has_buy: bool, has_sell: bool, buy_qty: int = 10, sell_qty: int = 10) -> str:
        has_kafka = any(tf != "1d" for tf in self.timeframes)
        
        code = "# 메인 실행\ndef main():\n"
        
        if has_kafka:
            code += '''    if not kafka_consumer:
        print("❌ Kafka Consumer 없음")
        return
    
    print("\\n🚀 Kafka Streaming 시작\\n")
    try:
        for msg in kafka_consumer:
            topic = msg.topic
            data = msg.value
            tf = topic.split('.')[-1]
            
            prev_data[f"prev_{tf}"] = latest_data.get(topic, {})
            latest_data[topic] = data
            
            print(f"📨 [{topic}] {data.get('t', 'N/A')}")
            
'''
            if has_buy:
                code += f'''            if check_buy_signal():
                print("🟢 매수 신호!")
                send_buy_order({buy_qty})
            
'''
            if has_sell:
                code += f'''            if check_sell_signal():
                print("🔴 매도 신호!")
                send_sell_order({sell_qty})
            
'''
        else:
            code += '''    print("\\n🚀 Polling (60초)\\n")
    try:
        while True:
'''
            if has_buy:
                code += f'''            if check_buy_signal():
                print("🟢 매수 신호!")
                send_buy_order({buy_qty})
'''
            if has_sell:
                code += f'''            if check_sell_signal():
                print("🔴 매도 신호!")
                send_sell_order({sell_qty})
'''
            code += "            time.sleep(60)\n"
        
        code += '''    except KeyboardInterrupt:
        print("\\n⏹️  중단")
    except Exception as e:
        print(f"\\n❌ 오류: {e}")
    finally:
        if kafka_consumer: kafka_consumer.close()
        if redis_client: redis_client.close()

if __name__ == "__main__":
    main()'''
        
        return code


def generate_strategy_file(strategy_json: Dict[str, Any], member_id: int, output_path: str = None) -> str:
    """
    전략 JSON → Python 파일 생성
    
    Returns:
        생성된 파일 경로
    """
    symbol = strategy_json.get("meta", {}).get("universe", ["005930"])[0]
    name = strategy_json.get("strategy_name", "unknown")
    
    generator = StrategyCodeGenerator()
    code = generator.generate_code(strategy_json, member_id, symbol)
    
    if not output_path:
        output_path = f"strategy_{name}_{symbol}.py"
    
    with open(output_path, 'w', encoding='utf-8') as f:
        f.write(code)
    
    print(f"✅ 파일 생성: {output_path}")
    return output_path
