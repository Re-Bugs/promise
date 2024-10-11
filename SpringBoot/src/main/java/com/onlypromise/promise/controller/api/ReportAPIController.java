package com.onlypromise.promise.controller.api;

import com.onlypromise.promise.DTO.api.ReportDTO;
import com.onlypromise.promise.domain.Image;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ReportAPIController {

    private final ReportService reportService;
    private final UserService userService;

    @PostMapping(value = "/addReport", produces = "application/json", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, String>> addReport(@RequestPart @Valid ReportDTO reportDTO, @RequestPart(value = "imageFile", required = false) MultipartFile imageFile)
    {
        Map<String, String> response = new HashMap<>();

        Optional<User> findUser = userService.findUserByBottleId(reportDTO.getBottleId());
        if (findUser.isEmpty())
        {
            response.put("message", "user not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        try
        {
            User user = findUser.get();
            Image image = null;

            if (imageFile != null && !imageFile.isEmpty())
            {
                String extension = getFileExtension(imageFile.getOriginalFilename());
                if (!isImageFile(extension)) {
                    response.put("message", "invalid image file");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }

                String uuidFileName = UUID.randomUUID().toString();
                String fileName = uuidFileName + "." + extension;

                // 현재 작업 디렉토리 내 이미지 폴더에 저장
                String projectPath = System.getProperty("user.dir");
                String imagePath = projectPath + "/report-images/"; // 이미지 저장 경로
                File directory = new File(imagePath);
                if (!directory.exists()) directory.mkdirs(); //디렉터리가 없다면 생성

                String absolutePath = imagePath + fileName;
                imageFile.transferTo(new File(absolutePath));

                image = Image.builder()
                        .name(fileName)
                        .path(absolutePath)
                        .build();

                reportService.saveImage(image);
            }

            Report newReport = Report.builder()
                    .user(user)
                    .title(reportDTO.getTitle())
                    .content(reportDTO.getContent())
                    .image(image)
                    .createAt(LocalDateTime.now())
                    .build();

            reportService.save(newReport);
            response.put("message", "success");

            return ResponseEntity.status(HttpStatus.OK).body(response);
        }
        catch (Exception e)
        {
            log.error("Error while processing report: ", e);
            response.put("message", "server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    //검증 오류시 오류 메시지 반환하는 핸들러
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex)
    {
        Map<String, String> errors = new HashMap<>();

        // 검증 오류 발생한 필드와 메시지를 응답에 포함
        for (FieldError error : ex.getBindingResult().getFieldErrors()) errors.put("message", error.getDefaultMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    // 파일 확장자 추출 메서드
    private String getFileExtension(String filename)
    {
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    // 지정된 확장자가 맞는지 검사
    private boolean isImageFile(String extension)
    {
        String[] allowedExtensions = {"jpg", "jpeg", "png"};
        return Arrays.asList(allowedExtensions).contains(extension.toLowerCase());
    }
}
