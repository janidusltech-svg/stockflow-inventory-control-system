package com.stockflow.security;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/user-accounts")
@PreAuthorize("hasRole('IT_SYSTEMS_OFFICER')")
public class UserAccountController {

    private final UserAccountService userAccountService;

    public UserAccountController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("accounts", userAccountService.findAll());
        return "user-accounts/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        prepareForm(model, new UserAccountForm(), "Create User Account", "/user-accounts", true);
        return "user-accounts/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("userAccountForm") UserAccountForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, form, "Create User Account", "/user-accounts", true);
            return "user-accounts/form";
        }
        try {
            userAccountService.create(form);
            redirectAttributes.addFlashAttribute("successMessage", "User account created successfully");
            return "redirect:/user-accounts";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("account.invalid", exception.getMessage());
            prepareForm(model, form, "Create User Account", "/user-accounts", true);
            return "user-accounts/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        UserAccount account = userAccountService.findById(id);
        prepareForm(model, UserAccountForm.from(account), "Edit User Account",
                "/user-accounts/" + id, false);
        model.addAttribute("accountId", id);
        return "user-accounts/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("userAccountForm") UserAccountForm form,
                         BindingResult bindingResult,
                         Authentication authentication,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, form, "Edit User Account", "/user-accounts/" + id, false);
            model.addAttribute("accountId", id);
            return "user-accounts/form";
        }
        try {
            userAccountService.update(id, form, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "User account updated successfully");
            return "redirect:/user-accounts";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("account.invalid", exception.getMessage());
            prepareForm(model, form, "Edit User Account", "/user-accounts/" + id, false);
            model.addAttribute("accountId", id);
            return "user-accounts/form";
        }
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id,
                               @RequestParam boolean active,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            userAccountService.setActive(id, active, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    active ? "User account activated" : "User account deactivated");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/user-accounts";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id,
                                @RequestParam String newPassword,
                                RedirectAttributes redirectAttributes) {
        try {
            userAccountService.resetPassword(id, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Password reset successfully");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/user-accounts";
    }

    private void prepareForm(Model model, UserAccountForm form, String pageTitle,
                             String formAction, boolean creating) {
        model.addAttribute("userAccountForm", form);
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("formAction", formAction);
        model.addAttribute("roles", UserRole.values());
        model.addAttribute("creating", creating);
    }
}
