package com.onlypromise.promise.service;

import com.onlypromise.promise.DTO.DailyTakenDTO;
import com.onlypromise.promise.domain.MedicationLog;
import com.onlypromise.promise.domain.Notification;
import com.onlypromise.promise.domain.User;
import com.onlypromise.promise.repository.MedicationLogRepository;
import com.onlypromise.promise.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MedicationLogRepository medicationLogRepository;

    @Transactional
    public void save(Notification notification) {
        notificationRepository.save(notification);
    }

    public Map<String, List<DailyTakenDTO>> getMedicationsStatusByDate(User user, LocalDate targetDate)
    {
        // 해당 유저의 모든 알림 조회
        List<Notification> notifications = notificationRepository.findByUser(user);

        // 해당 날짜의 복용 기록 조회
        List<MedicationLog> medicationLogs = medicationLogRepository.findByUserAndTimeBetween(
                user,
                targetDate.atStartOfDay(),
                targetDate.plusDays(1).atStartOfDay()
        );

        // 복용한 알림 ID 수집
        Set<Long> takenNotificationIds = medicationLogs.stream()
                .map(log -> log.getNotification().getId())
                .collect(Collectors.toSet());

        // 복용한 약물과 복용하지 않은 약물 분류
        List<DailyTakenDTO> takenMedications = new ArrayList<>();
        List<DailyTakenDTO> notTakenMedications = new ArrayList<>();

        for (Notification notification : notifications)
        {
            DailyTakenDTO dto = convertToDTO(notification);
            if (takenNotificationIds.contains(notification.getId())) takenMedications.add(dto);
            else notTakenMedications.add(dto);
        }

        // 결과 반환 (복용한 약물, 복용하지 않은 약물)
        Map<String, List<DailyTakenDTO>> result = new HashMap<>();
        result.put("taken", takenMedications);
        result.put("notTaken", notTakenMedications);

        return result;
    }

    // Notification 객체를 DailyTakenDTO로 변환하는 메서드
    private DailyTakenDTO convertToDTO(Notification notification)
    {
        return DailyTakenDTO.builder()
                .id(notification.getId())
                .medicineName(notification.getMedicine().getName()) // Medicine 객체의 이름을 가져옴
                .build();
    }

}
