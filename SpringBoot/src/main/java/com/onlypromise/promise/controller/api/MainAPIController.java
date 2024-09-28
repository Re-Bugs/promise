package com.onlypromise.promise.controller.api;

import com.onlypromise.promise.DTO.api.NotificationDTO;
import com.onlypromise.promise.domain.enumeration.NotificationValue;
import com.onlypromise.promise.service.MedicationLogService;
import com.onlypromise.promise.domain.Notification;
import com.onlypromise.promise.domain.User;
import com.onlypromise.promise.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MainAPIController {
    private final MedicationLogService medicationLogService;
    private final UserService userService;

    @PostMapping("/dosage/{bottleId}")
    public ResponseEntity<Map<String, String>> updateMedicationStatusByBottleId(@PathVariable String bottleId)
    {
        String responseMessage = medicationLogService.updateMedicationStatusByBottleId(bottleId);

        // JSON 응답을 위한 Map 생성
        Map<String, String> response = new HashMap<>();
        response.put("message", responseMessage);

        if (responseMessage.equals("Duplication Dose")) return ResponseEntity.status(HttpStatus.CONFLICT).body(response); // 409 Conflict
        else if (responseMessage.equals("bottleId not found.")) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response); // 404 Not Found
        else return ResponseEntity.status(HttpStatus.OK).body(response); // 200 OK
    }

    @GetMapping("/lookup/{bottleId}")
    public ResponseEntity<Map<String, Object>> getMedicineData(@PathVariable String bottleId)
    {
        Map<String, Object> response = new HashMap<>();

        // bottleId로 유저 정보 가져오기
        User user = userService.findUserByBottleId(bottleId).orElse(null);

        if (user == null)
        {
            response.put("message", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        // 사용자의 Notification 정보 가져오기
        List<Notification> notifications = userService.findNotificationsByUser(user);
        if (notifications.isEmpty())
        {
            response.put("message", "No notifications found for this user");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        // Notification을 NotificationDTO로 변환
        List<NotificationDTO> notificationDTOs = notifications.stream()
                .map(notification -> new NotificationDTO(
                        notification.getId(),
                        notification.getRemainingDose(),
                        notification.getRenewalDate(),
                        notification.getDailyDose().toString(),
                        notification.getMorning(),
                        notification.getAfternoon(),
                        notification.getEvening()
                ))
                .collect(Collectors.toList());

        // 성공 시 데이터 반환
        response.put("notifications", notificationDTOs);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 알림값 조회 메서드
    @GetMapping("/notification_value")
    public ResponseEntity<Map<String, Object>> getNotificationValue(@RequestParam String bottleId)
    {
        Map<String, Object> response = new HashMap<>();
        Optional<User> user = userService.findUserByBottleId(bottleId);

        if (user.isEmpty())
        {
            response.put("message", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        NotificationValue value = user.get().getNotificationValue();
        response.put("NotificationValue", value);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 알림값 변경 메서드
    @PostMapping("/notification_value")
    public ResponseEntity<Map<String, String>> setNotificationValue(@RequestParam String bottleId, @RequestParam String value) {
        Map<String, String> response = new HashMap<>();
        Optional<User> user = userService.findUserByBottleId(bottleId);

        if (user.isEmpty())
        {
            response.put("message", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        try
        {
            // Enum으로 변환하여 처리
            NotificationValue notificationValue = NotificationValue.valueOf(value.toLowerCase());
            User updateUser = user.get().toBuilder().notificationValue(notificationValue).build();
            userService.save(updateUser);  // 변경된 User를 데이터베이스에 저장
            response.put("message", "success");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }
        catch (IllegalArgumentException e)
        {
            // 잘못된 value 값 처리
            response.put("message", "Invalid Value");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

}