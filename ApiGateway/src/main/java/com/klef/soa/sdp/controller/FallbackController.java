package com.klef.soa.sdp.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/students")
    public ResponseEntity<Map<String, String>> studentFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "503",
                        "error", "Service Unavailable",
                        "message", "Student Information Service is currently unavailable. Please try again later."));
    }

    @GetMapping("/attendance")
    public ResponseEntity<Map<String, String>> attendanceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "503",
                        "error", "Service Unavailable",
                        "message", "Attendance Service is currently unavailable. Please try again later."));
    }

    @GetMapping("/results")
    public ResponseEntity<Map<String, String>> resultFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "503",
                        "error", "Service Unavailable",
                        "message", "Result Service is currently unavailable. Please try again later."));
    }
}
