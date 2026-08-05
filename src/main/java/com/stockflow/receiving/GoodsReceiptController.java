package com.stockflow.receiving;

import com.stockflow.purchasing.PurchaseOrder;
import com.stockflow.purchasing.PurchaseOrderRepository;
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
@RequestMapping("/goods-receipts")
public class GoodsReceiptController {

    private final GoodsReceiptService goodsReceiptService;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public GoodsReceiptController(GoodsReceiptService goodsReceiptService,
                                  PurchaseOrderRepository purchaseOrderRepository) {
        this.goodsReceiptService = goodsReceiptService;
        this.purchaseOrderRepository = purchaseOrderRepository;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) GoodsReceiptStatus status,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                       Model model) {
        model.addAttribute("goodsReceipts",
                goodsReceiptService.findAll(keyword, status, fromDate, toDate));
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("statuses", GoodsReceiptStatus.values());
        return "goods-receipts/list";
    }

    @GetMapping("/new")
    public String createForm(@RequestParam(required = false) Long purchaseOrderId,
                             Model model) {
        GoodsReceiptForm form = new GoodsReceiptForm();
        form.setPurchaseOrderId(purchaseOrderId);
        model.addAttribute("goodsReceiptForm", form);
        prepareForm(model, "Create Goods Receipt", "/goods-receipts");
        return "goods-receipts/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute GoodsReceiptForm goodsReceiptForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, "Create Goods Receipt", "/goods-receipts");
            return "goods-receipts/form";
        }
        try {
            GoodsReceipt receipt = goodsReceiptService.create(goodsReceiptForm);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Draft goods receipt created. Add the delivered products."
            );
            return "redirect:/goods-receipts/" + receipt.getId();
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("goodsReceipt", ex.getMessage());
            prepareForm(model, "Create Goods Receipt", "/goods-receipts");
            return "goods-receipts/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        prepareDetail(model, goodsReceiptService.findById(id), new GoodsReceiptItemForm());
        return "goods-receipts/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        GoodsReceipt receipt = goodsReceiptService.findById(id);
        if (!receipt.isEditable()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Only draft goods receipts can be edited"
            );
            return "redirect:/goods-receipts/" + id;
        }
        model.addAttribute("goodsReceiptForm", goodsReceiptService.toForm(receipt));
        model.addAttribute("receiptNumber", receipt.getReceiptNumber());
        prepareForm(model, "Edit Goods Receipt", "/goods-receipts/" + id);
        return "goods-receipts/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute GoodsReceiptForm goodsReceiptForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, "Edit Goods Receipt", "/goods-receipts/" + id);
            model.addAttribute("receiptNumber", goodsReceiptService.findById(id).getReceiptNumber());
            return "goods-receipts/form";
        }
        try {
            goodsReceiptService.update(id, goodsReceiptForm);
            redirectAttributes.addFlashAttribute("successMessage", "Goods receipt updated");
            return "redirect:/goods-receipts/" + id;
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("goodsReceipt", ex.getMessage());
            prepareForm(model, "Edit Goods Receipt", "/goods-receipts/" + id);
            model.addAttribute("receiptNumber", goodsReceiptService.findById(id).getReceiptNumber());
            return "goods-receipts/form";
        }
    }

    @PostMapping("/{id}/items")
    public String addItem(@PathVariable Long id,
                          @Valid @ModelAttribute GoodsReceiptItemForm receiptItemForm,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        GoodsReceipt receipt = goodsReceiptService.findById(id);
        if (bindingResult.hasErrors()) {
            prepareDetail(model, receipt, receiptItemForm);
            return "goods-receipts/detail";
        }
        try {
            goodsReceiptService.addItem(id, receiptItemForm);
            redirectAttributes.addFlashAttribute("successMessage", "Delivered product recorded");
            return "redirect:/goods-receipts/" + id;
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("receiptItem", ex.getMessage());
            prepareDetail(model, receipt, receiptItemForm);
            return "goods-receipts/detail";
        }
    }

    @PostMapping("/{receiptId}/items/{itemId}/remove")
    public String removeItem(@PathVariable Long receiptId,
                             @PathVariable Long itemId,
                             RedirectAttributes redirectAttributes) {
        try {
            goodsReceiptService.removeItem(receiptId, itemId);
            redirectAttributes.addFlashAttribute("successMessage", "Receipt item removed");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/goods-receipts/" + receiptId;
    }

    @PostMapping("/{id}/confirm")
    public String confirm(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            goodsReceiptService.confirm(id);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Receipt confirmed and accepted stock added to inventory"
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/goods-receipts/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            goodsReceiptService.cancelDraft(id);
            redirectAttributes.addFlashAttribute("successMessage", "Draft receipt cancelled");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/goods-receipts/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            goodsReceiptService.deleteDraft(id);
            redirectAttributes.addFlashAttribute("successMessage", "Draft receipt deleted");
            return "redirect:/goods-receipts";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/goods-receipts/" + id;
        }
    }

    private void prepareForm(Model model, String pageTitle, String formAction) {
        model.addAttribute("purchaseOrders", goodsReceiptService.findReceivablePurchaseOrders());
        model.addAttribute("locations", goodsReceiptService.findActiveLocations());
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("formAction", formAction);
    }

    private void prepareDetail(Model model,
                               GoodsReceipt receipt,
                               GoodsReceiptItemForm itemForm) {
        PurchaseOrder order = purchaseOrderRepository
                .findByIdWithDetails(receipt.getPurchaseOrder().getId())
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));
        model.addAttribute("goodsReceipt", receipt);
        model.addAttribute("purchaseOrder", order);
        model.addAttribute("receiptItemForm", itemForm);
        model.addAttribute("availableOrderItems", order.getItems().stream()
                .filter(item -> item.getRemainingQuantity() > 0)
                .toList());
    }
}
