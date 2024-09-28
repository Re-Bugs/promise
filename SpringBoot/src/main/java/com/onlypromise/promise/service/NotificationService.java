package com.onlypromise.promise.service;

import com.onlypromise.promise.domain.Notification;
import com.onlypromise.promise.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void save(Notification notification) {
        notificationRepository.save(notification);
    }

    // 알림을 ID로 찾는 메서드 (추가 기능 필요 시)
    public Notification findById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 알림이 존재하지 않습니다: " + id));
    }
}
