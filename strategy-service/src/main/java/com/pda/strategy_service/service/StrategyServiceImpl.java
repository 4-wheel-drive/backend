package com.pda.strategy_service.service;

import com.pda.common_service.exception.MemberException;
import com.pda.common_service.response.ResponseMessage;
import com.pda.common_service.stock.MemberStock;
import com.pda.common_service.stock.MemberStockRepository;
import com.pda.common_service.stock.Stock;
import com.pda.common_service.stock.dto.StockInfo;
import com.pda.common_service.user.domain.Member;
import com.pda.common_service.user.repository.MemberRepository;
import com.pda.strategy_service.controller.dto.StrategyResponse.ReadStrategies;
import com.pda.strategy_service.domain.Strategy;
import com.pda.strategy_service.domain.dto.StrategyDto;
import com.pda.strategy_service.repository.StrategyRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StrategyServiceImpl implements StrategyService {
    private final MemberStockRepository memberStockRepository;
    private final MemberRepository memberRepository;
    private final StrategyRepository strategyRepository;

    @Override
    public ReadStrategies getStrategies(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(ResponseMessage.MEMBER_NOT_FOUND));
        List<MemberStock> memberStocks = memberStockRepository.findByMember(member);
        List<StrategyDto> strategyDtos = new ArrayList<>();

        for (MemberStock memberStock : memberStocks) {
            Stock stock = memberStock.getStock();
            StockInfo stockInfo = stock.toDto();
            List<Strategy> strategies = strategyRepository.findAllByMemberStock(memberStock);

            for (Strategy strategy : strategies) {
                BigDecimal profitAmount = strategy.getStrategyProfitSummary()
                        .getCurrentPrice()
                        .subtract(strategy.getStrategyProfitSummary().getAvgBuyPrice());

                StrategyDto strategyDto = strategy.toDto(stockInfo, profitAmount);
                strategyDtos.add(strategyDto);
            }
        }
        return new ReadStrategies(strategyDtos);
    }
}
