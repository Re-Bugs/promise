package com.promise.promise.api.service;

import com.promise.promise.api.DTO.SignUpDTO;
import com.promise.promise.api.repository.MedicationLogRepository;
import com.promise.promise.api.repository.NotificationAPIRepository;
import com.promise.promise.api.repository.UserAPIRepository;
import com.promise.promise.domain.MedicationLog;
import com.promise.promise.domain.Notification;
import com.promise.promise.domain.User;
import com.promise.promise.domain.enumeration.NotificationValue;
import com.promise.promise.domain.enumeration.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAPIService {
    private final UserAPIRepository userApiRepository;
    private final MedicationLogRepository medicationLogRepository;

    // 특정 유저 PK로 유저 찾기
    public Optional<User> findUserById(Long id) {
        return userApiRepository.findById(id);
    }

    // UserAPIService.java
    public List<Notification> findNotificationsByUser(User user) {
        return user.getNotifications();  // User 객체로부터 알림 리스트를 바로 가져옴
    }

    // bottle_id로 유저 찾기
    public Optional<User> findUserByBottleId(String bottleId) {
        return userApiRepository.findByBottleId(bottleId);
    }

    // User 기반으로 복용 기록 조회
    public List<MedicationLog> findMedicationLogsByUser(User user) {
        return medicationLogRepository.findByUser(user);  // 사용자 기반으로 복용 기록 조회
    }

    // 회원 가입 로직 (기존 로직 유지)
    public Map<String, String> signUp(SignUpDTO signUpDTO) {
        Map<String, String> response = new HashMap<>();
        try {
            // 기본값 설정
            User user = User.builder()
                    .role(Role.user)  // 기본값: user
                    .name(signUpDTO.getName())
                    .notificationValue(NotificationValue.bottle) // 기본값: bottle
                    .bottleId(signUpDTO.getBottleId())
                    .build();

            userApiRepository.save(user); // DB에 저장

            // 성공 메시지 JSON으로 반환
            response.put("message", "success");
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 데이터 무결성 관련 예외 처리 (중복된 키 등)
            log.warn("Data integrity violation: {}", e.getMessage());
            response.put("message", "Duplicate bottle code");
        } catch (Exception e) {
            // 예외가 발생하면 실패 메시지 반환
            log.error("An error occurred: {}", e.getMessage());
            response.put("message", "fail");
        }
        return response;
    }

    public boolean testSignUp(SignUpDTO signUpDTO) {
        try {
            // User 객체 생성
            User user = User.builder()
                    .role(Role.user)  // 기본값: user
                    .name(signUpDTO.getName())
                    .notificationValue(NotificationValue.bottle) // 기본값: bottle
                    .bottleId(signUpDTO.getBottleId())
                    .build();

            userApiRepository.save(user); // DB에 저장

            return true;  // 회원가입 성공
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("중복된 bottle ID: {}", signUpDTO.getBottleId());
            return false;  // 중복된 bottle ID 처리
        } catch (Exception e) {
            log.error("회원가입 중 오류 발생: {}", e.getMessage());
            return false;  // 기타 오류 처리
        }
    }


}