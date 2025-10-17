package com.pda.trading_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
		"com.pda.trading_service",
		"com.pda.common_service"
})
@EnableJpaAuditing
@EnableScheduling
public class TradingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradingServiceApplication.class, args);
	}

}
