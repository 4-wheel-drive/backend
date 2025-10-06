package com.pda.strategy_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.pda.common_service.exception.ResourceNotFound;
import com.pda.common_service.response.ResponseMessage;
import com.pda.strategy_service.domain.Strategy;
import com.pda.strategy_service.domain.StrategySummary;
import com.pda.strategy_service.repository.jpa.StrategyRepository;
import com.pda.strategy_service.repository.jpa.StrategySummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StrategySummaryService {

    private final OpenAIClient openAIClient;
    private final StrategyRepository strategyRepository;
    private final StrategySummaryRepository strategySummaryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateSummary(String strategyJson) {
        try {
            String prompt = """
                    📌 [목표]
                    너는 전문 트레이딩 전략 분석가이자 AI 리포트 작성자야.
                    아래 JSON은 사용자가 직접 구성한 자동매매 전략 설정이야.
                    이 데이터를 바탕으로, 전략의 목적, 조건, 리스크를 세 부분으로 나눠 **구조화된 요약(JSON)** 을 생성해줘.
                    
                    ⚙️ [출력 형식 - JSON]
                    {
                      "summaryOverview": "string",
                      "summaryCondition": "string",
                      "summaryRisk": "string"
                    }
                    
                    🧭 [세부 작성 규칙]
                    1️⃣ summaryOverview — "전략 개요"
                    - 전략의 핵심 의도와 작동 방식을 한 문단으로 요약
                    
                    2️⃣ summaryCondition — "매매 조건 요약"
                    - 매수(`buy`) 또는 매도(`sell`) 조건이 존재하는 경우에만 포함
                    - **“매수:” 또는 “매도:” 같은 단어를 쓰지 말 것**
                    - 매수만 있으면 그 조건만 자연스럽게 문장으로 작성
                    - 매도만 있으면 그 조건만 자연스럽게 문장으로 작성
                    - 두 조건이 모두 있을 경우 자연스럽게 하나의 문단으로 연결 (예: “매수 시에는 …하며, 매도 시에는 …한다.”)
                    - 각 조건은 핵심적인 트리거와 기준만 요약
                    
                    3️⃣ summaryRisk — "리스크 요약"
                    - 전략의 약점, 오탐 가능성, 변동성에 대한 주의사항을 간결히 요약
                    
                    🎯 [작성 시 유의]
                    - 반드시 JSON 형태로 출력할 것
                    - 불필요한 설명, 문장, 구분 단어(‘매도 조건 없음’ 등)는 절대 금지
                    - 자연스러운 한국어로 핵심만 작성
                    - 전략 데이터에 없는 항목은 언급하지 말 것
                    
                    📘 [입력 전략 데이터]:
                    """ + strategyJson;

            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model(ChatModel.GPT_4O_MINI)
                    .addUserMessage(prompt)
                    .temperature(0.4)
                    .maxCompletionTokens(500)
                    .build();

            ChatCompletion completion = openAIClient.chat()
                    .completions()
                    .create(params);

            String result = completion.choices().get(0).message().content().orElse("").trim();
            return result;

        } catch (Exception e) {
            return """
                    {
                      "summaryOverview": "요약 생성 중 오류가 발생했습니다.",
                      "summaryCondition": "",
                      "summaryRisk": ""
                    }
                    """;
        }
    }

    @Transactional
    public StrategySummary generateSummaryAndSave(Long strategyId, String strategyJson) {
        Strategy strategy = strategyRepository.findById(strategyId)
                .orElseThrow(() -> new ResourceNotFound(ResponseMessage.STRATEGY_NOT_FOUND));
        System.out.println(strategy);
        try {
            String aiResponse = generateSummary(strategyJson);
            System.out.println(aiResponse);
            String cleaned = aiResponse
                    .replaceAll("(?s)```json", "")
                    .replaceAll("(?s)```", "")
                    .trim();

            JsonNode jsonNode = objectMapper.readTree(cleaned);
            String overview = jsonNode.path("summaryOverview").asText("");
            String risk = jsonNode.path("summaryRisk").asText("");

            String conditionText;
            JsonNode conditionNode = jsonNode.path("summaryCondition");
            if (conditionNode.isObject()) {
                conditionText = objectMapper.writeValueAsString(conditionNode);
            } else {
                conditionText = conditionNode.asText("");
            }

            StrategySummary summary = StrategySummary.builder()
                    .strategy(strategy)
                    .summaryOverview(overview)
                    .summaryCondition(conditionText)
                    .summaryRisk(risk)
                    .build();

            saveSummary(summary);
            return summary;

        } catch (Exception e) {
            throw new RuntimeException("요약 저장 실패", e);
        }
    }

    @Transactional
    public void saveSummary(StrategySummary summary) {
        strategySummaryRepository.save(summary);
    }
}
