package com.onlypromise.promise.service;

import com.onlypromise.promise.domain.MedicationLog;
import com.onlypromise.promise.domain.Notification;
import com.onlypromise.promise.domain.User;
import com.onlypromise.promise.repository.MedicationLogRepository;
import com.onlypromise.promise.repository.UserRepository;
import com.onlypromise.promise.DTO.web.LoginDTO;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final MedicationLogRepository medicationLogRepository;

    // 회원가입(웹) 메서드
    public boolean signUp(User user) {
        if(userRepository.existsByUserId(user.getUserId())) return false; // DB에 user ID가 이미 존재한다면 false
        else userRepository.save(user); // DB에 user ID가 존재하지 않는다면 DB에 저장
        return true;
    }

    // 로그인 메서드(web)
    public Boolean login(LoginDTO loginDTO)
    {
        Optional<User> user = userRepository.findByUserId(loginDTO.getUserId()); // 리포지토리에서 user를 찾음
        return user.isPresent() && user.get().getUserPassword().equals(loginDTO.getPassword()); // 찾은 user와 dto를 비교하여 로그인 성공여부 리턴
    }

    public Boolean APILogin(com.onlypromise.promise.DTO.api.LoginDTO loginDTO)
    {
        Optional<User> findUser = userRepository.findByBottleId(loginDTO.getBottleId());
        return findUser.isPresent();
    }

    // String 타입 userId로 user 찾기
    public Optional<User> findByUserId(String userId)
    {
        return userRepository.findByUserId(userId);
    }

    //닉네임이 중복되지 않는지 검증하는 메서드
    public boolean isNickNameExists(@NotBlank(message = "닉네임은 필수 항목입니다.") String nickName)
    {
        return userRepository.existsByNickName(nickName);
    }

    // PK로 유저 찾기
    public Optional<User> findUserById(Long id)
    {
        return userRepository.findById(id);
    }

    // bottle_id로 유저 찾기
    public Optional<User> findUserByBottleId(String bottleId)
    {
        return userRepository.findByBottleId(bottleId);
    }

    // User 기반으로 복용 기록 조회
    public List<MedicationLog> findMedicationLogsByUser(User user)
    {
        return medicationLogRepository.findByUser(user);  // 사용자 기반으로 복용 기록 조회
    }

    // user 기반으로 알림 정보 조회
    public List<Notification> findNotificationsByUser(User user)
    {
        return user.getNotifications();  // User 객체로부터 알림 리스트를 바로 가져옴
    }

    public void save(User user)
    {
        userRepository.save(user);
    }
}