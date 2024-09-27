package com.promise.promise.controller.web.login;

import com.promise.promise.domain.User;
import com.promise.promise.service.web.UserService;
import com.promise.promise.DTO.web.LoginDTO;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/login")
@RequiredArgsConstructor
public class LoginController {

    private final UserService userService;

    @GetMapping
    public String login(Model model)
    {
        model.addAttribute("loginDTO", new LoginDTO());
        return "loginView/login";
    }

    @PostMapping
    public String loginProcess(@ModelAttribute LoginDTO loginDTO, RedirectAttributes redirectAttributes, HttpSession session)
    {
        if(userService.login(loginDTO))
        {
            User user = userService.findByUserId(loginDTO.getUserId()).orElseThrow();
            session.setAttribute("user", user);
            return "redirect:/home";
        }
        else
        {
            redirectAttributes.addFlashAttribute("errorMessage", "로그인에 실패했습니다. 아이디와 비밀번호를 확인해주세요.");
            return "redirect:/login";
        }
    }
}
