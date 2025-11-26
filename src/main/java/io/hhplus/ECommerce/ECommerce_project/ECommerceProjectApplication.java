package io.hhplus.ECommerce.ECommerce_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableRetry
@EnableScheduling  // StockSyncScheduler 동작
@EnableAsync       // StockEventListener 비동기 처리 동작
public class ECommerceProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(ECommerceProjectApplication.class, args);
	}

}
