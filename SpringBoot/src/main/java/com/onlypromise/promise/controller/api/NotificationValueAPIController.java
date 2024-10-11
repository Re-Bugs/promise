package com.onlypromise.promise.controller.api;


import com.onlypromise.promise.domain.User;
import com.onlypromise.promise.domain.enumeration.NotificationValue;
import com.onlypromise.promise.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class NotificationValueAPIController {

    private final UserService userService;

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
    @PatchMapping("/notification_value")
    public ResponseEntity<Map<String, String>> setNotificationValue(@RequestParam String bottleId, @RequestParam String value) {
        Map<String, String> response = new HashMap<>();
        Optional<User> fintUser = userService.findUserByBottleId(bottleId);

        if (fintUser.isEmpty())
        {
            response.put("message", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        User user = fintUser.get();

        try
        {
            // Enum으로 변환하여 처리
            NotificationValue notificationValue = NotificationValue.valueOf(value.toLowerCase());
            User updateUser = user.toBuilder().notificationValue(notificationValue).build();
            userService.save(updateUser);  // 변경된 User를 데이터베이스에 저장
            response.put("message", "success");
            log.info("알림값 변경 요청 : user_absolute_id : {}", updateUser.getId());
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }
        catch (IllegalArgumentException e)
        {
            // 잘못된 value 값 처리
            log.warn("잘못된 알림 값 요청 : {}, 값 : {}", user.getId(), value);
            response.put("message", "Invalid Value");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
