package com.promise.promise.service;

import com.promise.promise.api.DTO.SignUpDTO;
import com.promise.promise.domain.User;
import com.promise.promise.domain.enumeration.NotificationValue;
import com.promise.promise.domain.enumeration.Role;
import com.promise.promise.repository.UserRepository;
import com.promise.promise.web.DTO.LoginDTO;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean signUp(User user) {
        if(userRepository.existsByUserId(user.getUserId())) return false; // DB에 user ID가 이미 존재한다면 false
        else userRepository.save(user); // DB에 user ID가 존재하지 않는다면 DB에 저장
        return true;
    }

    public Boolean login(LoginDTO loginDTO)
    {
        Optional<User> user = userRepository.findByUserId(loginDTO.getUserId());
        return user.isPresent() && user.get().getUserPassword().equals(loginDTO.getPassword());
    }

    public Optional<User> findByUserId(String userId)
    {
        return userRepository.findByUserId(userId);
    }

    public boolean isNickNameExists(@NotBlank(message = "닉네임은 필수 항목입니다.") String nickName)
    {
        return userRepository.existsByNickName(nickName);
    }




    public String apiSignUp(SignUpDTO signUpDTO) {
        // 기본값 설정
        User user = User.builder()
                .role(Role.user)  // 기본값: user
                .name(signUpDTO.getName())
                .notificationValue(NotificationValue.bottle) // 기본값: bottle
                .bottleId(signUpDTO.getBottleId())
                .build();

        userRepository.save(user); // DB에 저장

        return "success";
    }
}