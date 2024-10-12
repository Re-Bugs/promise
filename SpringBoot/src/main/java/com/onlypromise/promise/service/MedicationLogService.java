package com.onlypromise.promise.service;

import com.onlypromise.promise.domain.MedicationLog;
import com.onlypromise.promise.domain.Notification;
import com.onlypromise.promise.domain.User;
import com.onlypromise.promise.repository.MedicationLogRepository;
import com.onlypromise.promise.repository.NotificationRepository;
import com.onlypromise.promise.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicationLogService {

    private final NotificationRepository notificationRepository;
    private final MedicationLogRepository medicationLogRepository;
    private final UserRepository userRepository;

    // bottleId로 유저 찾고 복용 상태 업데이트(API)
    public int updateMedicationStatus(String bottleId)
    {
        Optional<User> userOptional = userRepository.findByBottleId(bottleId);

        if (userOptional.isEmpty())
        {
            log.warn("User not found with bottleId: {}", bottleId);
            return 1;
        }

        User user = userOptional.get();
        LocalDateTime currentTime = LocalDateTime.now();
        String currentTimePeriod = determineTimePeriod(currentTime);

        // 현재 사용자의 알림만 조회
        List<Notification> notifications = notificationRepository.findByUser(user);

        // 현재 시간대와 알림이 일치하는 약물이 있는지 검사
        boolean hasMatchingNotification = false;

        for (Notification notification : notifications)
        {
            if (isMatchingTimePeriod(notification, currentTimePeriod))
            {
                hasMatchingNotification = true;

                if (samePeriod(user, notification, currentTimePeriod, currentTime))
                {
                    log.warn("중복 복용 요청 : {},  유저 PK : {}, 유저 이름 : {}", currentTimePeriod, user.getId(), user.getName());
                    return 2; // 중복 복용
                }

                // 남은 약물 수 감소 및 저장
                if (notification.getRemainingDose() == 0)
                {
                    log.warn("약물 소진됨 - ,  유저 PK : {}, 유저 이름 : {}", user.getId(), user.getName());
                    return 3; // 약물이 모두 소진됨
                }

                updateNotificationRemainingDose(notification); // 약물 상태 업데이트
                addMedicationLog(user, notification, currentTime); // 투약 기록 추가 (정상적으로 복용한 경우)
            }
        }

        // 일치하는 시간대에 약물이 없을 때 오류 메시지 반환
        if (!hasMatchingNotification)
        {
            log.warn("예정되지 않은 복용 요청 : {},  유저 PK : {}, 유저 이름 : {}", currentTimePeriod, user.getId(), user.getName());
            return 4;
        }

        log.info("복용완료 : {},  유저 PK : {}, 유저 이름 : {}", currentTimePeriod, user.getId(), user.getName());
        return 0; //성공시 0 리턴
    }

    // 시간대 판단 로직
    private String determineTimePeriod(LocalDateTime currentTime)
    {
        int hour = currentTime.getHour();
        int minute = currentTime.getMinute();

        if ((hour == 4 && minute >= 1) || (hour > 4 && hour < 10) || (hour == 10 && minute == 0)) return "morning";
        else if ((hour == 10 && minute >= 1) || (hour > 10 && hour < 15) || (hour == 15 && minute == 0)) return "afternoon";
        else return "evening";
    }


    // 현재 시간대와 알림의 시간대가 일치하는지 확인
    private boolean isMatchingTimePeriod(Notification notification, String currentTimePeriod)
    {
        return (currentTimePeriod.equals("morning") && Boolean.TRUE.equals(notification.getMorning())) ||
                (currentTimePeriod.equals("afternoon") && Boolean.TRUE.equals(notification.getAfternoon())) ||
                (currentTimePeriod.equals("evening") && Boolean.TRUE.equals(notification.getEvening()));
    }

    // 같은 시간대에 이미 복용 기록이 있는지 확인하는 메서드
    private boolean samePeriod(User user, Notification notification, String timePeriod, LocalDateTime currentTime)
    {
        LocalDate today = currentTime.toLocalDate();
        List<MedicationLog> logs = medicationLogRepository.findByUserAndNotificationAndTimeBetween(
                user,
                notification,
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );

        // 같은 시간대에 복용 기록이 있는지 확인
        return logs.stream().anyMatch(log -> determineTimePeriod(log.getTime()).equals(timePeriod));
    }

    // 남은 약물 수 감소 및 저장
    private void updateNotificationRemainingDose(Notification notification)
    {
        if (notification.getRemainingDose() > 0)
        {
            Notification updatedNotification = notification.toBuilder()
                    .remainingDose((short) (notification.getRemainingDose() - 1))
                    .build();
            notificationRepository.save(updatedNotification);
        }
    }

    // 투약 기록 추가
    private void addMedicationLog(User user, Notification notification, LocalDateTime currentTime)
    {
        MedicationLog medicationLog = MedicationLog.builder()
                .notification(notification)
                .user(user)
                .time(currentTime)
                .status(true)
                .build();
        medicationLogRepository.save(medicationLog);
    }

    public List<MedicationLog> findByUserAndTimeBetween(User user, LocalDateTime startTime, LocalDateTime endTime)
    {
        return medicationLogRepository.findByUserAndTimeBetween(user, startTime, endTime);
    }
}