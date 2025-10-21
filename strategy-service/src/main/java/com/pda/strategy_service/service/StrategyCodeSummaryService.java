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
            String prompt = """
                    너는 Python 자동매매 전략 코드 리팩토링 전문가야.
                    
                    아래 전략 코드를 **현실감 있는 수도코드 스타일**로 재구성해줘.  
                    코드는 실제로 실행되지 않아도 되지만, 각 함수가 무슨 일을 하는지 흐름이 코드만 봐도 이해되게 작성해라.
                    
                    🎯 리팩토링 기준
                    - import, 설정, 환경변수, logger, try/except, print 등은 제거
                    - 함수 내부는 단순 pass 대신, 동작 의도를 드러내는 구체적 호출 형태로 표현
                      예: `return fetch_market_price(symbol, field, timeframe)`  
                           `return send_trading_request("BUY", symbol, qty)`
                    - 불필요한 주석은 전부 제거, 코드 상단의 전략 요약만 남김
                    - 코드 외부의 설명이나 분석 문장은 절대 작성하지 말 것
                    - 실행 흐름은 다음 순서를 따라야 함:
                      가격 확인 → 지표 확인 → 조건 판단 → 주문 실행 → 전략 중지
                    - 함수 이름은 원본 구조(get_price, get_indicator, check_buy_signal, send_order, main 등)를 유지
                    - 포트, IP, URL, 토큰 등 민감 정보는 <REDACTED> 처리 또는 생략
                    
                    💡 strategyJson 기반 상단 주석 구성
                    - JSON에 존재하는 경우만 아래 항목 포함:
                      # 전략 이름:
                      # 종목:
                      # 로직 요약:
                      # 알고리즘 흐름:
                    
                    📘 입력 코드:
                    """ + strategyCode + "\n\n📘 전략 정보(JSON):\n" + strategyJson;

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
