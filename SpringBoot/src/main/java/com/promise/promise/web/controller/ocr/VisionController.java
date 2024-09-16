package com.promise.promise.web.controller.ocr;

import com.promise.promise.domain.Medicine;
import com.promise.promise.service.VisionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Controller
@RequestMapping("ocr")
public class VisionController {

    private final VisionService visionService;

    // 정규식 패턴을 클래스 레벨에서 미리 컴파일하여 재사용
    private static final Pattern NINE_DIGIT_PATTERN = Pattern.compile("\\b\\d{9}\\b");
    private static final Pattern BETWEEN_PATTERN = Pattern.compile("명칭(.*?)주사제", Pattern.DOTALL);
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\b(\\d0)\\b(?!\\s*[a-zA-Z㎎])");
    private static final Pattern MEAL_SET_PATTERN = Pattern.compile("(하루\\s*\\d+[회번]\\).*?(아침|점심|저녁)(/.*?(아침|점심|저녁))?\\s*식(사후)?)");
    private static final Pattern DAY_NUMBER_PATTERN = Pattern.compile("하루\\s*(1|2|3)\\s*[회번]");
    private static final Pattern MEAL_PATTERN = Pattern.compile("(아침|점심|저녁)");

    public VisionController(VisionService visionService) {
        this.visionService = visionService;
    }

    @GetMapping("/upload")
    public String showUploadForm() {
        return "ocr/upload";
    }

    @PostMapping("/extract-text")
    public String extractTextFromImage(MultipartFile file, Model model, RedirectAttributes redirectAttributes) {
        try {
            String extractedText = visionService.extractTextFromImage(file);
            List<MedicationDTO> dtoList = buildMedicationDtoList(extractedText);

            List<MedicineDTO> validDtoList = filterAndBuildValidDtoList(dtoList);
            redirectAttributes.addFlashAttribute("dtoList", validDtoList);  // redirect 시 FlashAttribute로 데이터 전달
        } catch (Exception e) {
            redirectAttributes.addAttribute("errorMessage", "오류가 발생했습니다. 다시 시도해 주세요.");
            log.error("OCR error = {}", e.getMessage());
        }
        return "redirect:/ocr/result";  // forward 대신 redirect 사용
    }

    @GetMapping("/result")
    public String showResult(Model model) {
        List<MedicineDTO> dtoList = (List<MedicineDTO>) model.getAttribute("dtoList");

        if (dtoList != null && !dtoList.isEmpty()) {
            for (MedicineDTO dto : dtoList) {
                Optional<Medicine> medicine = visionService.getMedicineById(dto.getMedicineId());
                medicine.ifPresent(med -> {
                    dto.setName(med.getName());
                    dto.setCategory(med.getCategory());
                });
            }
            model.addAttribute("dtoList", dtoList);
        }
        return "ocr/result";
    }

    // 약품 코드의 개수를 기준으로 MedicationDTO 리스트 생성
    private List<MedicationDTO> buildMedicationDtoList(String extractedText) {
        List<String> nineDigitNumbers = extractNineDigitNumbers(extractedText); // 약품 코드 리스트
        List<String> dayAndMealSet = extractDayAndMealSet(extractedText);       // 식사 세트 리스트
        List<String> twoDigitNumbers = extractValidTwoDigitNumbers(extractedText); // 총 투약일수 리스트
        List<String> dailyDosageTimes = extractDayNumber(extractedText);        // 하루 몇 회 복용 리스트
        List<String> mealTimes = extractMealTimesFromList(dayAndMealSet);       // 식사 시간 리스트

        List<MedicationDTO> dtoList = new ArrayList<>();
        int size = nineDigitNumbers.size(); // 약품 코드 개수를 기준으로 처리

        for (int i = 0; i < size; ++i) {
            MedicationDTO dto = new MedicationDTO();
            dto.setMedicationCode(nineDigitNumbers.get(i));

            // 나머지 데이터는 부족한 경우 기본값 처리
            dto.setTotalDosageDays(getValueOrDefault(twoDigitNumbers, i, "0"));
            dto.setDailyDosageTimes(getValueOrDefault(dailyDosageTimes, i, "0"));
            dto.setMealTimes(List.of(getValueOrDefault(mealTimes, i, "없음")));

            dtoList.add(dto);
        }

        return dtoList;
    }

    // 기본값 처리를 위한 메서드
    private String getValueOrDefault(List<String> list, int index, String defaultValue) {
        return (index < list.size()) ? list.get(index) : defaultValue;
    }

    // MedicationDTO 리스트를 기반으로 값이 누락되지 않은 DTO 객체만 필터링
    private List<MedicineDTO> filterAndBuildValidDtoList(List<MedicationDTO> dtoList) {
        List<MedicineDTO> validDtoList = new ArrayList<>();
        for (MedicationDTO dto : dtoList) {
            if (isValidDto(dto)) {
                Optional<Medicine> medicine = visionService.getMedicineByProductCode(dto.getMedicationCode());
                medicine.ifPresent(med -> {
                    MedicineDTO medicineDTO = new MedicineDTO(
                            med.getId(),
                            dto.getTotalDosageDays(),
                            dto.getDailyDosageTimes(),
                            dto.getMealTimes(),
                            med.getName(),
                            med.getCategory()
                    );
                    logDto(medicineDTO);
                    validDtoList.add(medicineDTO);
                });
            }
        }
        return validDtoList;
    }

    private boolean isValidDto(MedicationDTO dto) {
        return dto.getMedicationCode() != null &&
                dto.getTotalDosageDays() != null &&
                dto.getDailyDosageTimes() != null &&
                dto.getMealTimes() != null && !dto.getMealTimes().isEmpty();
    }

    private void logDto(MedicineDTO dto) {
        log.info("Medicine ID: {}", dto.getMedicineId());
        log.info("Total Dosage Days: {}", dto.getTotalDosageDays());
        log.info("Daily Dosage Times: {}", dto.getDailyDosageTimes());
        log.info("Meal Times: {}", dto.getMealTimes());
    }

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
}