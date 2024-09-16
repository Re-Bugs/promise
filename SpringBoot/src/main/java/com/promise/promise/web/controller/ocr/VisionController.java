package com.promise.promise.web.controller;

import com.promise.promise.service.VisionService;
import com.promise.promise.web.controller.ocr.MedicationDTO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequestMapping("ocr")
public class VisionController {

    private final VisionService visionService;  // final로 선언

    // 생성자를 통해 VisionService를 주입
    public VisionController(VisionService visionService) {
        this.visionService = visionService;
    }

    @GetMapping("/upload")
    public String showUploadForm() {
        return "ocr/upload";
    }

    @PostMapping("/extract-text")
    public String extractTextFromImage(MultipartFile file, Model model) {
        try {
            // VisionService에서 텍스트 추출
            String extractedText = visionService.extractTextFromImage(file);
            //System.out.println(extractedText);

            // 9자리 숫자를 정규표현식으로 추출
            List<String> nineDigitNumbers = extractNineDigitNumbers(extractedText);
            //for (String i : nineDigitNumbers) System.out.println(i + " ");

            // 문장 추출
            List<String> test = extractDayAndMealSet(extractedText);
            //for (String i : test) System.out.println(i + " ");

            // 총 투약일수
            List<String> twoDigitNumbersBetweenWords = extractValidTwoDigitNumbers(extractedText);
            //for (String i : twoDigitNumbersBetweenWords) System.out.println(i + " ");

            // 하루 몇 회 추출
            List<String> test2 = extractDayNumber(extractedText);
            //for (String i : test2) System.out.println(i + " ");

            // 식사 시간 추출
            List<String> test3 = extractMealTimesFromList(test);
            //for (String i : test3) System.out.println(i + " ");

            // DTO 리스트 생성
            List<MedicationDTO> dtoList = new ArrayList<>();

            // 각 리스트의 최소 크기로 데이터 맞추기 (최소 길이만큼 반복)
            int size = nineDigitNumbers.size();

            for (int i = 0; i < size; ++i) {
                MedicationDTO newDto = new MedicationDTO();

                // 각 DTO 필드에 값 매핑
                newDto.setMedicationCode(nineDigitNumbers.get(i));            // 약품 코드
                newDto.setTotalDosageDays(twoDigitNumbersBetweenWords.get(i)); // 총 투약일수
                newDto.setDailyDosageTimes(test2.get(i));                      // 하루 몇 회
                newDto.setMealTimes(List.of(test3.get(i)));                    // 식사 시간 리스트로 설정

                // DTO 리스트에 추가
                dtoList.add(newDto);
            }

            for(MedicationDTO dto : dtoList)
            {
                System.out.println(dto.getMedicationCode());
                System.out.println(dto.getTotalDosageDays());
                System.out.println(dto.getDailyDosageTimes());
                System.out.println(dto.getMealTimes());
                System.out.println();
            }



            model.addAttribute("dtoList", dtoList);
        } catch (Exception e) {
            model.addAttribute("text", "Error processing image: " + e.getMessage());
        }
        return "ocr/result"; // 결과를 보여줄 템플릿
    }

    // 명칭과 주사제 사이에서 9자리 숫자를 찾는 메서드
    private List<String> extractNineDigitNumbers(String text) {
        List<String> nineDigitNumbers = new ArrayList<>();

        // "명칭"으로 시작하고 "주사제"로 끝나는 부분을 추출하는 정규표현식
        Pattern betweenPattern = Pattern.compile("명칭(.*?)주사제", Pattern.DOTALL);
        Matcher betweenMatcher = betweenPattern.matcher(text);

        // 명칭과 주사제 사이에 있는 9자리 숫자를 찾기
        if (betweenMatcher.find()) {
            String betweenWords = betweenMatcher.group(1); // 명칭과 주사제 사이의 텍스트

            // 명칭과 주사제 사이에서 9자리 숫자를 추출하는 정규표현식
            Pattern pattern = Pattern.compile("\\b\\d{9}\\b");
            Matcher matcher = pattern.matcher(betweenWords);

            // 일치하는 9자리 숫자 찾기
            while (matcher.find()) {
                nineDigitNumbers.add(matcher.group());
            }
        }

        return nineDigitNumbers;
    }

