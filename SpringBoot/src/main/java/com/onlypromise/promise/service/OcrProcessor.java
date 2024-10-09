package com.onlypromise.promise.service;

import com.onlypromise.promise.DTO.MedicationDTO;
import com.onlypromise.promise.DTO.api.MedicineDTO;
import com.onlypromise.promise.domain.Medicine;
import com.onlypromise.promise.domain.Notification;
import com.onlypromise.promise.domain.User;
import com.onlypromise.promise.domain.enumeration.DailyDose;
import com.onlypromise.promise.repository.MedicineRepository;
import com.onlypromise.promise.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcrProcessor {

    private final NotificationRepository notificationRepository;
    private final MedicineRepository medicineRepository;

    private static final Pattern NINE_DIGIT_PATTERN = Pattern.compile("\\b\\d{9}\\b");
    private static final Pattern BETWEEN_PATTERN = Pattern.compile("명칭(.*?)주사제", Pattern.DOTALL);
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\b(\\d0)\\b(?!\\s*[a-zA-Z㎎])");
    private static final Pattern MEAL_SET_PATTERN = Pattern.compile("(하루\\s*\\d+[회번]\\).*?(아침|점심|저녁)(/.*?(아침|점심|저녁))?\\s*식(사후)?)");
    private static final Pattern DAY_NUMBER_PATTERN = Pattern.compile("하루\\s*(1|2|3)\\s*[회번]");
    private static final Pattern MEAL_PATTERN = Pattern.compile("(아침|점심|저녁)");

    public List<MedicationDTO> buildMedicationDtoList(String extractedText)
    {
        List<String> nineDigitNumbers = extractNineDigitNumbers(extractedText);
        List<String> dayAndMealSet = extractDayAndMealSet(extractedText);
        List<String> twoDigitNumbers = extractValidTwoDigitNumbers(extractedText);
        List<String> dailyDosageTimes = extractDayNumber(extractedText);
        List<String> mealTimes = extractMealTimesFromList(dayAndMealSet);

        List<MedicationDTO> dtoList = new ArrayList<>();
        int size = nineDigitNumbers.size();

        for (int i = 0; i < size; ++i)
        {
            MedicationDTO dto = new MedicationDTO();
            dto.setMedicationCode(nineDigitNumbers.get(i));
            dto.setTotalDosageDays(getValueOrDefault(twoDigitNumbers, i, "0"));
            dto.setDailyDosageTimes(getValueOrDefault(dailyDosageTimes, i, "0"));
            dto.setMealTimes(List.of(getValueOrDefault(mealTimes, i, "없음")));

            dtoList.add(dto);
        }

        return dtoList;
    }

    public List<MedicineDTO> filterAndBuildValidDtoList(List<MedicationDTO> dtoList, User user)
    {
        List<MedicineDTO> validDtoList = new ArrayList<>();
        for (MedicationDTO dto : dtoList)
        {
            if (isValidDto(dto))
            {
                Optional<Medicine> medicine = medicineRepository.findByProductCode(dto.getMedicationCode());
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

    public List<String> saveNotifications(User user, List<MedicineDTO> validDtoList)
    {
        List<String> warningMessages = new ArrayList<>();
        for (MedicineDTO medicineDTO : validDtoList)
        {
            Optional<Medicine> findMedicine = medicineRepository.findById(medicineDTO.getMedicineId());

            if (findMedicine.isEmpty()) //등록되지 않은 약품코드
            {
                log.info("등록되지 않은 약품코드 = {}", medicineDTO.getMedicineId());
                warningMessages.add(medicineDTO.getMedicineId() + " 이러한 약품코드를 찾을 수 없습니다.");
                continue;
            }

            Medicine medicine = findMedicine.get();
            Optional<Notification> findNotification = notificationRepository.findByUserAndMedicine(user, medicine);

            if (findNotification.isPresent()) //이미 등록되어있는 약품코드
            {
                Notification notification = findNotification.get();
                short remainingDose = notification.getRemainingDose();
                int totalDosageDays = Integer.parseInt(medicineDTO.getTotalDosageDays());

                Notification updateNotification = notification.toBuilder()
                        .renewalDate(LocalDate.now().plusDays(totalDosageDays))
                        .remainingDose((short) (remainingDose + totalDosageDays))
                        .build();

                notificationRepository.save(updateNotification);
                log.info("user Absolute ID : {} - 처방전 재인식 = {}", user.getId(), notification.getMedicine().getName());
                warningMessages.add(notification.getMedicine().getName() + "은 이미 알림에 등록되어서 재처방일과 남은 약물 수를 업데이트 하였습니다.");
                continue;
            }

            DailyDose dailyDose = convertToDailyDose(medicineDTO.getDailyDosageTimes()); //일일 복용횟수
            List<String> mealTimes = medicineDTO.getMealTimes(); //하루 언제 복용해야하는지

            Notification notificationUpdate1 = Notification.builder() //하루 언제 복용해야하는지를 제외하고 알림 객체 생성
                    .user(user)
                    .medicine(medicine)
                    .dailyDose(dailyDose)
                    .createdAt(LocalDate.now())
                    .renewalDate(LocalDate.now().plusDays(Integer.parseInt(medicineDTO.getTotalDosageDays())))
                    .remainingDose((short) Integer.parseInt(medicineDTO.getTotalDosageDays()))
                    .total((short) Integer.parseInt(medicineDTO.getTotalDosageDays()))
                    .build();

            Notification notificationUpdate2 = setNotificationTimes(notificationUpdate1, mealTimes); //알림 객체에 하루 언제 복용해야하는지 매핑

            notificationRepository.save(notificationUpdate2); //저장
        }
        return warningMessages;
    }

    private Notification setNotificationTimes(Notification notification, List<String> mealTimes)
    {
        Notification.NotificationBuilder notificationBuilder = notification.toBuilder().morning(false).afternoon(false).evening(false);

        for (String mealTime : mealTimes)
        {
            String[] meals = mealTime.split(",");
            for (String meal : meals)
            {
                meal = meal.trim();
                log.info(meal);
                if (meal.equals("아침")) notificationBuilder.morning(true);
                else if (meal.equals("점심")) notificationBuilder.afternoon(true);
                else if (meal.equals("저녁")) notificationBuilder.evening(true);
            }
        }
        return notificationBuilder.build();
    }

    private DailyDose convertToDailyDose(String dailyDosageTimes)
    {
        switch (dailyDosageTimes)
        {
            case "1":
                return DailyDose.one;
            case "2":
                return DailyDose.two;
            case "3":
                return DailyDose.three;
            default:
                log.warn("잘못된 일일 복용 횟수: {}", dailyDosageTimes);
                throw new IllegalArgumentException("Invalid daily dosage times: " + dailyDosageTimes);
        }
    }

    private boolean isValidDto(MedicationDTO dto)
    {
        return dto.getMedicationCode() != null &&
                dto.getTotalDosageDays() != null &&
                dto.getDailyDosageTimes() != null &&
                dto.getMealTimes() != null && !dto.getMealTimes().isEmpty();
    }

    private void logDto(MedicineDTO dto, User user)
    {
        log.info("---------------add Medicine---------------");
        log.info("user Absolute ID: {}", user.getId());
        log.info("Medicine ID: {}", dto.getMedicineId());
        log.info("Total Dosage Days: {}", dto.getTotalDosageDays());
        log.info("Daily Dosage Times: {}", dto.getDailyDosageTimes());
        log.info("Meal Times: {}", dto.getMealTimes());
    }

    // 추출 메서드
    private List<String> extractNineDigitNumbers(String text)
    {
        List<String> nineDigitNumbers = new ArrayList<>();
        Matcher betweenMatcher = BETWEEN_PATTERN.matcher(text);
        if (betweenMatcher.find())
        {
            String betweenWords = betweenMatcher.group(1);
            Matcher matcher = NINE_DIGIT_PATTERN.matcher(betweenWords);
            while (matcher.find()) nineDigitNumbers.add(matcher.group());
        }
        return nineDigitNumbers;
    }

    private List<String> extractValidTwoDigitNumbers(String text)
    {
        List<String> validNumbers = new ArrayList<>();
        Matcher betweenMatcher = BETWEEN_PATTERN.matcher(text);
        if (betweenMatcher.find())
        {
            String betweenWords = betweenMatcher.group(1);
            Matcher digitMatcher = DIGIT_PATTERN.matcher(betweenWords);
            while (digitMatcher.find()) validNumbers.add(digitMatcher.group(1));
        }
        return validNumbers;
    }

    private List<String> extractDayAndMealSet(String text)
    {
        List<String> dayAndMealSet = new ArrayList<>();
        Matcher mealSetMatcher = MEAL_SET_PATTERN.matcher(text);
        while (mealSetMatcher.find()) dayAndMealSet.add(mealSetMatcher.group());
        return dayAndMealSet;
    }

    private List<String> extractDayNumber(String text)
    {
        List<String> dayNumbers = new ArrayList<>();
        Matcher dayNumberMatcher = DAY_NUMBER_PATTERN.matcher(text);
        while (dayNumberMatcher.find()) dayNumbers.add(dayNumberMatcher.group(1));
        return dayNumbers;
    }

    private List<String> extractMealTimesFromList(List<String> lines)
    {
        List<String> mealTimes = new ArrayList<>();
        for (String line : lines)
        {
            Matcher mealMatcher = MEAL_PATTERN.matcher(line);
            List<String> mealsInLine = new ArrayList<>();
            while (mealMatcher.find()) mealsInLine.add(mealMatcher.group(1));
            if (!mealsInLine.isEmpty()) mealTimes.add(String.join(",", mealsInLine));
        }
        return mealTimes;
    }

    private String getValueOrDefault(List<String> list, int index, String defaultValue)
    {
        return (index < list.size()) ? list.get(index) : defaultValue;
    }
}