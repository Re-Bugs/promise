package com.onlypromise.promise.repository.web;

import com.onlypromise.promise.domain.Notification;
import com.onlypromise.promise.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // User 기반으로 Notification 목록 조회
    List<Notification> findByUser(User user);

}
