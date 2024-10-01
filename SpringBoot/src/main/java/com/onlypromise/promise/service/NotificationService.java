package com.onlypromise.promise.service;

import com.onlypromise.promise.DTO.DailyTakenDTO;
import com.onlypromise.promise.domain.MedicationLog;
import com.onlypromise.promise.domain.Medicine;
import com.onlypromise.promise.domain.Notification;
import com.onlypromise.promise.domain.User;
import com.onlypromise.promise.domain.enumeration.DailyDose;
import com.onlypromise.promise.repository.MedicationLogRepository;
import com.onlypromise.promise.repository.MedicineRepository;
import com.onlypromise.promise.repository.NotificationRepository;
import com.onlypromise.promise.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final MedicineRepository medicineRepository;

    @Transactional
    public void save(Notification notification) {
        notificationRepository.save(notification);
    }

    // 사용자와 약물로 알림 조회
    public Optional<Notification> findByUserAndMedicine(User user, Medicine medicine) {
        return notificationRepository.findByUserAndMedicine(user, medicine);
    }

    // 복용 및 미복용 조회
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

    @Transactional
    public void createNotification(String bottleId, Long medicineId, short total, boolean morning, boolean afternoon, boolean evening) {
        // bottleId로 User 찾기
        Optional<User> userOptional = userRepository.findByBottleId(bottleId);
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("User not found with bottleId: " + bottleId);
        }
        User user = userOptional.get();

        // 약물 ID로 Medicine 찾기
        Optional<Medicine> medicineOptional = medicineRepository.findById(medicineId);
        if (medicineOptional.isEmpty()) {
            throw new IllegalArgumentException("Medicine not found with ID: " + medicineId);
        }
        Medicine medicine = medicineOptional.get();

        // DailyDose 계산 (아침, 점심, 저녁 중 체크된 개수에 따라 결정)
        int doseCount = (morning ? 1 : 0) + (afternoon ? 1 : 0) + (evening ? 1 : 0);
        DailyDose dailyDose = switch (doseCount) {
            case 1 -> DailyDose.one;
            case 2 -> DailyDose.two;
            case 3 -> DailyDose.three;
            default -> throw new IllegalArgumentException("Invalid dose count");
        };

        // Notification 생성 및 저장
        LocalDate currentDate = LocalDate.now();
        Notification notification = Notification.builder()
                .user(user)
                .medicine(medicine)
                .total(total)
                .remainingDose(total)
                .createdAt(currentDate)
                .renewalDate(currentDate.plusDays(total)) // 투약일수를 더해 renewalDate 설정
                .dailyDose(dailyDose)
                .morning(morning)
                .afternoon(afternoon)
                .evening(evening)
                .build();

        notificationRepository.save(notification);
    }

    public List<Notification> findNotificationByUser(User user)
    {
        return notificationRepository.findByUser(user);
    }

}
