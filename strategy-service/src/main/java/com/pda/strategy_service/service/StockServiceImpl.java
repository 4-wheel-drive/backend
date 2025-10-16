package com.pda.strategy_service.service;

import com.pda.common_service.stock.Stock;
import com.pda.common_service.stock.repository.StockRepository;
import com.pda.common_service.user.domain.Member;
import com.pda.common_service.user.repository.MemberRepository;
import com.pda.strategy_service.controller.dto.StockResponse.ReadStocks;
import com.pda.strategy_service.controller.dto.StockResponse.StockItem;
import com.pda.strategy_service.domain.Strategy;
import com.pda.strategy_service.repository.jpa.StrategyRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockServiceImpl implements StockService {
    
    private final StockRepository stockRepository;
    private final StrategyRepository strategyRepository;
    private final MemberRepository memberRepository;
    
    @Override
    public ReadStocks getAllStocks() {
        List<Stock> stocks = stockRepository.findAll();
        List<StockItem> stockItems = stocks.stream()
                .map(stock -> {
                    var stockInfo = stock.toDto();
                    return new StockItem(
                            stockInfo.stockName(),
                            stockInfo.stockCode(),
                            stockInfo.stockImgUri()
                    );
                })
                .toList();
        
        return new ReadStocks(stockItems);
    }
    
    @Override
    public ReadStocks getMyStocks(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        
        List<Strategy> strategies = strategyRepository.findAllByMemberId(memberId);
        
        List<Stock> uniqueStocks = strategies.stream()
                .map(Strategy::getStock)
                .filter(stock -> stock != null)
                .distinct()
                .toList();
        
        List<StockItem> stockItems = uniqueStocks.stream()
                .map(stock -> {
                    var stockInfo = stock.toDto();
                    return new StockItem(
                            stockInfo.stockName(),
                            stockInfo.stockCode(),
                            stockInfo.stockImgUri()
                    );
                })
                .toList();
        
        return new ReadStocks(stockItems);
    }
}

