package com.pda.trading_service.batch.orderExecution;

import com.pda.trading_service.config.StrategyModuleClient;
import com.pda.trading_service.controller.dto.StrategyWithMemberDto;
import com.pda.trading_service.domain.execution.TradeExecution;
import com.pda.trading_service.domain.order.StockOrder;
import com.pda.trading_service.service.kis.KisTradeExecutionService;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class OrderExecutionProcessor implements ItemProcessor<StockOrder, TradeExecution> {

    private final KisTradeExecutionService kisTradeExecutionService;
    private final StrategyModuleClient strategyModuleClient;

    @Override
    public TradeExecution process(StockOrder stockOrder) {
        LocalDate now = LocalDate.now(ZoneId.of("Asia/Seoul"));

        StrategyWithMemberDto strategyInfo = strategyModuleClient.getStrategyInfo(stockOrder.getStrategyId());
        return kisTradeExecutionService.checkTradeExecution(stockOrder, now, strategyInfo);
    }
}
