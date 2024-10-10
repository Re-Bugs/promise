package com.onlypromise.promise.controller.web.admin;

import com.onlypromise.promise.domain.Report;
import com.onlypromise.promise.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class adminReportController {

    private final ReportService reportService;

    @GetMapping("/report/{id}")
    public String reportInfo(@PathVariable long id, RedirectAttributes redirectAttributes, Model model)
    {
        Optional<Report> findReport = reportService.findById(id);

        if(findReport.isEmpty())
        {
            redirectAttributes.addFlashAttribute("errorMessage", "해당 민원을 찾을 수 없습니다.");
            return "redirect:/admin/home";
        }

        Report report = findReport.get();

        model.addAttribute("report", report);
        return "adminView/reportInfo";
    }
}
