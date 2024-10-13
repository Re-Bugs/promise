package com.onlypromise.promise.controller.api;

import com.onlypromise.promise.DTO.api.DailyTakenDTO;
import com.onlypromise.promise.DTO.api.NotificationDTO;
import com.onlypromise.promise.domain.Medicine;
import com.onlypromise.promise.service.MedicationLogService;
import com.onlypromise.promise.domain.Notification;
import com.onlypromise.promise.domain.User;
import com.onlypromise.promise.service.MedicineService;
import com.onlypromise.promise.service.NotificationService;
import com.onlypromise.promise.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MainAPIController {
    private final MedicationLogService medicationLogService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final MedicineService medicineService;


    @PostMapping("/dosage/{bottleId}")
    public ResponseEntity<Map<String, String>> updateMedicationStatusByBottleId(@PathVariable String bottleId)
    {
        int responseCode = medicationLogService.updateMedicationStatus(bottleId);

        // JSON 응답을 위한 Map 생성
        Map<String, String> response = new HashMap<>();

        switch(responseCode)
        {
            case 0:
                response.put("message", "success");
                return ResponseEntity.ok(response);
            case 1:
                response.put("message", "Bottle ID not found.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            case 2:
                response.put("message", "You already have a history of taking it at that time.");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            case 3:
                response.put("message", "Medication fully consumed.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            case 4:
                response.put("message", "No medication scheduled for this time period.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            default:
                log.error("약물 복용 요청 에러 발생");
                response.put("message", "Server error.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/lookup/{bottleId}")
    public ResponseEntity<Map<String, Object>> getNotifications(@PathVariable String bottleId)
    {
        Map<String, Object> response = new HashMap<>();

        // bottleId로 유저 정보 가져오기
        Optional<User> findUser = userService.findUserByBottleId(bottleId);

        if (findUser.isEmpty())
        {
            response.put("message", "User not found");
            log.warn("알림 내역 조회 - 잘못된 약통코드 : {}", bottleId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        User user = findUser.get();

        // 사용자의 Notification 정보 가져오기
        List<Notification> notifications = userService.findNotificationsByUser(user);
        if (notifications.isEmpty())
        {
            response.put("message", "No notifications found for this user");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        // Notification을 NotificationDTO로 변환
        List<NotificationDTO> notificationDTOs = new ArrayList<>();

        for(Notification notification : notifications)
        {
            Optional<Medicine> findMedicine = medicineService.findMedicineById(notification.getMedicine().getId());
            NotificationDTO newDto = new NotificationDTO();
            newDto.setId(notification.getId());
            newDto.setName(findMedicine.get().getName());
            newDto.setRemainingDose(notification.getRemainingDose());
            newDto.setDailyDose(notification.getDailyDose().toString());
            newDto.setRenewalDate(notification.getRenewalDate());
            newDto.setMorning(notification.getMorning() != null ? notification.getMorning() : false);
            newDto.setAfternoon(notification.getAfternoon() != null ? notification.getAfternoon() : false);
            newDto.setEvening(notification.getEvening() != null ? notification.getEvening() : false);

            notificationDTOs.add(newDto);
        }

        // 성공 시 데이터 반환
        response.put("notifications", notificationDTOs);
        log.info("알림내역 확인 - user PK : {}, 이름 : {}", user.getId(), user.getName());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    @GetMapping("/daily_taken")
    public ResponseEntity<Map<String, Object>> getDailyTaken(@RequestParam String bottleId, @RequestParam LocalDate date) {

        Map<String, Object> response = new HashMap<>();
        Optional<User> findUser = userService.findUserByBottleId(bottleId);

        if (findUser.isEmpty())
        {
            response.put("message", "user not found.");
            log.warn("일일 복용 내역 조회 - 잘못된 약통코드 : {}", bottleId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        User user = findUser.get();

        // 날짜별로 유저의 복용 상태를 확인
        Map<String, List<DailyTakenDTO>> medicationStatus = notificationService.getMedicationsStatusByDate(user, date);

        // 복용한 약물과 복용하지 않은 약물을 결과에 담기
        response.put("taken", medicationStatus.get("taken"));
        response.put("notTaken", medicationStatus.get("notTaken"));
        log.info("일일 복용 내용 확인 - user PK : {}, 이름 : {}, 날짜 : {}", user.getId(), user.getName(), date);

        // 성공적으로 복용 상태 반환
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    // 날짜 형식 오류 처리 핸들러
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatchException(MethodArgumentTypeMismatchException ex)
    {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Invalid date format. Please use YYYY-MM-DD.");
        log.warn("잘못된 날짜 요청 오류 발생");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}