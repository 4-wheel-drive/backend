package com.pda.trading_service.batch.orderExecution;

import com.pda.trading_service.domain.execution.TradeExecution;
import com.pda.trading_service.domain.order.StockOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
@EnableBatchProcessing
public class OrderExecutionJobConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final CreatedOrderReader createdOrderReader;
    private final OrderExecutionProcessor orderExecutionProcessor;
    private final OrderExecutionWriter orderExecutionWriter;


    @Bean
    public Job orderExecutionCheckJob() {
        return new JobBuilder("orderExecutionCheckJob", jobRepository)
                .start(orderExecutionStep())
                .build();
    }

    @Bean
    public Step orderExecutionStep() {
        return new StepBuilder("orderExecutionStep", jobRepository)
                .<StockOrder, TradeExecution>chunk(20, transactionManager)
                .reader(createdOrderReader)
                .processor(orderExecutionProcessor)
                .writer(orderExecutionWriter)
                .build();
    }
}
