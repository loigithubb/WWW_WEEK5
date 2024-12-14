/*
 * @ (#) UserController.java       1.0     08/11/2024
 *
 * Copyright (c) 2024 IUH. All rights reserved.
 */

package vn.edu.iuh.fit.frontend.controllers;
/*
 * @description:
 * @author: Nguyen Thanh Nhut
 * @date: 08/11/2024
 * @version:    1.0
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.iuh.fit.backend.dtos.*;
import vn.edu.iuh.fit.frontend.models.*;

import java.security.Principal;
import java.util.*;

@Controller
@RequestMapping(value = "/company")
public class UserController {

    private final AddressModel addressModel;
    private final CompanyModel companyModel;
    private final JobModel jobModel;
    private final UserModel userModel;
    private final JobApplicationModel jobApplicationModel;

    public UserController(AddressModel addressModel, CompanyModel companyModel, JobModel jobModel, UserModel userModel, JobApplicationModel jobApplicationModel) {
        this.addressModel = addressModel;
        this.companyModel = companyModel;
        this.jobModel = jobModel;
        this.userModel = userModel;
        this.jobApplicationModel = jobApplicationModel;
    }


    @RequestMapping(value = "/account-registration", method = RequestMethod.GET)
    public String showRegistrationForm(Model model) throws JsonProcessingException {

        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof UsernamePasswordAuthenticationToken)) {
            return "redirect:/login";
        }
        List<String> roles = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        if (roles.contains("COMPANY")) {
            return "redirect:/company/dashboard";
        }

        List<Map<String, String>> countries = addressModel.getCountries();
        model.addAttribute("company", new CompanyDTO());
        model.addAttribute("countries", countries);
        return "companies/company-account-registration";
    }


    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public String registerCompany(@ModelAttribute("company") CompanyDTO companyDTO, Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        UserDTO currentUser = userModel.getCurrentUser();
        companyDTO.setUserId(currentUser.getId());
        if (companyModel.saveCompany(companyDTO)) {
            return "companies/company-dashboard";
        }
        return "companies/company-account-registration";
    }

    @RequestMapping(value = "/dashboard", method = RequestMethod.GET)
    public String showDashboard(@RequestParam(value = "jobId", required = false) Long jobId,
                                @RequestParam(value = "search", required = false) String search,
                                @RequestParam(value = "activeTab", required = false) String activeTab, Model model) {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof UsernamePasswordAuthenticationToken)) {
            return "redirect:/login";
        }

        User userDetails = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<String> roles = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

        // Check if the user has already registered a company
        if (!roles.contains("COMPANY")) {
            return "redirect:/company/account-registration";
        }

        CompanyDTO company = companyModel.getCompanyByUsername(userDetails.getUsername());
        model.addAttribute("company", company);
        List<JobDTO> jobs = jobModel.getJobsByCompanyId(company.getId(), 0, 15).getContent().stream().toList();
        model.addAttribute("jobs", jobs);

        long jobIdValue = jobId == null ? 0 : jobId;
        String searchValue = search == null ? "" : search;

        List<JobApplicationDTO> jobApplicationDTOS = jobApplicationModel.getCandidatesByCompanyId(company.getId(), jobIdValue, searchValue, 0, 15).getContent().stream().toList();
        model.addAttribute("jobApplicationDTOS", jobApplicationDTOS);

        JobDTO jobDTO = new JobDTO();
        jobDTO.setJobSkills(new ArrayList<>());
        jobDTO.setCompany(company);
        model.addAttribute("jobDTO", jobDTO);

        Set<SkillDTO> skills = jobModel.getAllSkills();
        model.addAttribute("skills", skills);

        if (jobId!= null || !searchValue.isEmpty()) {
            model.addAttribute("selectedJobId", jobIdValue);
            model.addAttribute("activeTab", "candidates");
        } else {
            model.addAttribute("activeTab", activeTab == null ? "dashboard" : activeTab);
        }

        return "companies/company-dashboard";
    }

    @RequestMapping(value = "/job/save", method = RequestMethod.POST)
    public String saveJob(@ModelAttribute("jobDTO") JobDTO jobDTO, RedirectAttributes redirectAttributes) {
        jobDTO.setStatus(1);
        if (jobModel.saveJob(jobDTO)) {
            redirectAttributes.addFlashAttribute("successMessage", "Job saved successfully");
            redirectAttributes.addFlashAttribute("activeTab", "jobs");
            return "redirect:/company/dashboard";
        }
        return "companies/company-dashboard";
    }

    @RequestMapping(value = "/send-email", method = RequestMethod.POST)
    public String sendEmail(@RequestParam("applicationId") Long jobApplicationId,
                            @RequestParam("to") String to, @RequestParam("subject") String subject,
                            @RequestParam("message") String message, RedirectAttributes redirectAttributes) {
        if (companyModel.sendEmail(to, subject, message)) {
            redirectAttributes.addFlashAttribute("emailSuccessMessage", "Email sent successfully");
            jobApplicationModel.updateStatus(jobApplicationId, 1);
        } else {
            redirectAttributes.addFlashAttribute("emailErrorMessage", "Email sent failed");
        }
        return "redirect:/company/dashboard?activeTab=candidates";
    }

    @RequestMapping(value = "/job-application/reject", method = RequestMethod.POST)
    public String rejectJobApplication(@RequestParam("jobApplicationId") Long jobApplicationId, RedirectAttributes redirectAttributes) {
        try {
            jobApplicationModel.updateStatus(jobApplicationId, -1);
            redirectAttributes.addFlashAttribute("rejectSuccessMessage", "Job application rejected successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("rejectErrorMessage", "Job application rejected failed");
        }
        return "redirect:/company/dashboard?activeTab=candidates";
    }

}
