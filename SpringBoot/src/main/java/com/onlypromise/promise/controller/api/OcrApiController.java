package com.onlypromise.promise.controller.api;

import com.onlypromise.promise.domain.User;
import com.onlypromise.promise.service.OcrProcessor;
import com.onlypromise.promise.service.UserService;
import com.onlypromise.promise.service.VisionService;
import com.onlypromise.promise.DTO.MedicationDTO;
import com.onlypromise.promise.DTO.api.MedicineDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class OcrApiController {

    private final OcrProcessor ocrProcessor;
    private final UserService userService;
    private final VisionService visionService;

    @PostMapping("/extract-text")
    public ResponseEntity<Map<String, Object>> extractTextFromImage(@RequestParam("file") MultipartFile file, @RequestParam("bottleId") String bottleId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<User> findUser = userService.findUserByBottleId(bottleId);
            if(findUser.isEmpty())
            {
                response.put("message", "user not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            User user = findUser.get();

            String extractedText = visionService.extractTextFromImage(file);
            List<MedicationDTO> dtoList = ocrProcessor.buildMedicationDtoList(extractedText);
            List<MedicineDTO> validDtoList = ocrProcessor.filterAndBuildValidDtoList(dtoList, user);
            List<String> warningMessages = ocrProcessor.saveNotifications(user, validDtoList);

            response.put("data", validDtoList);
            response.put("warningMessage", warningMessages);
            log.info("처방전 인식 성공 user_absolute_id : {}", user.getId());
            return ResponseEntity.ok(response);

        }
        catch (Exception e) {
            // 그 외의 예외 발생 시
            log.error("Error processing OCR extraction: ", e);
            response.put("error", "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}