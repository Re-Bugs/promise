package com.onlypromise.promise.repository;

import com.onlypromise.promise.domain.MedicationLog;
import com.onlypromise.promise.domain.Medicine;
import com.onlypromise.promise.domain.Notification;
import com.onlypromise.promise.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicationLogRepository extends JpaRepository<MedicationLog, Long> {

    // User 기반으로 MedicationLog 목록 조회
    List<MedicationLog> findByUser(User user);

    // 특정 유저와 알림, 그리고 시간대 범위에 해당하는 복용 기록을 조회
    List<MedicationLog> findByUserAndNotificationAndTimeBetween(User user, Notification notification, LocalDateTime startTime, LocalDateTime endTime);

    // 특정 유저와 시간 범위에 해당하는 복용 기록을 조회하는 메서드
    List<MedicationLog> findByUserAndTimeBetween(User user, LocalDateTime startTime, LocalDateTime endTime);


}
