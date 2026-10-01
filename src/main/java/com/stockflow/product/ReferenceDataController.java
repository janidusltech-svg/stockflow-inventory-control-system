package com.stockflow.product;

import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
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

import java.util.function.Consumer;

@Controller
@RequestMapping("/reference-data")
@PreAuthorize("hasAnyRole('OPERATIONS_MANAGER', 'STORE_SUPERVISOR')")
public class ReferenceDataController {

    private final ReferenceDataService referenceDataService;

    public ReferenceDataController(ReferenceDataService referenceDataService) {
        this.referenceDataService = referenceDataService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", referenceDataService.findCategories());
        model.addAttribute("brands", referenceDataService.findBrands());
        model.addAttribute("units", referenceDataService.findUnits());
        model.addAttribute("categoryUsage", referenceDataService.categoryUsage());
        model.addAttribute("brandUsage", referenceDataService.brandUsage());
        model.addAttribute("unitUsage", referenceDataService.unitUsage());
        return "reference-data/list";
    }

    @GetMapping("/categories/new")
    public String newCategory(Model model) {
        prepareForm(model, new ReferenceDataForm(), "Add Category",
                "/reference-data/categories", "Category", true, true);
        return "reference-data/form";
    }

    @PostMapping("/categories")
    public String createCategory(
            @Valid @ModelAttribute("referenceDataForm") ReferenceDataForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        return processForm(form, bindingResult, model, redirectAttributes,
                "Add Category", "/reference-data/categories", "Category", true, true,
                referenceDataService::createCategory, "Category created successfully");
    }

    @GetMapping("/categories/{id}/edit")
    public String editCategory(@PathVariable Long id, Model model) {
        prepareForm(model, referenceDataService.getCategoryForm(id), "Edit Category",
                "/reference-data/categories/" + id, "Category", true, true);
        return "reference-data/form";
    }

    @PostMapping("/categories/{id}")
    public String updateCategory(
            @PathVariable Long id,
            @Valid @ModelAttribute("referenceDataForm") ReferenceDataForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        return processForm(form, bindingResult, model, redirectAttributes,
                "Edit Category", "/reference-data/categories/" + id, "Category", true, true,
                value -> referenceDataService.updateCategory(id, value),
                "Category updated successfully");
    }

    @PostMapping("/categories/{id}/status")
    public String categoryStatus(@PathVariable Long id, @RequestParam boolean active,
                                 RedirectAttributes redirectAttributes) {
        referenceDataService.setCategoryActive(id, active);
        addStatusMessage(redirectAttributes, "Category", active);
        return "redirect:/reference-data#categories";
    }

    @GetMapping("/brands/new")
    public String newBrand(Model model) {
        prepareForm(model, new ReferenceDataForm(), "Add Brand",
                "/reference-data/brands", "Brand", false, false);
        return "reference-data/form";
    }

    @PostMapping("/brands")
    public String createBrand(
            @Valid @ModelAttribute("referenceDataForm") ReferenceDataForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        return processForm(form, bindingResult, model, redirectAttributes,
                "Add Brand", "/reference-data/brands", "Brand", false, false,
                referenceDataService::createBrand, "Brand created successfully");
    }

    @GetMapping("/brands/{id}/edit")
    public String editBrand(@PathVariable Long id, Model model) {
        prepareForm(model, referenceDataService.getBrandForm(id), "Edit Brand",
                "/reference-data/brands/" + id, "Brand", false, false);
        return "reference-data/form";
    }

    @PostMapping("/brands/{id}")
    public String updateBrand(
            @PathVariable Long id,
            @Valid @ModelAttribute("referenceDataForm") ReferenceDataForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        return processForm(form, bindingResult, model, redirectAttributes,
                "Edit Brand", "/reference-data/brands/" + id, "Brand", false, false,
                value -> referenceDataService.updateBrand(id, value),
                "Brand updated successfully");
    }

    @PostMapping("/brands/{id}/status")
    public String brandStatus(@PathVariable Long id, @RequestParam boolean active,
                              RedirectAttributes redirectAttributes) {
        referenceDataService.setBrandActive(id, active);
        addStatusMessage(redirectAttributes, "Brand", active);
        return "redirect:/reference-data#brands";
    }

