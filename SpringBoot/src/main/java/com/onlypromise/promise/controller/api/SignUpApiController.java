package com.onlypromise.promise.controller.api;

import com.onlypromise.promise.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class SignUpApiController {

    private final UserService userService;


}