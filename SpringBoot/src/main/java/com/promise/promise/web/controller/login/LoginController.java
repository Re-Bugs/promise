package com.promise.promise.web.controller.login;

import com.promise.promise.domain.User;
import com.promise.promise.domain.enumeration.Role;
import com.promise.promise.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/login")
public class LoginController {

    private final UserService userService;

    @Autowired
    public LoginController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping(value = "/sign_up")
    public String signUp(Model model)
    {
        model.addAttribute("user", new SignUpDTO());
        return "loginView/sign_up";
    }

    @PostMapping("/sign_up")
    public String registerUser(@ModelAttribute SignUpDTO signUpDTO, RedirectAttributes redirectAttributes, HttpSession session) {
        //약통코드가 null이면 DB에 NULL 처리하는 로직
        if (signUpDTO.getBottleId() != null && signUpDTO.getBottleId().trim().isEmpty()) //약통 코드가 null이면
            signUpDTO.setBottleId(null);//DB에 null값을 넣음

        //빌더로 user 객체 생성
        User user = User.builder()
                .userId(signUpDTO.getUserId())
                .userPassword(signUpDTO.getUserPassword())
                .name(signUpDTO.getName())
                .age(signUpDTO.getAge())
                .nickName(signUpDTO.getNickName())
                .notificationValue(signUpDTO.getNotificationValue())
                .bottleId(signUpDTO.getBottleId())
                .zipcode(signUpDTO.getZipcode())
                .role(Role.USER) //role 은 기본적으로 user
                .build();

        if(userService.signUp(user))
        {
            redirectAttributes.addFlashAttribute("message", "회원가입이 성공적으로 완료되었습니다.");
            return "redirect:/home";
        }
        else
        {
            redirectAttributes.addFlashAttribute("errorMessage", "이미 존재하는 아이디입니다.");
            return "redirect:/login/sign_up";
        }
    }


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
            return "redirect:/";
        }
        else
        {
            redirectAttributes.addFlashAttribute("errorMessage", "로그인에 실패했습니다. 아이디와 비밀번호를 확인해주세요.");
            return "redirect:/login";
        }
    }
}
