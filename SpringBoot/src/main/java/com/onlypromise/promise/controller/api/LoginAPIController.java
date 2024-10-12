package com.onlypromise.promise.controller.api;


import com.onlypromise.promise.DTO.api.LoginDTO;
import com.onlypromise.promise.domain.User;
import com.onlypromise.promise.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class LoginAPIController {

    private final UserService userService;

    @PostMapping(value = "/login", produces = "application/json")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginDTO loginDTO)
    {
        Map<String, String> response =new HashMap<>();
        if(userService.APILogin(loginDTO))
        {
            Optional<User> findUser = userService.findUserByBottleId(loginDTO.getBottleId());
            User updateUser = findUser.get().toBuilder().name(loginDTO.getName()).age(loginDTO.getAge()).build();
            userService.save(updateUser);
            response.put("message", "success");
            log.info("유저 로그인 : {}, 이름 : {}", updateUser.getId(), updateUser.getName());
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }
            response.put("message", "user not found");
            log.info("로그인 실패 - 약통코드 : {}", loginDTO.getBottleId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

}
