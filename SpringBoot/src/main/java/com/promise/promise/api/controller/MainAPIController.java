package com.promise.promise.api.controller;

import com.promise.promise.api.service.MedicationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MainAPIController {
    private final MedicationLogService medicationLogService;

    @PostMapping("/{bottleId}")
    public ResponseEntity<Map<String, String>> updateMedicationStatusByBottleId(@PathVariable String bottleId) {
        String responseMessage = medicationLogService.updateMedicationStatusByBottleId(bottleId);

        // JSON 응답을 위한 Map 생성
        Map<String, String> response = new HashMap<>();
        response.put("message", responseMessage);

        if (responseMessage.equals("You already have a history of taking it at that time.")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response); // 409 Conflict
        } else if (responseMessage.equals("bottleId not found.")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response); // 404 Not Found
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(response); // 200 OK
        }
    }
}