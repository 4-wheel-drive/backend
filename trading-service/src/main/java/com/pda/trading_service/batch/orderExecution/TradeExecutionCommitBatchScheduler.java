package com.pda.trading_service.batch.orderExecution;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeExecutionCommitBatchScheduler {
    private final JobLauncher jobLauncher;
    private final Job orderExecutionCheckJob;

    @Scheduled(cron = "0 32 15 * * *", zone = "Asia/Seoul")
    public void runJob() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        log.info("체결 배치 실행 시작");
        jobLauncher.run(orderExecutionCheckJob, jobParameters);
    }
}