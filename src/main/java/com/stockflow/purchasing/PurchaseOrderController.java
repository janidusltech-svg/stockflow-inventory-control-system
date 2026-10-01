package com.stockflow.purchasing;

import com.stockflow.product.ProductRepository;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
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
@RequestMapping("/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;
    private final SupplierService supplierService;
    private final ProductRepository productRepository;

    public PurchaseOrderController(
            PurchaseOrderService purchaseOrderService,
            SupplierService supplierService,
            ProductRepository productRepository
    ) {
        this.purchaseOrderService = purchaseOrderService;
        this.supplierService = supplierService;
        this.productRepository = productRepository;
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Model model
    ) {
        model.addAttribute(
                "purchaseOrders",
                purchaseOrderService.findAll(
                        keyword,
                        status,
                        fromDate,
                        toDate
                )
        );
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("statuses", PurchaseOrderStatus.values());

        return "purchase-orders/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute(
                "purchaseOrderForm",
                new PurchaseOrderForm()
        );
        prepareOrderForm(
                model,
                "Create Purchase Order",
                "/purchase-orders"
        );
        return "purchase-orders/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute
            PurchaseOrderForm purchaseOrderForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepareOrderForm(
                    model,
                    "Create Purchase Order",
                    "/purchase-orders"
            );
            return "purchase-orders/form";
        }

        try {
            PurchaseOrder order =
                    purchaseOrderService.create(purchaseOrderForm);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Purchase order created. Add products before approval."
            );

            return "redirect:/purchase-orders/" + order.getId();
        } catch (IllegalArgumentException exception) {
            bindingResult.reject(
                    "purchaseOrder",
                    exception.getMessage()
            );

            prepareOrderForm(
                    model,
                    "Create Purchase Order",
                    "/purchase-orders"
            );

            return "purchase-orders/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(
            @PathVariable Long id,
            Model model
    ) {
        prepareDetail(
                model,
                purchaseOrderService.findById(id),
                new PurchaseOrderItemForm()
        );

        return "purchase-orders/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        PurchaseOrder order = purchaseOrderService.findById(id);

        if (!order.isEditable()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Only draft purchase orders can be edited"
            );
            return "redirect:/purchase-orders/" + id;
        }

        model.addAttribute(
                "purchaseOrderForm",
                purchaseOrderService.toForm(order)
        );

        prepareOrderForm(
                model,
                "Edit Purchase Order",
                "/purchase-orders/" + id
        );

        model.addAttribute(
                "orderNumber",
                order.getOrderNumber()
        );

        return "purchase-orders/form";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute
            PurchaseOrderForm purchaseOrderForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepareOrderForm(
                    model,
                    "Edit Purchase Order",
                    "/purchase-orders/" + id
            );

            model.addAttribute(
                    "orderNumber",
                    purchaseOrderService
                            .findById(id)
                            .getOrderNumber()
            );

            return "purchase-orders/form";
        }

        try {
            purchaseOrderService.update(
                    id,
                    purchaseOrderForm
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Purchase order updated successfully"
            );

            return "redirect:/purchase-orders/" + id;
        } catch (IllegalArgumentException exception) {
            bindingResult.reject(
                    "purchaseOrder",
                    exception.getMessage()
            );

            prepareOrderForm(
                    model,
                    "Edit Purchase Order",
                    "/purchase-orders/" + id
            );

            model.addAttribute(
                    "orderNumber",
                    purchaseOrderService
                            .findById(id)
                            .getOrderNumber()
            );

            return "purchase-orders/form";
        }
    }

    @PostMapping("/{id}/items")
    public String addItem(
            @PathVariable Long id,
            @Valid @ModelAttribute
            PurchaseOrderItemForm orderItemForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        PurchaseOrder order =
                purchaseOrderService.findById(id);

        if (bindingResult.hasErrors()) {
            prepareDetail(
                    model,
                    order,
                    orderItemForm
            );
            return "purchase-orders/detail";
        }

        try {
            purchaseOrderService.addItem(
                    id,
                    orderItemForm
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Product added to purchase order"
            );

            return "redirect:/purchase-orders/" + id;
        } catch (IllegalArgumentException exception) {
            bindingResult.reject(
                    "orderItem",
                    exception.getMessage()
            );

            prepareDetail(
                    model,
                    order,
                    orderItemForm
            );

            return "purchase-orders/detail";
        }
    }

    @PostMapping("/{orderId}/items/{itemId}/remove")
    public String removeItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            purchaseOrderService.removeItem(
                    orderId,
                    itemId
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Product removed from purchase order"
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        return "redirect:/purchase-orders/" + orderId;
    }

    @PostMapping("/{id}/status")
    public String changeStatus(
            @PathVariable Long id,
            @RequestParam PurchaseOrderStatus status,
            RedirectAttributes redirectAttributes
    ) {
        try {
            if (status == PurchaseOrderStatus.RECEIVED
                    || status ==
                    PurchaseOrderStatus.PARTIALLY_RECEIVED) {

                throw new IllegalArgumentException(
                        "Receiving status is updated by the Goods Receiving module"
                );
            }

            purchaseOrderService.changeStatus(id, status);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Purchase order status changed to "
                            + status.name().toLowerCase()
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        return "redirect:/purchase-orders/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            purchaseOrderService.deleteDraft(id);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Draft purchase order deleted permanently"
            );
            return "redirect:/purchase-orders";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        } catch (DataIntegrityViolationException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "This purchase order is linked to other records and was not deleted"
            );
        }
        return "redirect:/purchase-orders/" + id;
    }

    private void prepareOrderForm(
            Model model,
            String pageTitle,
            String formAction
    ) {
        model.addAttribute(
                "suppliers",
                supplierService.findAll(null)
        );
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("formAction", formAction);
    }

    private void prepareDetail(
            Model model,
            PurchaseOrder order,
            PurchaseOrderItemForm itemForm
    ) {
        model.addAttribute("purchaseOrder", order);
        model.addAttribute("orderItemForm", itemForm);

        model.addAttribute(
                "products",
                productRepository.findAll()
                        .stream()
                        .filter(product -> product.isActive())
                        .toList()
        );
    }
}