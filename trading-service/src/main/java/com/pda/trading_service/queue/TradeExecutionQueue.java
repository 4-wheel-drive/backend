package com.pda.trading_service.queue;

import com.pda.trading_service.domain.order.StockOrder;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.springframework.stereotype.Component;

@Component
public class TradeExecutionQueue {
    private final BlockingQueue<StockOrder> queue = new LinkedBlockingQueue<>();

    public void enqueue(StockOrder order) {
        if (!queue.contains(order)) {
            queue.offer(order);
        }
    }

    public StockOrder dequeue() throws InterruptedException {
        return queue.take();
    }
}

