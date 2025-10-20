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
                    너는 전문 트레이딩 전략 분석가이자 AI 리포트 작성자야.  
아래 JSON은 사용자가 직접 구성한 자동매매 전략 설정이야.  
이 데이터를 기반으로, 전략의 **핵심 의도**, **매매 조건**, **리스크 포인트**를 세 부분으로 나눈 구조화된 요약(JSON)을 생성해줘.
이 전략 설명을 리스트 없이 줄글 형식으로 작성하고, 문장 구조는 명확하게, 사용자 입장에서 쉽게 이해할 수 있도록 자세하고 읽기 쉬운 문체로 작성해줘.
모든 내용은 존댓말로 작성해줘.

⚙️ [출력 형식]
{
  "summaryOverview": "string",
  "summaryCondition": "string",
  "summaryRisk": "string"
}

🧭 [세부 작성 규칙]

1️⃣ summaryOverview — "전략 개요"
- 전략의 목적과 작동 원리를 간결하게 설명하되,  
  사용자가 이 전략을 통해 무엇을 노리는지, 어떤 시장 상황에 적합한지를 포함할 것  
- 단순한 기술적 설명이 아닌, **전략의 성격과 의도(추세 추종 / 반전 포착 / 돌파 대응 등)** 이 드러나야 함  
- 문체는 자연스럽고 분석 리포트처럼 작성

2️⃣ summaryCondition — "매매 조건 요약"
- 매수(buy), 매도(sell) 조건이 존재하는 경우에만 기술
- **‘매수:’, ‘매도:’ 같은 단어는 사용하지 말 것**
- 조건은 핵심 트리거와 지표 기준만 간결하게 서술
- 두 조건이 모두 있을 경우, **하나의 문단으로 자연스럽게 연결**
  (예: “가격이 상단선을 돌파하면 진입하며, 하단선을 이탈하거나 손익 기준에 도달하면 청산한다.”)
- 조건에 포함된 **지표, 타임프레임, 수치 기준(예: -3%, +7%)** 은 명확하게 제시할 것

3️⃣ summaryRisk — "리스크 요약"
- 전략의 잠재적 약점이나 오탐 가능성, 변동성에 따른 주의점을 간결히 요약
- “언제 효과가 떨어질 수 있는지” 또는 “추가로 고려하면 좋은 보완 조건”을 제안
- 예: “횡보장에서는 허위 돌파 신호가 잦을 수 있어 거래량 기반 필터를 추가하는 것이 좋다.”

🎯 [작성 시 유의]
- 반드시 **정확한 JSON 형식**으로 출력할 것
- 불필요한 단어(‘매도 조건 없음’, ‘매수 조건만 존재’)는 절대 포함하지 말 것
- 자연스러운 한국어 문장으로 작성하되, 분석 리포트처럼 객관적이고 간결해야 함
- 전략 데이터에 존재하지 않는 항목은 언급하지 말 것
- 사용자가 전략을 실제로 **운용할 때 어떤 점에 집중해야 하는지**가 드러나야 함

                    
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