    // 총 투약일수를 찾는 메서드
    public List<String> extractValidTwoDigitNumbers(String text) {
        List<String> validNumbers = new ArrayList<>();

        // "명칭"으로 시작하고 "주사제"로 끝나는 부분을 추출하는 정규표현식
        Pattern betweenPattern = Pattern.compile("명칭(.*?)주사제", Pattern.DOTALL);
        Matcher betweenMatcher = betweenPattern.matcher(text);

        if (betweenMatcher.find()) {
            // "명칭"과 "주사제" 사이의 텍스트 추출
            String betweenWords = betweenMatcher.group(1);

            // 1의 자리가 0인 2자리 숫자를 찾고 뒤에 단위나 공백을 포함한 경우를 제외
            Pattern digitPattern = Pattern.compile("\\b(\\d0)\\b(?!\\s*[a-zA-Z㎎])");  // 공백 허용
            Matcher digitMatcher = digitPattern.matcher(betweenWords);

            // 매칭된 2자리 숫자들을 리스트에 저장
            while (digitMatcher.find()) {
                validNumbers.add(digitMatcher.group(1));  // 2자리 숫자만 추출
            }
        }

        // 반환된 유효한 숫자 리스트
        return validNumbers;
    }

    // "하루"로 시작하고 "식"으로 끝나는 문자열을 추출하는 메서드
    public List<String> extractDayAndMealSet(String text) {
        List<String> dayAndMealSet = new ArrayList<>();

        // "하루"로 시작하고 "식"으로 끝나는 문자열을 추출하는 정규표현식
        Pattern mealSetPattern = Pattern.compile("(하루\\s*\\d+[회번]\\).*?(아침|점심|저녁)(/.*?(아침|점심|저녁))?\\s*식(사후)?)");
        Matcher mealSetMatcher = mealSetPattern.matcher(text);

        // 매칭된 문자열들을 리스트에 저장
        while (mealSetMatcher.find()) {
            dayAndMealSet.add(mealSetMatcher.group());  // 전체 매칭된 문자열을 추출
        }

        return dayAndMealSet;
    }

    // List<String> 형태인 test 리스트의 각 문자열에서 "하루" 뒤에 나오는 1~3 사이의 숫자를 추출하는 메서드
    public List<String> extractDayNumberFromList(List<String> textList) {
        List<String> extractedNumbers = new ArrayList<>();

        for (String text : textList) {
            // 각 문자열에서 "하루" 뒤에 나오는 1~3 사이의 숫자를 추출하는 메서드 호출
            extractedNumbers.addAll(extractDayNumber(text));
        }

        return extractedNumbers;
    }

    // "하루" 다음에 나오는 1~3 사이의 숫자를 추출하는 메서드
    public List<String> extractDayNumber(String text) {
        List<String> dayNumbers = new ArrayList<>();

        // "하루" 뒤에 1~3 사이의 숫자와 "회" 또는 "번"이 나오는 패턴 추출
        Pattern dayNumberPattern = Pattern.compile("하루\\s*(1|2|3)\\s*[회번]");
        Matcher dayNumberMatcher = dayNumberPattern.matcher(text);

        // 매칭된 숫자들을 리스트에 저장
        while (dayNumberMatcher.find()) {
            dayNumbers.add(dayNumberMatcher.group(1));  // 숫자만 추출
        }

        return dayNumbers;
    }

    // 여러 줄이 들어간 리스트에서 아침, 점심, 저녁을 추출하는 메서드
    public List<String> extractMealTimesFromList(List<String> lines) {
        List<String> mealTimes = new ArrayList<>();

        // 각 줄에 대해 처리
        for (String line : lines) {
            // "아침", "점심", "저녁" 뒤에 추가 텍스트가 와도 매칭되도록 설정
            Pattern mealPattern = Pattern.compile("(아침|점심|저녁)"); // 단어 경계만 매칭
            Matcher mealMatcher = mealPattern.matcher(line);

            List<String> mealsInLine = new ArrayList<>();
            while (mealMatcher.find()) {
                mealsInLine.add(mealMatcher.group(1));  // "아침", "점심", "저녁"만 추출
            }

            // 추출된 식사 시간이 있으면 세트로 추가
            if (!mealsInLine.isEmpty()) {
                mealTimes.add(String.join(",", mealsInLine));  // 공백으로 구분하여 출력
            }
        }

        return mealTimes;
    }

}
