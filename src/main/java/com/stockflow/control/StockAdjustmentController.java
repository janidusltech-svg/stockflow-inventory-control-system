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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/stock-adjustments")
public class StockAdjustmentController {

    private final StockAdjustmentService stockAdjustmentService;

    public StockAdjustmentController(StockAdjustmentService stockAdjustmentService) {
        this.stockAdjustmentService = stockAdjustmentService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) AdjustmentStatus status, Model model) {
        model.addAttribute("adjustments", stockAdjustmentService.findAll(status));
        model.addAttribute("statuses", AdjustmentStatus.values());
        model.addAttribute("selectedStatus", status);
        return "stock-adjustments/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("stockAdjustmentForm", new StockAdjustmentForm());
        prepareForm(model, "Create Stock Adjustment", "/stock-adjustments");
        return "stock-adjustments/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute StockAdjustmentForm stockAdjustmentForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, "Create Stock Adjustment", "/stock-adjustments");
            return "stock-adjustments/form";
        }
        try {
            stockAdjustmentService.create(stockAdjustmentForm);
            redirectAttributes.addFlashAttribute("successMessage", "Draft stock adjustment created.");
            return "redirect:/stock-adjustments";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("stockAdjustment", ex.getMessage());
            prepareForm(model, "Create Stock Adjustment", "/stock-adjustments");
            return "stock-adjustments/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        StockAdjustment adjustment = stockAdjustmentService.findById(id);
        model.addAttribute("stockAdjustmentForm", stockAdjustmentService.toForm(adjustment));
        model.addAttribute("adjustmentNumber", adjustment.getAdjustmentNumber());
        prepareForm(model, "Edit Stock Adjustment", "/stock-adjustments/" + id);
        return "stock-adjustments/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute StockAdjustmentForm stockAdjustmentForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("adjustmentNumber", stockAdjustmentService.findById(id).getAdjustmentNumber());
            prepareForm(model, "Edit Stock Adjustment", "/stock-adjustments/" + id);
            return "stock-adjustments/form";
        }
        try {
            stockAdjustmentService.update(id, stockAdjustmentForm);
            redirectAttributes.addFlashAttribute("successMessage", "Stock adjustment updated.");
            return "redirect:/stock-adjustments";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("stockAdjustment", ex.getMessage());
            model.addAttribute("adjustmentNumber", stockAdjustmentService.findById(id).getAdjustmentNumber());
            prepareForm(model, "Edit Stock Adjustment", "/stock-adjustments/" + id);
            return "stock-adjustments/form";
        }
    }

    @PostMapping("/{id}/submit")
    public String submit(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return perform(id, redirectAttributes, "submitted for approval", stockAdjustmentService::submit);
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return perform(id, redirectAttributes, "approved and applied to stock", stockAdjustmentService::approve);
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return perform(id, redirectAttributes, "rejected", stockAdjustmentService::reject);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return perform(id, redirectAttributes, "deleted", stockAdjustmentService::deleteDraft);
    }

    private String perform(Long id, RedirectAttributes redirectAttributes,
                           String successText, java.util.function.Consumer<Long> action) {
        try {
            action.accept(id);
            redirectAttributes.addFlashAttribute("successMessage", "Stock adjustment " + successText + ".");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/stock-adjustments";
    }

    private void prepareForm(Model model, String pageTitle, String formAction) {
        model.addAttribute("balances", stockAdjustmentService.findBalances());
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("formAction", formAction);
    }
}
