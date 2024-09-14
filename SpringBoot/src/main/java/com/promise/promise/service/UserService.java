package com.promise.promise.service;

import com.promise.promise.domain.User;
import com.promise.promise.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void saveUser(User user) {
        userRepository.save(user);  // User 엔티티를 DB에 저장
    }
}