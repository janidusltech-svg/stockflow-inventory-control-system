package com.stockflow.issuing;

import jakarta.validation.Valid;
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
@RequestMapping("/recipients")
public class RecipientController {

    private final RecipientService recipientService;

    public RecipientController(RecipientService recipientService) {
        this.recipientService = recipientService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Boolean active,
                       Model model) {
        model.addAttribute("recipients", recipientService.findAll(keyword, active));
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedActive", active);
        return "recipients/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("recipientForm", new RecipientForm());
        prepareForm(model, "Add Recipient", "/recipients");
        return "recipients/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute RecipientForm recipientForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, "Add Recipient", "/recipients");
            return "recipients/form";
        }
        try {
            recipientService.create(recipientForm);
            redirectAttributes.addFlashAttribute("successMessage", "Recipient added successfully.");
            return "redirect:/recipients";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("recipient", ex.getMessage());
            prepareForm(model, "Add Recipient", "/recipients");
            return "recipients/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Recipient recipient = recipientService.findById(id);
        model.addAttribute("recipientForm", recipientService.toForm(recipient));
        prepareForm(model, "Edit Recipient", "/recipients/" + id);
        return "recipients/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute RecipientForm recipientForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, "Edit Recipient", "/recipients/" + id);
            return "recipients/form";
        }
        try {
            recipientService.update(id, recipientForm);
            redirectAttributes.addFlashAttribute("successMessage", "Recipient updated successfully.");
            return "redirect:/recipients";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("recipient", ex.getMessage());
            prepareForm(model, "Edit Recipient", "/recipients/" + id);
            return "recipients/form";
        }
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        recipientService.setActive(id, false);
        redirectAttributes.addFlashAttribute("successMessage", "Recipient deactivated.");
        return "redirect:/recipients";
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        recipientService.setActive(id, true);
        redirectAttributes.addFlashAttribute("successMessage", "Recipient activated.");
        return "redirect:/recipients";
    }

    private void prepareForm(Model model, String pageTitle, String formAction) {
        model.addAttribute("recipientTypes", RecipientType.values());
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("formAction", formAction);
    }
}
