package com.stockflow.control;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reorder-rules")
public class ReorderRuleController {

    private final ReorderRuleService reorderRuleService;

    public ReorderRuleController(ReorderRuleService reorderRuleService) {
        this.reorderRuleService = reorderRuleService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("rules", reorderRuleService.findAll());
        model.addAttribute("alertMap", reorderRuleService.calculateAlertMap());
        return "reorder-rules/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("reorderRuleForm", new ReorderRuleForm());
        prepareForm(model, "Create Reorder Rule", "/reorder-rules");
        return "reorder-rules/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute ReorderRuleForm reorderRuleForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, "Create Reorder Rule", "/reorder-rules");
            return "reorder-rules/form";
        }
        try {
            reorderRuleService.create(reorderRuleForm);
            redirectAttributes.addFlashAttribute("successMessage", "Reorder rule created successfully.");
            return "redirect:/reorder-rules";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("reorderRule", ex.getMessage());
            prepareForm(model, "Create Reorder Rule", "/reorder-rules");
            return "reorder-rules/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        ReorderRule rule = reorderRuleService.findById(id);
        model.addAttribute("reorderRuleForm", reorderRuleService.toForm(rule));
        prepareForm(model, "Edit Reorder Rule", "/reorder-rules/" + id);
        return "reorder-rules/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute ReorderRuleForm reorderRuleForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, "Edit Reorder Rule", "/reorder-rules/" + id);
            return "reorder-rules/form";
        }
        try {
            reorderRuleService.update(id, reorderRuleForm);
            redirectAttributes.addFlashAttribute("successMessage", "Reorder rule updated successfully.");
            return "redirect:/reorder-rules";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("reorderRule", ex.getMessage());
            prepareForm(model, "Edit Reorder Rule", "/reorder-rules/" + id);
            return "reorder-rules/form";
        }
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        reorderRuleService.setActive(id, true);
        redirectAttributes.addFlashAttribute("successMessage", "Reorder rule activated.");
        return "redirect:/reorder-rules";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        reorderRuleService.setActive(id, false);
        redirectAttributes.addFlashAttribute("successMessage", "Reorder rule deactivated.");
        return "redirect:/reorder-rules";
    }

    private void prepareForm(Model model, String pageTitle, String formAction) {
        model.addAttribute("products", reorderRuleService.findProducts());
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("formAction", formAction);
    }
}
