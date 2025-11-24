package io.hhplus.ECommerce.ECommerce_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class ECommerceProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(ECommerceProjectApplication.class, args);
	}

}
