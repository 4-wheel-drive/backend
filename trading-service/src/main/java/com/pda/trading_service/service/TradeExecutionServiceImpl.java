package com.pda.trading_service.service;

import com.pda.common_service.user.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TradeExecutionServiceImpl implements TradeExecutionService {

    @Override
    public void getTradeExecution(Long memberId, Long strategyId) {

    }
}
