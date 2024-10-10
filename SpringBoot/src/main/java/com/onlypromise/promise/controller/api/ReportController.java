package com.onlypromise.promise.controller.api;

import com.onlypromise.promise.DTO.api.ReportDTO;
import com.onlypromise.promise.domain.Report;
import com.onlypromise.promise.domain.User;
import com.onlypromise.promise.service.ReportService;
import com.onlypromise.promise.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ReportController {

    private final ReportService reportService;
    private final UserService userService;

    @PostMapping(value = "/addReport", produces = "application/json")
    public ResponseEntity<Map<String, String>> addReport(@Valid @RequestBody ReportDTO reportDTO)
    {
        Map<String, String> response = new HashMap<>();

        Optional<User> findUser = userService.findUserByBottleId(reportDTO.getBottleId());
        if(findUser.isEmpty())
        {
            response.put("message", "user not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        try
        {
            User user = findUser.get();

            Report newReport = Report.builder().user(user).title(reportDTO.getTitle()).content(reportDTO.getContent()).build();
            reportService.save(newReport);
            response.put("message", "success");

            return ResponseEntity.status(HttpStatus.OK).body(response);
        }
        catch (Exception e)
        {
            response.put("message", "server error");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    //검증 오류시 오류 메시지 반환하는 핸들러
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        // 검증 오류 발생한 필드와 메시지를 응답에 포함
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put("message", error.getDefaultMessage());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }
}
