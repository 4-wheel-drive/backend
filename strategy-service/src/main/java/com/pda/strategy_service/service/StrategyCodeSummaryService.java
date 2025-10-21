package com.pda.strategy_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.pda.common_service.exception.ResourceNotFound;
import com.pda.common_service.response.ResponseMessage;
import com.pda.strategy_service.domain.Strategy;
import com.pda.strategy_service.domain.StrategyCodeSummary;
import com.pda.strategy_service.repository.jpa.StrategyCodeSummaryRepository;
import com.pda.strategy_service.repository.jpa.StrategyRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyCodeSummaryService {

    private final OpenAIClient openAIClient;
    private final StrategyRepository strategyRepository;
    private final StrategyCodeSummaryRepository strategyCodeSummaryRepository;

    public String generateSummary(String strategyCode, Map<String, Object> strategyJson) {
        try {
            String strategyName = (String) strategyJson.getOrDefault("strategy_name", "");
            String stockCode = (String) strategyJson.getOrDefault("stock_code", "");
            String summary = (String) strategyJson.getOrDefault("summary",
                    (String) strategyJson.getOrDefault("description", ""));

            StringBuilder metaInfo = new StringBuilder();
            if (!strategyName.isEmpty()) {
                metaInfo.append("# 전략 이름: ").append(strategyName).append("\n");
            }
            if (!stockCode.isEmpty()) {
                metaInfo.append("# 종목: ").append(stockCode).append("\n");
            }
            if (!summary.isEmpty()) {
                metaInfo.append("# 로직 요약: ").append(summary).append("\n");
            }

            String prompt = """
                    너는 Python 자동매매 전략을 **전략의 흐름이 명확하게 드러나도록 수도코드 형태로 리팩토링하는 전문가**야.

                    아래 JSON과 코드는 사용자가 만든 실제 자동매매 전략이야.  
                    이걸 기반으로, **사람이 읽어서 전략의 의도를 바로 이해할 수 있는 수도코드**를 만들어줘.

                    ✣️ 작성 기준

                    - 코드는 **전략의 의사결정 흐름 중심**으로 작성할 것.  
                      (가격 확인 → 지표 확인 → 조건 판단 → 주문 실행 → 전략 종료)

                    - Kafka, Redis, API, WebSocket, 데이터 파싱 등 기술적 구현은 **완전히 제거**하고,  
                      그 역할은 "무엇을 한다"가 명확히 드러나는 추상적 함수명으로 표현해라.  
                      예: kafka_consumer.poll() → get_latest_market_info()

                    - 단순하고 추상적인 이름(initialize_strategy, fetch_market_data, execute_order)은 절대 사용하지 마라.  
                      대신 **전략의 조건이나 의도를 직접 드러내는 함수명**을 사용해라.  
                      예:
                        ❌ is_condition_met() → ✅ is_price_below_100k_and_rsi_below_30()  
                        ❌ execute_order() → ✅ place_buy_order_if_signal_detected()

                    - 함수명은 **비전공자도 읽으면 의미를 이해할 수 있게** 구체적으로 작성해야 한다.  
                      함수 하나하나가 "무엇을 판단하거나 수행하는지" 바로 알 수 있어야 한다.

                    - 코드 내부에는 **절대로 절대로 절대로 주석을 작성하지 않는다.**  
                      오직 코드 상단에 JSON 기반으로 생성된 전략 요약 주석만 포함한다.

                    - import, print, try/except, logger, 포트, URL, 토큰 등 민감한 요소는 전부 제거.
                    - 코드 외부의 설명, 분석 문장 없이 **오직 코드만 출력.**

                    - 필요 시 전략의 조건을 반영해 세부 함수명을 구성할 것.  
                      예를 들어 JSON에 "RSI 30 이하" 조건이 있다면  
                      함수명에 `rsi_below_30` 같은 단어를 직접 포함시켜라.

                    💡 전략 정보 (JSON 기반 메타데이터):
                    """ + metaInfo + """

                    📘 목록 전력 코드:
                    """ + strategyCode;

            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model(ChatModel.GPT_4O)
                    .addUserMessage(prompt)
                    .temperature(0.2)
                    .maxCompletionTokens(1000)
                    .build();

            ChatCompletion completion = openAIClient.chat().completions().create(params);

            return completion.choices().get(0).message().content().orElse("").trim()
                    .replaceAll("(?s)```python", "")
                    .replaceAll("(?s)```", "").trim();

        } catch (Exception e) {
            log.error("코드 요약 생성 실패", e);
            return "# 코드 요약 생성 중 오류가 발생했습니다.";
        }
    }

    @Transactional
    public void generateSummaryAndSave(Map<String, Object> strategyJson, Long strategyId, String strategyCode) {
        Strategy strategy = strategyRepository.findById(strategyId)
                .orElseThrow(() -> new ResourceNotFound(ResponseMessage.STRATEGY_NOT_FOUND));

        try {
            String summarizedCode = generateSummary(strategyCode, strategyJson);

            StrategyCodeSummary summary = StrategyCodeSummary.builder()
                    .strategy(strategy)
                    .codeSummary(summarizedCode)
                    .build();

            saveSummary(summary);

        } catch (Exception e) {
            log.error("핵심 코드 요약 저장 실패", e);
            throw new RuntimeException("핵심 코드 요약 저장 실패", e);
        }
    }

    @Transactional
    public void saveSummary(StrategyCodeSummary strategyCodeSummary) {
        strategyCodeSummaryRepository.save(strategyCodeSummary);
    }
}
