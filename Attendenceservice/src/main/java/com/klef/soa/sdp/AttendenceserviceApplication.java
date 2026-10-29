package com.klef.soa.sdp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class AttendenceserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AttendenceserviceApplication.class, args);
		System.out.println("EduPulse Attendance Tracking Microservice started on port 8083!");
	}

}
