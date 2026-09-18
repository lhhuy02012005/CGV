package com.cgv.marketingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
//		scanBasePackages = {
//				"com.cgv.marketingservice",
//				"com.cgv.commondto"
//		}
)
public class MarketingserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MarketingserviceApplication.class, args);
	}

}
