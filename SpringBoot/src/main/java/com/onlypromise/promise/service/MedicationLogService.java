package com.onlypromise.promise.service.api;

import com.onlypromise.promise.repository.api.MedicationLogRepository;
import com.onlypromise.promise.repository.api.NotificationAPIRepository;
import com.onlypromise.promise.domain.MedicationLog;
import com.onlypromise.promise.domain.Notification;
import com.onlypromise.promise.domain.User;
import com.onlypromise.promise.repository.web.UserRepository;
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
    private final NotificationAPIRepository notificationAPIRepository;
    private final MedicationLogRepository medicationLogRepository;
    private final UserRepository userRepository;

    // bottleId로 유저 찾고 복용 상태 업데이트
    public String updateMedicationStatusByBottleId(String bottleId)
    {
        Optional<User> userOptional = userRepository.findByBottleId(bottleId);
        if (userOptional.isPresent())
        {
            User user = userOptional.get();
            LocalDateTime currentTime = LocalDateTime.now();
            String timePeriod = determineTimePeriod(currentTime);

            // 현재 사용자의 알림만 조회
            List<Notification> notifications = notificationAPIRepository.findByUser(user);

            for (Notification notification : notifications)
            {
                if ((timePeriod.equals("morning") && Boolean.TRUE.equals(notification.getMorning()))
                        || (timePeriod.equals("afternoon") && Boolean.TRUE.equals(notification.getAfternoon()))
                        || (timePeriod.equals("evening") && Boolean.TRUE.equals(notification.getEvening())))
                {

                    // 같은 시간대에 복용 기록이 있는지 확인
                    if (hasMedicationLogForSamePeriod(user, notification, timePeriod, currentTime)) return "Duplication Dose";  // 중복 복용 거절 메시지

                    // 남은 약물 수 감소
                    notification.setRemainingDose((short) (notification.getRemainingDose() - 1));
                    notificationAPIRepository.save(notification);

                    // 투약 기록 추가 (정상적으로 복용한 경우)
                    MedicationLog medicationLog = MedicationLog.builder()
                            .notification(notification)
                            .user(user)
                            .time(currentTime)
                            .status(true)
                            .build();
                    medicationLogRepository.save(medicationLog);
                }
            }

            return "success";
        }
        else
        {
            log.warn("User not found with bottleId: {}", bottleId);
            return "bottleId not found.";
        }
    }

    // 시간대 판단 로직
    private String determineTimePeriod(LocalDateTime currentTime) {
        if (currentTime.getHour() >= 4 && currentTime.getHour() <= 10) return "morning";
        else if (currentTime.getHour() > 10 && currentTime.getHour() <= 15) return "afternoon";
        else return "evening";
    }

    // 같은 시간대에 이미 복용 기록이 있는지 확인하는 메서드
    private boolean hasMedicationLogForSamePeriod(User user, Notification notification, String timePeriod, LocalDateTime currentTime)
    {
        LocalDate today = currentTime.toLocalDate();
        List<MedicationLog> logs = medicationLogRepository.findByUserAndNotificationAndTimeBetween(
                user,
                notification,
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );

        // 같은 시간대에 복용 기록이 있는지 확인
        for (MedicationLog log : logs)
        {
            if (determineTimePeriod(log.getTime()).equals(timePeriod)) return true;
        }
        return false;
    }
}