package com.onlypromise.promise.controller.web.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final String LOG_DIRECTORY = "/home/ubuntu/promise/log";

    @GetMapping("/view")
    public String viewLogFile(@RequestParam String fileName, Model model) {
        File logFile = new File(LOG_DIRECTORY, fileName);

        // 로그 파일 내용 읽기
        StringBuilder logContent = new StringBuilder();
        if (logFile.exists())
        {
            try (BufferedReader br = new BufferedReader(new FileReader(logFile)))
            {
                String line;
                while ((line = br.readLine()) != null) logContent.append(line).append("\n");
            }
            catch (IOException e)
            {
                log.error("Error reading log file: " + fileName, e);
                return "redirect:/admin/home";
            }
        }
        else
        {
            return "redirect:/admin/home";
        }

        model.addAttribute("logContent", logContent.toString());
        return "adminView/logFiles";
    }
}