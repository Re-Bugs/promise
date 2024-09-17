package com.promise.promise.api.repository;

import com.promise.promise.domain.Notification;
import com.promise.promise.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationAPIRepository extends JpaRepository<Notification, Long> {

    // User 기반으로 Notification 목록 조회
    List<Notification> findByUser(User user);
}
