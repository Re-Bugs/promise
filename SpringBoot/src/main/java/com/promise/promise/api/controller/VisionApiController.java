package com.promise.promise.api.controller;

import com.promise.promise.api.service.UserAPIService;
import com.promise.promise.domain.Medicine;
import com.promise.promise.domain.Notification;
import com.promise.promise.domain.User;
import com.promise.promise.domain.enumeration.DailyDose;
import com.promise.promise.service.NotificationService;
import com.promise.promise.service.VisionService;
import com.promise.promise.web.controller.ocr.MedicationDTO;
import com.promise.promise.web.controller.ocr.MedicineDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class VisionApiController {

    private final VisionService visionService;
    private final NotificationService notificationService;
    private final UserAPIService userService; // 유저를 조회할 서비스 추가

    // 정규식 패턴을 클래스 레벨에서 미리 컴파일하여 재사용
    private static final Pattern NINE_DIGIT_PATTERN = Pattern.compile("\\b\\d{9}\\b");
    private static final Pattern BETWEEN_PATTERN = Pattern.compile("명칭(.*?)주사제", Pattern.DOTALL);
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\b(\\d0)\\b(?!\\s*[a-zA-Z㎎])");
    private static final Pattern MEAL_SET_PATTERN = Pattern.compile("(하루\\s*\\d+[회번]\\).*?(아침|점심|저녁)(/.*?(아침|점심|저녁))?\\s*식(사후)?)");
    private static final Pattern DAY_NUMBER_PATTERN = Pattern.compile("하루\\s*(1|2|3)\\s*[회번]");
    private static final Pattern MEAL_PATTERN = Pattern.compile("(아침|점심|저녁)");

    // API로 사진 파일을 전송받아 처리, bottleId를 쿼리 파라미터로 받음
    @PostMapping("/extract-text")
    public ResponseEntity<Map<String, Object>> extractTextFromImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("bottleId") String bottleId) {

        Map<String, Object> response = new HashMap<>();
        try {
            // bottleId로 사용자 조회
            User user = userService.findUserByBottleId(bottleId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 bottleId로 사용자를 찾을 수 없습니다: " + bottleId));

            // 이미지에서 텍스트 추출
            String extractedText = visionService.extractTextFromImage(file);
            List<MedicationDTO> dtoList = buildMedicationDtoList(extractedText);
            List<MedicineDTO> validDtoList = filterAndBuildValidDtoList(dtoList, user);

            for (MedicineDTO medicineDTO : validDtoList) {
                // Notification 객체 생성 및 설정
                Notification.NotificationBuilder notificationBuilder = Notification.builder();
                notificationBuilder.user(user);

                // medicine_id 설정 (medicineId를 사용하여 Medicine 조회)
                Medicine medicine = visionService.getMedicineById(medicineDTO.getMedicineId())
                        .orElseThrow(() -> new IllegalArgumentException("해당 ID의 약물이 존재하지 않습니다: " + medicineDTO.getMedicineId()));
                notificationBuilder.medicine(medicine);

                // daily_dose 설정
                String dailyDosageTimes = medicineDTO.getDailyDosageTimes();
                log.info("Daily dosage times: {}", dailyDosageTimes);

                // 숫자 문자열을 Enum 값으로 변환하여 저장
                if ("1".equals(dailyDosageTimes)) {
                    notificationBuilder.dailyDose(DailyDose.one);
                } else if ("2".equals(dailyDosageTimes)) {
                    notificationBuilder.dailyDose(DailyDose.two);
                } else if ("3".equals(dailyDosageTimes)) {
                    notificationBuilder.dailyDose(DailyDose.three);
                } else {
                    throw new IllegalArgumentException("Invalid daily dosage times: " + dailyDosageTimes);
                }

                // morning, afternoon, evening 설정
                List<String> mealTimes = medicineDTO.getMealTimes();
                setNotificationTimes(notificationBuilder, mealTimes);

                // 생성일 설정 (현재 날짜)
                notificationBuilder.createdAt(LocalDate.now());

                // 재처방일 설정 (현재 날짜 + totalDosageDays)
                int totalDosageDays = Integer.parseInt(medicineDTO.getTotalDosageDays());
                notificationBuilder.renewalDate(LocalDate.now().plusDays(totalDosageDays));

                // 남은 약물 수 설정
                notificationBuilder.remainingDose((short) totalDosageDays);

                // total 설정 (총 투약일수)
                notificationBuilder.total((short) totalDosageDays);

                // 알림 객체 저장
                Notification notification = notificationBuilder.build();
                notificationService.save(notification);
            }

            response.put("status", "success");
            response.put("data", validDtoList);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("message", "fail");
            response.put("errorMessage", "오류가 발생했습니다. 다시 시도해 주세요.");
            log.error("OCR error = {}", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Notification 설정
    private void setNotificationTimes(Notification.NotificationBuilder notificationBuilder, List<String> mealTimes) {
        notificationBuilder.morning(false);
        notificationBuilder.afternoon(false);
        notificationBuilder.evening(false);

        for (String mealTime : mealTimes) {
            String[] meals = mealTime.split(",");
            for (String meal : meals) {
                meal = meal.trim();
                if (meal.equals("아침")) {
                    notificationBuilder.morning(true);
                } else if (meal.equals("점심")) {
                    notificationBuilder.afternoon(true);
                } else if (meal.equals("저녁")) {
                    notificationBuilder.evening(true);
                }
            }
        }
    }

    // 약품 코드의 개수를 기준으로 MedicationDTO 리스트 생성
    private List<MedicationDTO> buildMedicationDtoList(String extractedText) {
        List<String> nineDigitNumbers = extractNineDigitNumbers(extractedText);
        List<String> dayAndMealSet = extractDayAndMealSet(extractedText);
        List<String> twoDigitNumbers = extractValidTwoDigitNumbers(extractedText);
        List<String> dailyDosageTimes = extractDayNumber(extractedText);
        List<String> mealTimes = extractMealTimesFromList(dayAndMealSet);

        List<MedicationDTO> dtoList = new ArrayList<>();
        int size = nineDigitNumbers.size();

        for (int i = 0; i < size; ++i) {
            MedicationDTO dto = new MedicationDTO();
            dto.setMedicationCode(nineDigitNumbers.get(i));

            dto.setTotalDosageDays(getValueOrDefault(twoDigitNumbers, i, "0"));
            dto.setDailyDosageTimes(getValueOrDefault(dailyDosageTimes, i, "0"));
            dto.setMealTimes(List.of(getValueOrDefault(mealTimes, i, "없음")));

            dtoList.add(dto);
        }

        return dtoList;
    }

    // 필터링된 MedicationDTO 리스트 생성
    private List<MedicineDTO> filterAndBuildValidDtoList(List<MedicationDTO> dtoList, User user) {
        List<MedicineDTO> validDtoList = new ArrayList<>();
        for (MedicationDTO dto : dtoList) {
            if (isValidDto(dto)) {
                Optional<Medicine> medicine = visionService.getMedicineByProductCode(dto.getMedicationCode());
                medicine.ifPresent(med -> {
                    MedicineDTO medicineDTO = new MedicineDTO(
                            med.getId(),
                            dto.getTotalDosageDays(),
                            dto.getDailyDosageTimes(),
                            dto.getMealTimes()
                    );
                    logDto(medicineDTO, user);
                    validDtoList.add(medicineDTO);
                });
            }
        }
        return validDtoList;
    }

    // 기타 메서드
    private boolean isValidDto(MedicationDTO dto) {
        return dto.getMedicationCode() != null &&
                dto.getTotalDosageDays() != null &&
                dto.getDailyDosageTimes() != null &&
                dto.getMealTimes() != null && !dto.getMealTimes().isEmpty();
    }

    private void logDto(MedicineDTO dto, User user) {
        log.info("---------------add Medicine---------------");
        log.info("user Absolute ID: {}", user.getId());
        log.info("Medicine ID: {}", dto.getMedicineId());
        log.info("Total Dosage Days: {}", dto.getTotalDosageDays());
        log.info("Daily Dosage Times: {}", dto.getDailyDosageTimes());
        log.info("Meal Times: {}", dto.getMealTimes());
    }

    // 추출 메서드
    private List<String> extractNineDigitNumbers(String text) {
        List<String> nineDigitNumbers = new ArrayList<>();
        Matcher betweenMatcher = BETWEEN_PATTERN.matcher(text);
        if (betweenMatcher.find()) {
            String betweenWords = betweenMatcher.group(1);
            Matcher matcher = NINE_DIGIT_PATTERN.matcher(betweenWords);
            while (matcher.find()) {
                nineDigitNumbers.add(matcher.group());
            }
        }
        return nineDigitNumbers;
    }

    public List<String> extractValidTwoDigitNumbers(String text) {
        List<String> validNumbers = new ArrayList<>();
        Matcher betweenMatcher = BETWEEN_PATTERN.matcher(text);
        if (betweenMatcher.find()) {
            String betweenWords = betweenMatcher.group(1);
            Matcher digitMatcher = DIGIT_PATTERN.matcher(betweenWords);
            while (digitMatcher.find()) {
                validNumbers.add(digitMatcher.group(1));
            }
        }
        return validNumbers;
    }

    public List<String> extractDayAndMealSet(String text) {
        List<String> dayAndMealSet = new ArrayList<>();
        Matcher mealSetMatcher = MEAL_SET_PATTERN.matcher(text);
        while (mealSetMatcher.find()) {
            dayAndMealSet.add(mealSetMatcher.group());
        }
        return dayAndMealSet;
    }

    public List<String> extractDayNumber(String text) {
        List<String> dayNumbers = new ArrayList<>();
        Matcher dayNumberMatcher = DAY_NUMBER_PATTERN.matcher(text);
        while (dayNumberMatcher.find()) {
            dayNumbers.add(dayNumberMatcher.group(1));
        }
        return dayNumbers;
    }

    public List<String> extractMealTimesFromList(List<String> lines) {
        List<String> mealTimes = new ArrayList<>();
        for (String line : lines) {
            Matcher mealMatcher = MEAL_PATTERN.matcher(line);
            List<String> mealsInLine = new ArrayList<>();
            while (mealMatcher.find()) {
                mealsInLine.add(mealMatcher.group(1));
            }
            if (!mealsInLine.isEmpty()) {
                mealTimes.add(String.join(",", mealsInLine));
            }
        }
        return mealTimes;
    }

    // 기본값 처리를 위한 메서드
    private String getValueOrDefault(List<String> list, int index, String defaultValue) {
        return (index < list.size()) ? list.get(index) : defaultValue;
    }
}