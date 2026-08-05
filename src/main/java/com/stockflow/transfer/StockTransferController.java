package com.stockflow.transfer;

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
@RequestMapping("/stock-transfers")
public class StockTransferController {

    private final StockTransferService stockTransferService;

    public StockTransferController(StockTransferService stockTransferService) {
        this.stockTransferService = stockTransferService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) StockTransferStatus status,
                       Model model) {
        model.addAttribute("stockTransfers", stockTransferService.findAll(keyword, status));
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", StockTransferStatus.values());
        return "stock-transfers/list";
    }

    @GetMapping("/new")
    public String newTransfer(Model model) {
        model.addAttribute("stockTransferForm", new StockTransferForm());
        prepareForm(model, "Create Stock Transfer", "/stock-transfers");
        return "stock-transfers/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute StockTransferForm stockTransferForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, "Create Stock Transfer", "/stock-transfers");
            return "stock-transfers/form";
        }
        try {
            StockTransfer transfer = stockTransferService.create(stockTransferForm);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Draft transfer created. Add stock items next."
            );
            return "redirect:/stock-transfers/" + transfer.getId();
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("transfer", ex.getMessage());
            prepareForm(model, "Create Stock Transfer", "/stock-transfers");
            return "stock-transfers/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        prepareDetail(model, stockTransferService.findById(id), new StockTransferItemForm());
        return "stock-transfers/detail";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        StockTransfer transfer = stockTransferService.findById(id);
        if (!transfer.isEditable()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Only draft transfers can be edited"
            );
            return "redirect:/stock-transfers/" + id;
        }
        model.addAttribute("stockTransferForm", stockTransferService.toForm(transfer));
        model.addAttribute("transferNumber", transfer.getTransferNumber());
        prepareForm(model, "Edit Stock Transfer", "/stock-transfers/" + id);
        return "stock-transfers/form";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute StockTransferForm stockTransferForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute(
                    "transferNumber",
                    stockTransferService.findById(id).getTransferNumber()
            );
            prepareForm(model, "Edit Stock Transfer", "/stock-transfers/" + id);
            return "stock-transfers/form";
        }
        try {
            stockTransferService.update(id, stockTransferForm);
            redirectAttributes.addFlashAttribute("successMessage", "Transfer updated");
            return "redirect:/stock-transfers/" + id;
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("transfer", ex.getMessage());
            model.addAttribute(
                    "transferNumber",
                    stockTransferService.findById(id).getTransferNumber()
            );
            prepareForm(model, "Edit Stock Transfer", "/stock-transfers/" + id);
            return "stock-transfers/form";
        }
    }

    @PostMapping("/{id}/items")
    public String addItem(
            @PathVariable Long id,
            @Valid @ModelAttribute StockTransferItemForm stockTransferItemForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        StockTransfer transfer = stockTransferService.findById(id);
        if (bindingResult.hasErrors()) {
            prepareDetail(model, transfer, stockTransferItemForm);
            return "stock-transfers/detail";
        }
        try {
            stockTransferService.addItem(id, stockTransferItemForm);
            redirectAttributes.addFlashAttribute("successMessage", "Stock item added");
            return "redirect:/stock-transfers/" + id;
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("item", ex.getMessage());
            prepareDetail(model, transfer, stockTransferItemForm);
            return "stock-transfers/detail";
        }
    }

    @PostMapping("/{transferId}/items/{itemId}/remove")
    public String removeItem(@PathVariable Long transferId,
                             @PathVariable Long itemId,
                             RedirectAttributes redirectAttributes) {
        try {
            stockTransferService.removeItem(transferId, itemId);
            redirectAttributes.addFlashAttribute("successMessage", "Transfer item removed");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/stock-transfers/" + transferId;
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return runAction(id, "approved", redirectAttributes, () -> stockTransferService.approve(id));
    }

    @PostMapping("/{id}/dispatch")
    public String dispatch(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return runAction(
                id,
                "dispatched and deducted from the source location",
                redirectAttributes,
                () -> stockTransferService.dispatch(id)
        );
    }

    @PostMapping("/{id}/receive")
    public String receive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return runAction(
                id,
                "received and added to the destination location",
                redirectAttributes,
                () -> stockTransferService.receive(id)
        );
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return runAction(id, "cancelled", redirectAttributes, () -> stockTransferService.cancel(id));
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            stockTransferService.deleteDraft(id);
            redirectAttributes.addFlashAttribute("successMessage", "Draft transfer deleted");
            return "redirect:/stock-transfers";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/stock-transfers/" + id;
        }
    }

    private void prepareForm(Model model, String title, String action) {
        model.addAttribute("locations", stockTransferService.findActiveLocations());
        model.addAttribute("pageTitle", title);
        model.addAttribute("formAction", action);
    }

    private void prepareDetail(Model model,
                               StockTransfer transfer,
                               StockTransferItemForm itemForm) {
        model.addAttribute("stockTransfer", transfer);
        model.addAttribute("stockTransferItemForm", itemForm);
        model.addAttribute(
                "availableBalances",
                transfer.isEditable()
                        ? stockTransferService.findAvailableSourceStock(
                        transfer.getSourceLocation().getId())
                        : java.util.List.of()
        );
    }

    private String runAction(Long id,
                             String successAction,
                             RedirectAttributes redirectAttributes,
                             Runnable action) {
        try {
            action.run();
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Stock transfer " + successAction
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/stock-transfers/" + id;
    }
}
