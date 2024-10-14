package com.onlypromise.promise.controller.api;

import com.onlypromise.promise.domain.User;
import com.onlypromise.promise.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AlarmAPIController {

    private final UserService userService;

    @GetMapping("/alarm")
    public ResponseEntity<Map<String, String>> getAlarmTime(@RequestParam String bottleId)
    {
        Map<String, String> response = new HashMap<>();

        Optional<User> findUser = userService.findUserByBottleId(bottleId);
        if(findUser.isEmpty())
        {
            response.put("message", "User not found");
            log.warn("알림 시각 조회(API) - 잘못된 약통코드 : {}", bottleId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        User user = findUser.get();

        response.put("morning", String.valueOf(user.getMorningTime()));
        response.put("afternoon", String.valueOf(user.getAfternoonTime()));
        response.put("evening", String.valueOf(user.getEveningTime()));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PatchMapping("/alarm")
    public ResponseEntity<Map<String, String>> setAlarmTime(
            @RequestParam String bottleId,
            @RequestParam String morningTime,
            @RequestParam String afternoonTime,
            @RequestParam String eveningTime
    )
    {
        Map<String, String> response = new HashMap<>();


        Optional<User> findUser = userService.findUserByBottleId(bottleId);

        if (findUser.isEmpty())
        {
            response.put("message", "User not found");
            log.warn("알림 시각 변경(API) - 잘못된 약통코드 : {}", bottleId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        User user = findUser.get();

        try
        {
            // LocalTime 변환 및 업데이트
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            User updateUser = user.toBuilder()
                    .morningTime(LocalTime.parse(morningTime, timeFormatter))
                    .afternoonTime(LocalTime.parse(afternoonTime, timeFormatter))
                    .eveningTime(LocalTime.parse(eveningTime, timeFormatter))
                    .build();

            userService.save(updateUser);

            log.info("알림 시각 업데이트(API) - user PK : {}, 이름 : {} 아침 알림 시각 : {}, 점심 알림 시각 : {}, 저녁 알림 시각 : {}", updateUser.getId(), updateUser.getName(), updateUser.getMorningTime(), updateUser.getAfternoonTime(), updateUser.getEveningTime());
            response.put("message", "success");
            return ResponseEntity.status(HttpStatus.OK).body(response);

        }
        catch (DateTimeParseException e)
        {
            log.warn("잘못된 알림 시각 요청(API) - user PK : {}, 이름 : {}", user.getId(), user.getName());
            // 시간 형식이 잘못된 경우 처리
            response.put("message", "Invalid time format. Please use HH:mm format.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        catch (Exception e)
        {
            // 기타 예외 처리
            log.error("알림 시각 변경 오류 발생(API) - user PK : {}, 이름 : {}", user.getId(), user.getName());
            log.error("알림 시각 변경 오류", e);
            response.put("message", "server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
