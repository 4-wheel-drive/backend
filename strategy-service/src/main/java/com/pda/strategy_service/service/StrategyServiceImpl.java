package com.pda.strategy_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pda.common_service.exception.AuthException;
import com.pda.common_service.exception.MemberException;
import com.pda.common_service.exception.ResourceNotFound;
import com.pda.common_service.exception.StrategyException;
import com.pda.common_service.exception.StrategyTemplatesException;
import com.pda.common_service.response.ResponseMessage;
import com.pda.common_service.stock.Stock;
import com.pda.common_service.stock.dto.StockInfo;
import com.pda.common_service.stock.repository.StockRepository;
import com.pda.common_service.user.domain.Member;
import com.pda.common_service.user.repository.MemberRepository;
import com.pda.strategy_service.controller.dto.StrategyResponse.ProfitDto;
import com.pda.strategy_service.controller.dto.StrategyResponse.ProfitSeries;
import com.pda.strategy_service.controller.dto.StrategyResponse.ReadStrategies;
import com.pda.strategy_service.controller.dto.StrategyResponse.ReadStrategy;
import com.pda.strategy_service.domain.Strategy;
import com.pda.strategy_service.domain.StrategySummary;
import com.pda.strategy_service.domain.dto.SimpleStrategy;
import com.pda.strategy_service.domain.dto.StrategyDto;
import com.pda.strategy_service.domain.dto.StrategyMetaDto;
import com.pda.strategy_service.domain.dto.StrategySummaryDto;
import com.pda.strategy_service.domain.mongodb.CustomStrategy;
import com.pda.strategy_service.repository.jpa.StrategyRepository;
import com.pda.strategy_service.repository.jpa.StrategySummaryRepository;
import com.pda.strategy_service.repository.mongodb.CustomStrategyRepository;
import com.pda.strategy_service.repository.mongodb.StrategyTemplateRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StrategyServiceImpl implements StrategyService {
    private final MemberRepository memberRepository;
    private final StrategyRepository strategyRepository;
    private final StockRepository stockRepository;
    private final ProfitCalculator profitCalculator;
    private final StrategySummaryService strategySummaryService;
    private final StrategySummaryRepository strategySummaryRepository;
    private final CustomStrategyRepository customStrategyRepository;
    private final StrategyTemplateRepository strategyTemplateRepository;

    @Override
    public ReadStrategies getStrategies(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(ResponseMessage.MEMBER_NOT_FOUND));
        List<Strategy> strategies = strategyRepository.findAllByMember(member);

        List<StrategyDto> strategyDtos = new ArrayList<>();

        for (Strategy strategy : strategies) {
            Stock stock = strategy.getStock();
            StockInfo stockInfo = stock.toDto();

            BigDecimal profitAmount = strategy.getStrategyProfitSummary()
                    .getStrategyProfitSummaryCurrentPrice()
                    .subtract(strategy.getStrategyProfitSummary().getStrategyProfitSummaryAvgBuyPrice());

            StrategyDto strategyDto = strategy.toDto(stockInfo, profitAmount);
            strategyDtos.add(strategyDto);
        }
        return new ReadStrategies(strategyDtos);
    }

    @Override
    public ReadStrategy getMonoStrategy(Long memberId, Long strategyId) {
        Strategy strategy = strategyRepository.findById(strategyId)
                .orElseThrow(() -> new StrategyException(ResponseMessage.STRATEGY_NOT_FOUND));

        if (!(Objects.equals(strategy.getMember().getId(), memberId))) {
            throw new AuthException(ResponseMessage.PERMISSION_DENY);
        }

        StrategySummary strategySummary = strategySummaryRepository.findByStrategy(strategy);
        StrategySummaryDto strategySummaryDto = strategySummary.toDto();

        BigDecimal allCumulativeProfit = profitCalculator.allCumulativeProfit(strategy);
        BigDecimal weekCumulativeProfit = profitCalculator.weekCumulativeProfit(strategy);
        ProfitDto strategyProfit = new ProfitDto(allCumulativeProfit, weekCumulativeProfit);
        ProfitSeries periodSeries = profitCalculator.getAllPeriodSeries(strategy);

        CustomStrategy customStrategy = customStrategyRepository.findByStrategyId(strategyId);

        Stock stock = strategy.getStock();

        StockInfo stockInfo = stock.toDto();
        SimpleStrategy simpleStrategy = strategy.toSimpleStrategyDto();

        return new ReadStrategy(stockInfo, simpleStrategy, strategyProfit, customStrategy, periodSeries,
                strategySummaryDto);
    }

    @Override
    @Transactional
    public Strategy saveStrategyMeta(Long memberId, StrategyMetaDto strategyMeta) {
        Member member = memberRepository.findById(1L)
                .orElseThrow(() -> new MemberException(ResponseMessage.MEMBER_NOT_FOUND));

        Stock stock = stockRepository.findById(strategyMeta.stockId())
                .orElseThrow(() -> new ResourceNotFound(ResponseMessage.STOCK_NOT_FOUND));

        Strategy strategy = Strategy.create(stock, member, strategyMeta.strategyName());

        return strategyRepository.save(strategy);
    }

    @Override
    @Transactional
    public CustomStrategy saveStrategy(Long strategyMetaId, Map<String, Object> strategyJson) {
        try {
            CustomStrategy customStrategy = CustomStrategy.builder()
                    .strategyId(strategyMetaId)
                    .strategyName((String) strategyJson.get("strategy_name"))
                    .version((Integer) strategyJson.get("version"))
                    .meta((Map<String, Object>) strategyJson.get("meta"))
                    .buy((Map<String, Object>) strategyJson.get("buy"))
                    .sell((Map<String, Object>) strategyJson.get("sell"))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            CustomStrategy savedCustomStrategy = customStrategyRepository.save(customStrategy);

            String jsonString = new ObjectMapper().writeValueAsString(strategyJson);

            strategySummaryService.generateSummaryAndSave(strategyMetaId, jsonString);

            return savedCustomStrategy;

        } catch (Exception e) {
            throw new StrategyTemplatesException(ResponseMessage.STRATEGY_SAVE_FAILED);
        }
    }

}
