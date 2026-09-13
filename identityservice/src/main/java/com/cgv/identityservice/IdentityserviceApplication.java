package com.cgv.identityservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(
		scanBasePackages = {"com.cgv.identityservice", "com.cgv.commondto"}
//		exclude = {org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration.class}
)
@EnableFeignClients
public class IdentityserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(IdentityserviceApplication.class, args);
	}

}
