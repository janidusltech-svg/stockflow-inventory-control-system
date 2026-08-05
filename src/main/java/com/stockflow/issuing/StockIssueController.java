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
@RequestMapping("/stock-issues")
public class StockIssueController {

    private final StockIssueService stockIssueService;

    public StockIssueController(StockIssueService stockIssueService) {
        this.stockIssueService = stockIssueService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) StockIssueStatus status,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                       Model model) {
        model.addAttribute("stockIssues",
                stockIssueService.findAll(keyword, status, fromDate, toDate));
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("statuses", StockIssueStatus.values());
        return "stock-issues/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("stockIssueForm", new StockIssueForm());
        prepareForm(model, "Create Stock Issue", "/stock-issues");
        return "stock-issues/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute StockIssueForm stockIssueForm,
                         BindingResult bindingResult, Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, "Create Stock Issue", "/stock-issues");
            return "stock-issues/form";
        }
        try {
            StockIssue issue = stockIssueService.create(stockIssueForm);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Draft stock issue created. Add the products to issue.");
            return "redirect:/stock-issues/" + issue.getId();
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("stockIssue", ex.getMessage());
            prepareForm(model, "Create Stock Issue", "/stock-issues");
            return "stock-issues/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        prepareDetail(model, stockIssueService.findById(id), new StockIssueItemForm());
        return "stock-issues/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model,
                           RedirectAttributes redirectAttributes) {
        StockIssue issue = stockIssueService.findById(id);
        if (!issue.isEditable()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Only draft stock issues can be edited");
            return "redirect:/stock-issues/" + id;
        }
        model.addAttribute("stockIssueForm", stockIssueService.toForm(issue));
        model.addAttribute("issueNumber", issue.getIssueNumber());
        prepareForm(model, "Edit Stock Issue", "/stock-issues/" + id);
        return "stock-issues/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute StockIssueForm stockIssueForm,
                         BindingResult bindingResult, Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, "Edit Stock Issue", "/stock-issues/" + id);
            model.addAttribute("issueNumber", stockIssueService.findById(id).getIssueNumber());
            return "stock-issues/form";
        }
        try {
            stockIssueService.update(id, stockIssueForm);
            redirectAttributes.addFlashAttribute("successMessage", "Stock issue updated");
            return "redirect:/stock-issues/" + id;
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("stockIssue", ex.getMessage());
            prepareForm(model, "Edit Stock Issue", "/stock-issues/" + id);
            model.addAttribute("issueNumber", stockIssueService.findById(id).getIssueNumber());
            return "stock-issues/form";
        }
    }

    @PostMapping("/{id}/items")
    public String addItem(@PathVariable Long id,
                          @Valid @ModelAttribute StockIssueItemForm stockIssueItemForm,
                          BindingResult bindingResult, Model model,
                          RedirectAttributes redirectAttributes) {
        StockIssue issue = stockIssueService.findById(id);
        if (bindingResult.hasErrors()) {
            prepareDetail(model, issue, stockIssueItemForm);
            return "stock-issues/detail";
        }
        try {
            stockIssueService.addItem(id, stockIssueItemForm);
            redirectAttributes.addFlashAttribute("successMessage", "Product added to issue");
            return "redirect:/stock-issues/" + id;
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("stockIssueItem", ex.getMessage());
            prepareDetail(model, issue, stockIssueItemForm);
            return "stock-issues/detail";
        }
    }

    @PostMapping("/{issueId}/items/{itemId}/remove")
    public String removeItem(@PathVariable Long issueId, @PathVariable Long itemId,
                             RedirectAttributes redirectAttributes) {
        try {
            stockIssueService.removeItem(issueId, itemId);
            redirectAttributes.addFlashAttribute("successMessage", "Issue item removed");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/stock-issues/" + issueId;
    }

    @PostMapping("/{id}/confirm")
    public String confirm(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            stockIssueService.confirm(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Issue confirmed and stock quantities reduced");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/stock-issues/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            stockIssueService.cancelDraft(id);
            redirectAttributes.addFlashAttribute("successMessage", "Draft issue cancelled");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/stock-issues/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            stockIssueService.deleteDraft(id);
            redirectAttributes.addFlashAttribute("successMessage", "Draft issue deleted");
            return "redirect:/stock-issues";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/stock-issues/" + id;
        }
    }

    private void prepareForm(Model model, String pageTitle, String formAction) {
        model.addAttribute("recipients", stockIssueService.findActiveRecipients());
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("formAction", formAction);
    }

    private void prepareDetail(Model model, StockIssue issue, StockIssueItemForm itemForm) {
        model.addAttribute("stockIssue", issue);
        model.addAttribute("stockIssueItemForm", itemForm);
        model.addAttribute("availableStock", stockIssueService.findAvailableStock());
    }
}
