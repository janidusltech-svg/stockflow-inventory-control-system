package com.stockflow.security;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public String profile(Authentication authentication, Model model) {
        String username = authentication.getName();
        model.addAttribute("account", profileService.findByUsername(username));
        if (!model.containsAttribute("profileForm")) {
            model.addAttribute("profileForm", profileService.createForm(username));
        }
        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new PasswordChangeForm());
        }
        return "profile/index";
    }

    @PostMapping
    public String updateProfile(@Valid @ModelAttribute("profileForm") ProfileForm form,
                                BindingResult bindingResult,
                                Authentication authentication,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("account", profileService.findByUsername(authentication.getName()));
            model.addAttribute("passwordForm", new PasswordChangeForm());
            return "profile/index";
        }
        try {
            profileService.updateProfile(authentication.getName(), form);
            redirectAttributes.addFlashAttribute("successMessage", "Your profile was updated successfully");
            return "redirect:/profile";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("account", profileService.findByUsername(authentication.getName()));
            model.addAttribute("passwordForm", new PasswordChangeForm());
            model.addAttribute("errorMessage", exception.getMessage());
            return "profile/index";
        }
    }

    @PostMapping("/password")
    public String changePassword(@Valid @ModelAttribute("passwordForm") PasswordChangeForm form,
                                 BindingResult bindingResult,
                                 Authentication authentication,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("account", profileService.findByUsername(authentication.getName()));
            model.addAttribute("profileForm", profileService.createForm(authentication.getName()));
            return "profile/index";
        }
        try {
            profileService.changePassword(authentication.getName(), form);
            redirectAttributes.addFlashAttribute("successMessage", "Your password was changed successfully");
            return "redirect:/profile";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("account", profileService.findByUsername(authentication.getName()));
            model.addAttribute("profileForm", profileService.createForm(authentication.getName()));
            model.addAttribute("errorMessage", exception.getMessage());
            return "profile/index";
        }
    }
}