    @GetMapping("/units/new")
    public String newUnit(Model model) {
        prepareForm(model, new ReferenceDataForm(), "Add Measurement Unit",
                "/reference-data/units", "Measurement Unit", true, false);
        return "reference-data/form";
    }

    @PostMapping("/units")
    public String createUnit(
            @Valid @ModelAttribute("referenceDataForm") ReferenceDataForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        return processForm(form, bindingResult, model, redirectAttributes,
                "Add Measurement Unit", "/reference-data/units", "Measurement Unit", true, false,
                referenceDataService::createUnit, "Measurement unit created successfully");
    }

    @GetMapping("/units/{id}/edit")
    public String editUnit(@PathVariable Long id, Model model) {
        prepareForm(model, referenceDataService.getUnitForm(id), "Edit Measurement Unit",
                "/reference-data/units/" + id, "Measurement Unit", true, false);
        return "reference-data/form";
    }

    @PostMapping("/units/{id}")
    public String updateUnit(
            @PathVariable Long id,
            @Valid @ModelAttribute("referenceDataForm") ReferenceDataForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        return processForm(form, bindingResult, model, redirectAttributes,
                "Edit Measurement Unit", "/reference-data/units/" + id,
                "Measurement Unit", true, false,
                value -> referenceDataService.updateUnit(id, value),
                "Measurement unit updated successfully");
    }

    @PostMapping("/units/{id}/status")
    public String unitStatus(@PathVariable Long id, @RequestParam boolean active,
                             RedirectAttributes redirectAttributes) {
        referenceDataService.setUnitActive(id, active);
        addStatusMessage(redirectAttributes, "Measurement unit", active);
        return "redirect:/reference-data#units";
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return delete(() -> referenceDataService.deleteCategory(id), redirectAttributes,
                "Category deleted permanently", "categories");
    }

    @PostMapping("/brands/{id}/delete")
    public String deleteBrand(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return delete(() -> referenceDataService.deleteBrand(id), redirectAttributes,
                "Brand deleted permanently", "brands");
    }

    @PostMapping("/units/{id}/delete")
    public String deleteUnit(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return delete(() -> referenceDataService.deleteUnit(id), redirectAttributes,
                "Measurement unit deleted permanently", "units");
    }

    private String delete(Runnable deleteAction,
                          RedirectAttributes redirectAttributes,
                          String successMessage,
                          String section) {
        try {
            deleteAction.run();
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        } catch (DataIntegrityViolationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "This value is linked to existing products and was not deleted. Deactivate it instead.");
        }
        return "redirect:/reference-data#" + section;
    }

    private String processForm(ReferenceDataForm form,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes,
                               String pageTitle,
                               String formAction,
                               String referenceKind,
                               boolean showCode,
                               boolean showDescription,
                               Consumer<ReferenceDataForm> saveAction,
                               String successMessage) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, form, pageTitle, formAction, referenceKind,
                    showCode, showDescription);
            return "reference-data/form";
        }
        try {
            saveAction.accept(form);
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
            return "redirect:/reference-data";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("reference-data.invalid", exception.getMessage());
            prepareForm(model, form, pageTitle, formAction, referenceKind,
                    showCode, showDescription);
            return "reference-data/form";
        }
    }

    private void prepareForm(Model model,
                             ReferenceDataForm form,
                             String pageTitle,
                             String formAction,
                             String referenceKind,
                             boolean showCode,
                             boolean showDescription) {
        model.addAttribute("referenceDataForm", form);
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("formAction", formAction);
        model.addAttribute("referenceKind", referenceKind);
        model.addAttribute("showCode", showCode);
        model.addAttribute("showDescription", showDescription);
    }

    private void addStatusMessage(RedirectAttributes redirectAttributes,
                                  String label, boolean active) {
        redirectAttributes.addFlashAttribute("successMessage",
                label + (active ? " activated successfully" : " deactivated successfully"));
    }
}
