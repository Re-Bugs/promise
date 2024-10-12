package com.onlypromise.promise.controller.web;

import com.onlypromise.promise.domain.User;
import com.onlypromise.promise.domain.enumeration.Role;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class HomeController {

    @GetMapping
    public String home(HttpSession session)
    {
        User user = (User) session.getAttribute("user");
        if(user == null) return "redirect:/login";

        if(user.getRole().equals(Role.admin)) return "redirect:admin/home";

        return "home";
    }

}
