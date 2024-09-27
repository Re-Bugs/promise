package com.promise.promise.service.web;

import com.promise.promise.domain.User;
import com.promise.promise.repository.web.UserRepository;
import com.promise.promise.DTO.web.LoginDTO;
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
}