package com.promise.promise.api.controller;

import com.promise.promise.api.DTO.SignUpDTO;
import com.promise.promise.api.service.APIUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApiSignUpController {

    private final APIUserService apiUserService;

    @PostMapping(value = "/sign_up", produces = "application/json")
    public ResponseEntity<Map<String, String>> signUp(@Valid @RequestBody SignUpDTO signUpDTO) {
        Map<String, String> response = apiUserService.signUp(signUpDTO);

        if ("Duplicate bottle code".equals(response.get("message"))) {
            return ResponseEntity.status(409).body(response); // HTTP 409 Conflict
        } else if ("fail".equals(response.get("message"))) {
            return ResponseEntity.status(500).body(response); // HTTP 500 Internal Server Error
        }

        return ResponseEntity.ok(response); // HTTP 200 OK
    }

    //글로벌 예외처리기
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errors.put("message", error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors); // HTTP 400 Bad Request
    }
}