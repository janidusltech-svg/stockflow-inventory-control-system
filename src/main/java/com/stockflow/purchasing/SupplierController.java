package com.stockflow.purchasing;

import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
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
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword, Model model) {
        model.addAttribute("suppliers", supplierService.findAll(keyword));
        model.addAttribute("linkedSupplierIds", supplierService.findSupplierIdsWithPurchaseOrders());
        model.addAttribute("keyword", keyword);
        return "suppliers/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("supplier", new Supplier());
        model.addAttribute("pageTitle", "Add Supplier");
        model.addAttribute("formAction", "/suppliers");
        return "suppliers/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute Supplier supplier,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, "Add Supplier", "/suppliers");
            return "suppliers/form";
        }

        try {
            supplierService.create(supplier);
            redirectAttributes.addFlashAttribute("successMessage", "Supplier created successfully");
            return "redirect:/suppliers";
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("supplierCode", "duplicate", ex.getMessage());
            prepareForm(model, "Add Supplier", "/suppliers");
            return "suppliers/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("supplier", supplierService.findById(id));
        prepareForm(model, "Edit Supplier", "/suppliers/" + id);
        return "suppliers/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute Supplier supplier,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, "Edit Supplier", "/suppliers/" + id);
            return "suppliers/form";
        }

        try {
            supplierService.update(id, supplier);
            redirectAttributes.addFlashAttribute("successMessage", "Supplier updated successfully");
            return "redirect:/suppliers";
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("supplierCode", "duplicate", ex.getMessage());
            prepareForm(model, "Edit Supplier", "/suppliers/" + id);
            return "suppliers/form";
        }
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        supplierService.deactivate(id);
        redirectAttributes.addFlashAttribute("successMessage", "Supplier deactivated successfully");
        return "redirect:/suppliers";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            supplierService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Supplier deleted permanently");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "This supplier is linked to purchase orders and was not deleted. Deactivate it instead.");
        }
        return "redirect:/suppliers";
    }

    private void prepareForm(Model model, String pageTitle, String formAction) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("formAction", formAction);
    }
}
