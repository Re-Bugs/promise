package com.onlypromise.promise.controller.api;


import com.onlypromise.promise.DTO.api.LoginDTO;
import com.onlypromise.promise.domain.User;
import com.onlypromise.promise.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class LoginAPIController {

    private final UserService userService;

    @PatchMapping(value = "/login", produces = "application/json")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginDTO loginDTO)
    {
        Map<String, String> response =new HashMap<>();
        if(userService.APILogin(loginDTO))
        {
            Optional<User> findUser = userService.findUserByBottleId(loginDTO.getBottleId());
            User updateUser = findUser.get().toBuilder().name(loginDTO.getName()).age(loginDTO.getAge()).build();
            userService.save(updateUser);
            response.put("message", "success");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }
            response.put("message", "user not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

}
