package com.stockflow.issuing;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;

@Controller
@RequestMapping("/stock-returns")
public class StockReturnController {

    private final StockReturnService stockReturnService;

    public StockReturnController(StockReturnService stockReturnService) {
        this.stockReturnService = stockReturnService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) StockReturnStatus status,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                       Model model) {
        model.addAttribute("stockReturns",
                stockReturnService.findAll(keyword, status, fromDate, toDate));
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("statuses", StockReturnStatus.values());
        return "stock-returns/list";
    }

    @GetMapping("/new")
    public String createForm(@RequestParam(required = false) Long stockIssueId, Model model) {
        StockReturnForm form = new StockReturnForm();
        form.setStockIssueId(stockIssueId);
        model.addAttribute("stockReturnForm", form);
        prepareForm(model, "Create Stock Return", "/stock-returns");
        return "stock-returns/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute StockReturnForm stockReturnForm,
                         BindingResult bindingResult, Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, "Create Stock Return", "/stock-returns");
            return "stock-returns/form";
        }
        try {
            StockReturn returned = stockReturnService.create(stockReturnForm);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Draft stock return created. Add the returned products.");
            return "redirect:/stock-returns/" + returned.getId();
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("stockReturn", ex.getMessage());
            prepareForm(model, "Create Stock Return", "/stock-returns");
            return "stock-returns/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        prepareDetail(model, stockReturnService.findById(id), new StockReturnItemForm());
        return "stock-returns/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model,
                           RedirectAttributes redirectAttributes) {
        StockReturn returned = stockReturnService.findById(id);
        if (!returned.isEditable()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Only draft stock returns can be edited");
            return "redirect:/stock-returns/" + id;
        }
        model.addAttribute("stockReturnForm", stockReturnService.toForm(returned));
        model.addAttribute("returnNumber", returned.getReturnNumber());
        prepareForm(model, "Edit Stock Return", "/stock-returns/" + id);
        return "stock-returns/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute StockReturnForm stockReturnForm,
                         BindingResult bindingResult, Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, "Edit Stock Return", "/stock-returns/" + id);
            model.addAttribute("returnNumber", stockReturnService.findById(id).getReturnNumber());
            return "stock-returns/form";
        }
        try {
            stockReturnService.update(id, stockReturnForm);
            redirectAttributes.addFlashAttribute("successMessage", "Stock return updated");
            return "redirect:/stock-returns/" + id;
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("stockReturn", ex.getMessage());
            prepareForm(model, "Edit Stock Return", "/stock-returns/" + id);
            model.addAttribute("returnNumber", stockReturnService.findById(id).getReturnNumber());
            return "stock-returns/form";
        }
    }

    @PostMapping("/{id}/items")
    public String addItem(@PathVariable Long id,
                          @Valid @ModelAttribute StockReturnItemForm stockReturnItemForm,
                          BindingResult bindingResult, Model model,
                          RedirectAttributes redirectAttributes) {
        StockReturn returned = stockReturnService.findById(id);
        if (bindingResult.hasErrors()) {
            prepareDetail(model, returned, stockReturnItemForm);
            return "stock-returns/detail";
        }
        try {
            stockReturnService.addItem(id, stockReturnItemForm);
            redirectAttributes.addFlashAttribute("successMessage", "Returned product recorded");
            return "redirect:/stock-returns/" + id;
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("stockReturnItem", ex.getMessage());
            prepareDetail(model, returned, stockReturnItemForm);
            return "stock-returns/detail";
        }
    }

    @PostMapping("/{returnId}/items/{itemId}/remove")
    public String removeItem(@PathVariable Long returnId, @PathVariable Long itemId,
                             RedirectAttributes redirectAttributes) {
        try {
            stockReturnService.removeItem(returnId, itemId);
            redirectAttributes.addFlashAttribute("successMessage", "Return item removed");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/stock-returns/" + returnId;
    }

    @PostMapping("/{id}/confirm")
    public String confirm(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            stockReturnService.confirm(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Return confirmed. Accepted quantities were restored to stock.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/stock-returns/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            stockReturnService.cancelDraft(id);
            redirectAttributes.addFlashAttribute("successMessage", "Draft return cancelled");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/stock-returns/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            stockReturnService.deleteDraft(id);
            redirectAttributes.addFlashAttribute("successMessage", "Draft return deleted");
            return "redirect:/stock-returns";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/stock-returns/" + id;
        }
    }

    private void prepareForm(Model model, String pageTitle, String formAction) {
        model.addAttribute("stockIssues", stockReturnService.findReturnableIssues());
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("formAction", formAction);
    }

    private void prepareDetail(Model model, StockReturn returned,
                               StockReturnItemForm itemForm) {
        StockIssue issue = stockReturnService.findSourceIssue(returned.getStockIssue().getId());
        model.addAttribute("stockReturn", returned);
        model.addAttribute("stockReturnItemForm", itemForm);
        model.addAttribute("returnableItems", issue.getItems().stream()
                .filter(item -> item.getRemainingReturnableQuantity() > 0)
                .toList());
    }
}
