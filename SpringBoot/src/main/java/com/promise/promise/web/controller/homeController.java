package com.promise.promise.web.controller;

import com.promise.promise.domain.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class homeController {

    @GetMapping
    public String home()
    {
        return "home";
    }

}
