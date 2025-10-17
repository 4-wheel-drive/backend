package com.pda.trading_service.batch.orderExecution;

import com.pda.trading_service.domain.execution.TradeExecution;
import com.pda.trading_service.repository.TradeExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderExecutionWriter implements ItemWriter<TradeExecution> {
    private final TradeExecutionRepository tradeExecutionRepository;

    @Override
    public void write(Chunk<? extends TradeExecution> chunk) {
        tradeExecutionRepository.saveAll(chunk.getItems());
        log.info("{}건 주문 상태 저장 완료", chunk.size());
    }
}

