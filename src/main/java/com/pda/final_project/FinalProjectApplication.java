package com.pda.final_project;

import java.sql.SQLException;
import org.h2.tools.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FinalProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinalProjectApplication.class, args);
	}

	@Bean(initMethod = "start", destroyMethod = "stop")
	public Server h2TcpServer() throws SQLException {
		return Server.createTcpServer(
				"-tcp",
				"-tcpAllowOthers",
				"-tcpPort", "9092",
				"-ifNotExists"
		);
	}

	// 웹 콘솔 서버 (브라우저에서 보는 용도)
	@Bean(initMethod = "start", destroyMethod = "stop")
	public Server h2WebServer() throws SQLException {
		return Server.createWebServer(
				"-web", "-webAllowOthers", "-webPort", "8082"
		);
	}
}
