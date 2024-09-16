package com.promise.promise.web.controller.login;

import com.promise.promise.domain.User;
import com.promise.promise.domain.enumeration.Role;
import com.promise.promise.service.UserService;
import com.promise.promise.web.DTO.SignUpDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Slf4j
@Controller
@RequestMapping("/login")
@RequiredArgsConstructor
public class SignupController {

    private final UserService userService;

    @GetMapping(value = "/sign_up")
    public String signUp(Model model) {
        model.addAttribute("signUpDTO", new SignUpDTO());
        return "loginView/sign_up";
    }

    @PostMapping("/sign_up")
    public String registerUser(@Validated @ModelAttribute SignUpDTO signUpDTO, BindingResult bindingResult, RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            log.info("errors={}", bindingResult);
            return "loginView/sign_up";
        }

        // 닉네임 중복 확인
        if (userService.isNickNameExists(signUpDTO.getNickName())) {
            bindingResult.rejectValue("nickName", "error.nickName", "이미 존재하는 닉네임입니다.");
            return "loginView/sign_up";
        }

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
                .role(Role.user) //role 은 기본적으로 user
                .build();

        if (userService.signUp(user)) {
            redirectAttributes.addFlashAttribute("message", "회원가입이 성공적으로 완료되었습니다.");
            return "redirect:/login";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "이미 존재하는 아이디입니다.");
            return "redirect:/login/sign_up";
        }
    }

    //닉네임 중복 방지
    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrityViolationException(DataIntegrityViolationException ex, RedirectAttributes redirectAttributes) {
        if (ex.getCause() instanceof ConstraintViolationException) {
            redirectAttributes.addFlashAttribute("errorMessage", "닉네임이 중복되었습니다. 다른 닉네임을 사용해주세요.");
        }
        return "redirect:/login/sign_up";
    }
}
