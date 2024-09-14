package com.promise.promise.web.controller.login;

import com.promise.promise.domain.User;
import com.promise.promise.domain.enumeration.Role;
import com.promise.promise.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/login")
public class LoginController {

    private final UserService userService;

    // 생성자 주입 방식
    @Autowired
    public LoginController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping(value = "/sign_up")
    public String signUp(Model model)
    {
        model.addAttribute("user", new SignUpDTO());
        return "sign_up";
    }

    @PostMapping("/sign_up")
    public String registerUser(@ModelAttribute("signUpDTO") SignUpDTO signUpDTO) {
        if (signUpDTO.getBottleId() != null && signUpDTO.getBottleId().trim().isEmpty()) //약통 코드가 null이면
            signUpDTO.setBottleId(null);//DB에 null값을 넣음

        User user = User.builder()
                .userId(signUpDTO.getUserId())
                .userPassword(signUpDTO.getUserPassword())
                .name(signUpDTO.getName())
                .age(signUpDTO.getAge())
                .nickName(signUpDTO.getNickName())
                .notificationValue(signUpDTO.getNotificationValue())
                .bottleId(signUpDTO.getBottleId())
                .zipcode(signUpDTO.getZipcode())
                .role(Role.USER)
                .build();

        userService.saveUser(user);
        return "redirect:/";
    }


}
