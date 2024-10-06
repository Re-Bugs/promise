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
    public String updateMedicationStatus(String bottleId)
    {
        Optional<User> userOptional = userRepository.findByBottleId(bottleId);

        if (userOptional.isEmpty())
        {
            log.warn("User not found with bottleId: {}", bottleId);
            return "bottleId not found.";
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

                if (samePeriod(user, notification, currentTimePeriod, currentTime)) return "You already have a history of taking it at that time."; // 중복 복용 거절 메시지

                // 남은 약물 수 감소 및 저장
                updateNotificationRemainingDose(notification);

                // 투약 기록 추가 (정상적으로 복용한 경우)
                addMedicationLog(user, notification, currentTime);
            }
        }

        // 일치하는 시간대에 약물이 없을 때 오류 메시지 반환
        if (!hasMatchingNotification) {
            log.warn("No medication scheduled for {} time for bottleId: {}", currentTimePeriod, bottleId);
            return "No medication scheduled for this time period.";
        }

        return "success";
    }

    // 시간대 판단 로직
    private String determineTimePeriod(LocalDateTime currentTime)
    {
        int hour = currentTime.getHour();
        if (hour >= 4 && hour <= 10) return "morning";
        else if (hour > 10 && hour <= 15) return "afternoon";
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
        Notification updatedNotification = notification.toBuilder()
                .remainingDose((short) (notification.getRemainingDose() - 1))
                .build();
        notificationRepository.save(updatedNotification);
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

    List<MedicationLog> findByUserAndTimeBetween(User user, LocalDateTime startTime, LocalDateTime endTime)
    {
        return medicationLogRepository.findByUserAndTimeBetween(user, startTime, endTime);
    }
}