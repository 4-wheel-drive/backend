package com.pda.trading_service.batch.orderExecution;

import com.pda.trading_service.domain.order.OrderStatus;
import com.pda.trading_service.domain.order.StockOrder;
import com.pda.trading_service.repository.StockOrderRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreatedOrderReader implements ItemReader<StockOrder> {
    private final StockOrderRepository stockOrderRepository;
    private Iterator<StockOrder> iterator;

    @Override
    public StockOrder read() {
        if (iterator == null) {
            List<StockOrder> createdOrders = stockOrderRepository.findByStatusAndCreateAtToday(OrderStatus.CREATED);
            iterator = createdOrders.iterator();
        }
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }
}
