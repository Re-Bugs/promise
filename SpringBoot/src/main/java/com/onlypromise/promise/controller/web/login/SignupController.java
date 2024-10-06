package com.onlypromise.promise.controller.web.login;

import com.onlypromise.promise.domain.User;
import com.onlypromise.promise.domain.enumeration.Role;
import com.onlypromise.promise.service.UserService;
import com.onlypromise.promise.DTO.web.SignUpDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;


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
    public String registerUser(@Validated @ModelAttribute SignUpDTO signUpDTO, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {

        if (bindingResult.hasErrors()) {
            log.info("errors={}", bindingResult);
            return "loginView/sign_up";
        }

        // 닉네임 중복 확인
        if (userService.isNickNameExists(signUpDTO.getNickName())) {
            bindingResult.rejectValue("nickName", "error.nickName", "이미 존재하는 닉네임입니다.");
            return "loginView/sign_up";
        }

        Optional<User> findUser = userService.findUserByBottleId(signUpDTO.getBottleId());

        if(findUser.isEmpty())
        {
            bindingResult.rejectValue("bottleId", "error.bottleId", "잘못된 약통코드입니다.");
            return "loginView/sign_up";
        }

        if (userService.findByUserId(signUpDTO.getUserId()).isEmpty())
        {
            User user = findUser.get().toBuilder()
                    .userId(signUpDTO.getUserId())
                    .userPassword(signUpDTO.getUserPassword())
                    .name(signUpDTO.getName())
                    .age(signUpDTO.getAge())
                    .nickName(signUpDTO.getNickName())
                    .notificationValue(signUpDTO.getNotificationValue())
                    .bottleId(signUpDTO.getBottleId())
                    .zipcode(signUpDTO.getZipcode())
                    .role(Role.user)
                    .build();

            userService.save(user);
            redirectAttributes.addFlashAttribute("message", "회원가입이 성공적으로 완료되었습니다.");
            return "redirect:/login";
        }
        else
        {
            model.addAttribute("errorMessage", "이미 존재하는 아이디입니다.");
            return "loginView/sign_up";
        }
    }

    //닉네임 중복 방지
    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrityViolationException(DataIntegrityViolationException ex, RedirectAttributes redirectAttributes)
    {
        if (ex.getCause() instanceof ConstraintViolationException)
        {
            redirectAttributes.addFlashAttribute("errorMessage", "닉네임이 중복되었습니다. 다른 닉네임을 사용해주세요.");
        }
        return "redirect:/login/sign_up";
    }
}
