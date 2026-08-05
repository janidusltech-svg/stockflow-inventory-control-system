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
@RequestMapping("/stock-counts")
public class StockCountController {

    private final StockCountService stockCountService;

    public StockCountController(StockCountService stockCountService) {
        this.stockCountService = stockCountService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) StockCountStatus status, Model model) {
        model.addAttribute("stockCounts", stockCountService.findAll(status));
        model.addAttribute("statuses", StockCountStatus.values());
        model.addAttribute("selectedStatus", status);
        return "stock-counts/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("stockCountForm", new StockCountForm());
        model.addAttribute("locations", stockCountService.findLocations());
        return "stock-counts/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute StockCountForm stockCountForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("locations", stockCountService.findLocations());
            return "stock-counts/form";
        }
        try {
            StockCount stockCount = stockCountService.create(stockCountForm);
            redirectAttributes.addFlashAttribute("successMessage", "Stock-count session created from current system quantities.");
            return "redirect:/stock-counts/" + stockCount.getId();
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("stockCount", ex.getMessage());
            model.addAttribute("locations", stockCountService.findLocations());
            return "stock-counts/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("stockCount", stockCountService.findById(id));
        return "stock-counts/detail";
    }

    @PostMapping("/{countId}/items/{itemId}")
    public String updateItem(@PathVariable Long countId,
                             @PathVariable Long itemId,
                             @RequestParam Integer countedQuantity,
                             @RequestParam(required = false) String varianceNote,
                             RedirectAttributes redirectAttributes) {
        try {
            stockCountService.updateItem(countId, itemId, countedQuantity, varianceNote);
            redirectAttributes.addFlashAttribute("successMessage", "Physical quantity saved.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/stock-counts/" + countId;
    }

    @PostMapping("/{id}/submit")
    public String submit(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return perform(id, redirectAttributes,
                "Stock count submitted. Variances were sent for adjustment approval.",
                stockCountService::submit);
    }

    @PostMapping("/{id}/close")
    public String close(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return perform(id, redirectAttributes, "Stock count closed.", stockCountService::close);
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return perform(id, redirectAttributes, "Draft stock count cancelled.", stockCountService::cancel);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            stockCountService.deleteDraft(id);
            redirectAttributes.addFlashAttribute("successMessage", "Draft stock count deleted.");
            return "redirect:/stock-counts";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/stock-counts/" + id;
        }
    }

    private String perform(Long id, RedirectAttributes redirectAttributes,
                           String successText, java.util.function.Consumer<Long> action) {
        try {
            action.accept(id);
            redirectAttributes.addFlashAttribute("successMessage", successText);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/stock-counts/" + id;
    }
}
