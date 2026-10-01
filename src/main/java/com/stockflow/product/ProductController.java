package com.stockflow.product;

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
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) String keyword,
            Model model
    ) {
        model.addAttribute("products", productService.findAll(keyword));
        model.addAttribute("linkedProductIds", productService.findLinkedProductIds());
        model.addAttribute("keyword", keyword);
        return "products/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("productForm", new ProductForm());
        addFormData(model, "Add Product", "/products");
        return "products/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("productForm") ProductForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            addFormData(model, "Add Product", "/products");
            return "products/form";
        }

        try {
            productService.create(form);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Product created successfully"
            );
            return "redirect:/products";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject(
                    "catalogue.error",
                    exception.getMessage()
            );
            addFormData(model, "Add Product", "/products");
            return "products/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("productForm", productService.getForm(id));
        addFormData(model, "Edit Product", "/products/" + id);
        return "products/form";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("productForm") ProductForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            addFormData(model, "Edit Product", "/products/" + id);
            return "products/form";
        }

        try {
            productService.update(id, form);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Product updated successfully"
            );
            return "redirect:/products";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject(
                    "catalogue.error",
                    exception.getMessage()
            );
            addFormData(model, "Edit Product", "/products/" + id);
            return "products/form";
        }
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        productService.deactivate(id);
        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Product deactivated successfully"
        );
        return "redirect:/products";
    }

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            productService.delete(id);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Product deleted permanently"
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        } catch (DataIntegrityViolationException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "This product is linked to other StockFlow records and was not deleted. Deactivate it instead."
            );
        }
        return "redirect:/products";
    }

    private void addFormData(
            Model model,
            String pageTitle,
            String formAction
    ) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("formAction", formAction);
        model.addAttribute(
                "categories",
                productService.findActiveCategories()
        );
        model.addAttribute("brands", productService.findActiveBrands());
        model.addAttribute("units", productService.findActiveUnits());
    }
}
