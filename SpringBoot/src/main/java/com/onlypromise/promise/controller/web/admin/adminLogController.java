package com.onlypromise.promise.controller.web.admin;

import com.onlypromise.promise.domain.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/log")
public class adminLogController {

    @Value("${app.log.directory:/spring-boot/log}") //app.log.directory 가 설정되지 않을 경우 기본 경로 설정
    private String LOG_DIRECTORY;

    @GetMapping("/view")
    public String viewLogFile(@RequestParam String fileName, Model model, HttpSession session)
    {
        User user = (User) session.getAttribute("user");
        if(user == null) return "redirect:/login";

        File logFile = new File(LOG_DIRECTORY, fileName);

        // 로그 파일 내용 읽기
        StringBuilder logContent = new StringBuilder();
        boolean isErrorLog = false; // 에러 로그가 연속적으로 출력되도록 플래그 설정
        if (logFile.exists())
        {
            try (BufferedReader br = new BufferedReader(new FileReader(logFile)))
            {
                String line;
                while ((line = br.readLine()) != null) {
                    String cssClass = "info"; // 기본값을 info로 설정

                    // 에러 로그가 시작되거나 연속되는 경우
                    if (line.contains("ERROR"))
                    {
                        cssClass = "error";
                        isErrorLog = true; // 에러가 시작됨을 표시
                    }
                    else if (line.contains("WARN"))
                    {
                        cssClass = "warn";
                        isErrorLog = false; // WARN 로그가 나오면 에러 연속 종료
                    }
                    else if (line.contains("INFO"))
                    {
                        cssClass = "info";
                        isErrorLog = false; // INFO 로그가 나오면 에러 연속 종료
                    }
                    else if (isErrorLog) cssClass = "error"; // 에러 로그가 연속되는 동안 계속 error 클래스를 적용

                    logContent.append("<span class='").append(cssClass).append("'>")
                            .append(line).append("</span><br>");
                }
            }
            catch (IOException e)
            {
                log.error("Error reading log file: " + fileName, e);
                return "redirect:/admin/home";
            }
        }
        else
        {
            log.warn("로그파일 존재하지 않음");
            return "redirect:/admin/home";
        }

        model.addAttribute("fileName", fileName);
        model.addAttribute("logContent", logContent.toString());
        return "adminView/logFile";
    }
}