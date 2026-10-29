package com.klef.soa.sdp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
		System.out.println("EduPulse API Gateway started on port 8080!");
		System.out.println("Routes:");
		System.out.println("  /api/students/**  -> STUDENT-SERVICE       :8082");
		System.out.println("  /api/attendance/**-> ATTENDANCE-SERVICE   :8083");
		System.out.println("  /api/results/**   -> RESULT-SERVICE       :8084");
	}
}
