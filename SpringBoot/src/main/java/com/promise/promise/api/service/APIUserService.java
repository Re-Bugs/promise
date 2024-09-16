package com.promise.promise.api.service;

import com.promise.promise.api.DTO.SignUpDTO;
import com.promise.promise.api.repository.APIUserRepository;
import com.promise.promise.domain.User;
import com.promise.promise.domain.enumeration.NotificationValue;
import com.promise.promise.domain.enumeration.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class APIUserService {
    private final APIUserRepository apiUserRepository;

    // bottle_id로 유저 찾기
    public Optional<User> findUserByBottleId(String bottleId) {
        return apiUserRepository.findByBottleId(bottleId);
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

            apiUserRepository.save(user); // DB에 저장

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
}