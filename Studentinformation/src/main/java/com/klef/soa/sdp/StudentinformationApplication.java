package com.klef.soa.sdp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.klef.soa.sdp.client")
public class StudentinformationApplication {

	public static void main(String[] args) {
		SpringApplication.run(StudentinformationApplication.class, args);
		System.out.println("EduPulse Student Information Microservice started on port 8082!");
	}

}
