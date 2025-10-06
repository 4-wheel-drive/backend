package com.pda.strategy_service.service;

import com.pda.common_service.exception.MemberException;
import com.pda.common_service.exception.ResourceNotFound;
import com.pda.common_service.exception.StrategyException;
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
import com.pda.strategy_service.domain.dto.SimpleStrategy;
import com.pda.strategy_service.domain.dto.StrategyDto;
import com.pda.strategy_service.domain.dto.StrategyMetaDto;
import com.pda.strategy_service.repository.StrategyRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StrategyServiceImpl implements StrategyService {
    private final MemberRepository memberRepository;
    private final StrategyRepository strategyRepository;
    private final StockRepository stockRepository;
    private final ProfitCalculator profitCalculator;

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
    public ReadStrategy getMonoStrategy(Long strategyId) {
        Strategy strategy = strategyRepository.findById(strategyId)
                .orElseThrow(() -> new StrategyException(ResponseMessage.STRATEGY_NOT_FOUND));
        BigDecimal allCumulativeProfit = profitCalculator.allCumulativeProfit(strategy);
        BigDecimal weekCumulativeProfit = profitCalculator.weekCumulativeProfit(strategy);
        ProfitDto strategyProfit = new ProfitDto(allCumulativeProfit, weekCumulativeProfit);
        ProfitSeries periodSeries = profitCalculator.getAllPeriodSeries(strategy);

        Stock stock = strategy.getStock();

        StockInfo stockInfo = stock.toDto();
        SimpleStrategy simpleStrategy = strategy.toSimpleStrategyDto();

        return new ReadStrategy(stockInfo, simpleStrategy, strategyProfit, periodSeries);
    }

    @Override
    @Transactional
    public Strategy saveStrategy(Long memberId, StrategyMetaDto strategyMeta) {
        Member member = memberRepository.findById(1L)
                .orElseThrow(() -> new MemberException(ResponseMessage.MEMBER_NOT_FOUND));

        Stock stock = stockRepository.findById(strategyMeta.stockId())
                .orElseThrow(() -> new ResourceNotFound(ResponseMessage.STOCK_NOT_FOUND));

        Strategy strategy = Strategy.create(stock, member, strategyMeta.strategyName());

        return strategyRepository.save(strategy);
    }
}
