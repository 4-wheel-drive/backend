package com.pda.strategy_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {
		"com.pda.strategy_service",
		"com.pda.common_service"
})
@EnableJpaRepositories(basePackages = {
		"com.pda.strategy_service.repository.jpa",
		"com.pda.common_service.user.repository",
		"com.pda.common_service.stock.repository"
})
@EnableMongoRepositories(basePackages = {
		"com.pda.strategy_service.repository.mongodb"
})
@EnableJpaAuditing
public class StrategyServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(StrategyServiceApplication.class, args);
	}

}
