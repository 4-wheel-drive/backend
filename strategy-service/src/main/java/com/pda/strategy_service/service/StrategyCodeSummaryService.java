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

    public String generateSummary(String strategyCode) {
        try {
            String prompt = """
                    너는 Python 자동매매 전략 코드 리팩토링 전문가야.
                    
                    사용자가 작성한 원본 전략 코드를 입력받으면,
                    코드를 간결하게 정리하되, 코드 상단과 주요 로직 위에
                    **알고리즘의 흐름(전략이 어떻게 동작하는지)** 만 주석으로 추가해줘.
                    
                    ⚙️ [목표]
                    - 코드의 핵심 로직(지표 계산, 조건 판단, 매수/매도 실행)만 남긴 버전을 생성
                    - 외부 의존성(import os, redis, kafka, requests 등)은 모두 제거
                    - try/except, print, logger, 환경변수(os.getenv) 등 실행용 코드는 삭제
                    - 각 함수에 불필요한 세부 주석은 달지 말고,
                      대신 전체 전략의 동작 흐름을 자연스러운 한국어 주석으로 표현
                    - 상단에는 다음 정보를 포함:
                      # 전략 이름: (존재한다면)
                      # 종목: (존재한다면)
                      # 로직 요약: (핵심 매매 조건을 한 줄로 설명)
                    - 코드 외에는 불필요한 설명을 출력하지 말 것
                    - 반드시 ```python 으로 시작하고 ``` 으로 끝나는 형식을 유지
                    
                    💡 [주석 예시]
                    # 전략 이름: RSI 돌파 매수 전략
                    # 종목: 005930 (삼성전자)
                    # 로직 요약: RSI(14)가 30 이하에서 30선을 상향 돌파할 때 매수 진입
                    # 알고리즘 흐름:
                    # 1. RSI 지표값을 가져옴
                    # 2. 이전 봉과 현재 봉의 RSI 비교
                    # 3. RSI가 30 이하 → 30 상향 시 매수 조건 충족
                    # 4. 조건 만족 시 매수 함수 호출
                    
                    📘 [입력 코드]
                    """ + strategyCode;

            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model(ChatModel.GPT_4O_MINI)
                    .addUserMessage(prompt)
                    .temperature(0.3)
                    .maxCompletionTokens(800)
                    .build();

            ChatCompletion completion = openAIClient.chat()
                    .completions()
                    .create(params);

            String result = completion.choices().get(0).message().content().orElse("").trim();

            String cleaned = result
                    .replaceAll("(?s)```python", "")
                    .replaceAll("(?s)```", "")
                    .trim();

            return cleaned;

        } catch (Exception e) {
            log.error("코드 요약 생성 실패", e);
            return "# 코드 요약 생성 중 오류가 발생했습니다.";
        }
    }

    @Transactional
    public void generateSummaryAndSave(Long strategyId, String strategyCode) {
        Strategy strategy = strategyRepository.findById(strategyId)
                .orElseThrow(() -> new ResourceNotFound(ResponseMessage.STRATEGY_NOT_FOUND));

        try {
            String summarizedCode = generateSummary(strategyCode);

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
