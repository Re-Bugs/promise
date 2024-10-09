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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDate;
import java.util.*;

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
        int responseMessage = medicationLogService.updateMedicationStatus(bottleId);

        // JSON 응답을 위한 Map 생성
        Map<String, String> response = new HashMap<>();

        switch(responseMessage)
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
            newDto.setMorning(notification.getMorning());
            newDto.setAfternoon(notification.getAfternoon());
            newDto.setEvening(notification.getEvening());

            notificationDTOs.add(newDto);
        }

        // 성공 시 데이터 반환
        response.put("notifications", notificationDTOs);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    @GetMapping("/daily_taken")
    public ResponseEntity<Map<String, Object>> getDailyTaken(@RequestParam String bottleId, @RequestParam LocalDate date) {

        Map<String, Object> response = new HashMap<>();
        Optional<User> findUser = userService.findUserByBottleId(bottleId);

        if (findUser.isEmpty())
        {
            response.put("message", "user not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        User user = findUser.get();

        // 날짜별로 유저의 복용 상태를 확인
        Map<String, List<DailyTakenDTO>> medicationStatus = notificationService.getMedicationsStatusByDate(user, date);

        // 복용한 약물과 복용하지 않은 약물을 결과에 담기
        response.put("taken", medicationStatus.get("taken"));
        response.put("notTaken", medicationStatus.get("notTaken"));

        // 성공적으로 복용 상태 반환
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    // 날짜 형식 오류 처리 핸들러
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatchException(MethodArgumentTypeMismatchException ex)
    {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Invalid date format. Please use YYYY-MM-DD.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}