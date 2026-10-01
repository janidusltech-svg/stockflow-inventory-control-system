package com.stockflow.warehouse;

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
@RequestMapping("/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("warehouses", warehouseService.findAllWarehouses());
        model.addAttribute("locations", warehouseService.findAllLocations());
        model.addAttribute("warehouseIdsWithLocations", warehouseService.findWarehouseIdsWithLocations());
        model.addAttribute("linkedLocationIds", warehouseService.findLinkedLocationIds());
        return "warehouses/list";
    }

    @GetMapping("/new")
    public String newWarehouse(Model model) {
        model.addAttribute("warehouseForm", new WarehouseForm());
        prepareWarehouseForm(model, "Add Warehouse", "/warehouses");
        return "warehouses/form";
    }

    @PostMapping
    public String createWarehouse(
            @Valid @ModelAttribute WarehouseForm warehouseForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareWarehouseForm(model, "Add Warehouse", "/warehouses");
            return "warehouses/form";
        }
        try {
            warehouseService.createWarehouse(warehouseForm);
            redirectAttributes.addFlashAttribute("successMessage", "Warehouse added");
            return "redirect:/warehouses";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("warehouse", ex.getMessage());
            prepareWarehouseForm(model, "Add Warehouse", "/warehouses");
            return "warehouses/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editWarehouse(@PathVariable Long id, Model model) {
        Warehouse warehouse = warehouseService.findWarehouse(id);
        model.addAttribute("warehouseForm", warehouseService.toWarehouseForm(warehouse));
        prepareWarehouseForm(model, "Edit Warehouse", "/warehouses/" + id);
        return "warehouses/form";
    }

    @PostMapping("/{id}")
    public String updateWarehouse(
            @PathVariable Long id,
            @Valid @ModelAttribute WarehouseForm warehouseForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareWarehouseForm(model, "Edit Warehouse", "/warehouses/" + id);
            return "warehouses/form";
        }
        try {
            warehouseService.updateWarehouse(id, warehouseForm);
            redirectAttributes.addFlashAttribute("successMessage", "Warehouse updated");
            return "redirect:/warehouses";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("warehouse", ex.getMessage());
            prepareWarehouseForm(model, "Edit Warehouse", "/warehouses/" + id);
            return "warehouses/form";
        }
    }

    @PostMapping("/{id}/deactivate")
    public String deactivateWarehouse(@PathVariable Long id,
                                      RedirectAttributes redirectAttributes) {
        try {
            warehouseService.deactivateWarehouse(id);
            redirectAttributes.addFlashAttribute("successMessage", "Warehouse deactivated");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/warehouses";
    }

    @GetMapping("/locations/new")
    public String newLocation(@RequestParam(required = false) Long warehouseId,
                              Model model) {
        StorageLocationForm form = new StorageLocationForm();
        form.setWarehouseId(warehouseId);
        model.addAttribute("storageLocationForm", form);
        prepareLocationForm(model, "Add Storage Location", "/warehouses/locations");
        return "storage-locations/form";
    }

    @PostMapping("/locations")
    public String createLocation(
            @Valid @ModelAttribute StorageLocationForm storageLocationForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareLocationForm(model, "Add Storage Location", "/warehouses/locations");
            return "storage-locations/form";
        }
        try {
            warehouseService.createLocation(storageLocationForm);
            redirectAttributes.addFlashAttribute("successMessage", "Storage location added");
            return "redirect:/warehouses";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("location", ex.getMessage());
            prepareLocationForm(model, "Add Storage Location", "/warehouses/locations");
            return "storage-locations/form";
        }
    }

    @GetMapping("/locations/{id}/edit")
    public String editLocation(@PathVariable Long id, Model model) {
        StorageLocation location = warehouseService.findLocation(id);
        model.addAttribute(
                "storageLocationForm",
                warehouseService.toLocationForm(location)
        );
        prepareLocationForm(
                model,
                "Edit Storage Location",
                "/warehouses/locations/" + id
        );
        return "storage-locations/form";
    }

    @PostMapping("/locations/{id}")
    public String updateLocation(
            @PathVariable Long id,
            @Valid @ModelAttribute StorageLocationForm storageLocationForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareLocationForm(
                    model,
                    "Edit Storage Location",
                    "/warehouses/locations/" + id
            );
            return "storage-locations/form";
        }
        try {
            warehouseService.updateLocation(id, storageLocationForm);
            redirectAttributes.addFlashAttribute("successMessage", "Storage location updated");
            return "redirect:/warehouses";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("location", ex.getMessage());
            prepareLocationForm(
                    model,
                    "Edit Storage Location",
                    "/warehouses/locations/" + id
            );
            return "storage-locations/form";
        }
    }

    @PostMapping("/locations/{id}/deactivate")
    public String deactivateLocation(@PathVariable Long id,
                                     RedirectAttributes redirectAttributes) {
        try {
            warehouseService.deactivateLocation(id);
            redirectAttributes.addFlashAttribute("successMessage", "Storage location deactivated");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/warehouses";
    }

    @PostMapping("/{id}/delete")
    public String deleteWarehouse(@PathVariable Long id,
                                  RedirectAttributes redirectAttributes) {
        try {
            warehouseService.deleteWarehouse(id);
            redirectAttributes.addFlashAttribute("successMessage", "Warehouse deleted permanently");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "This warehouse is linked to other records and was not deleted. Deactivate it instead.");
        }
        return "redirect:/warehouses";
    }

    @PostMapping("/locations/{id}/delete")
    public String deleteLocation(@PathVariable Long id,
                                 RedirectAttributes redirectAttributes) {
        try {
            warehouseService.deleteLocation(id);
            redirectAttributes.addFlashAttribute("successMessage", "Storage location deleted permanently");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "This storage location is linked to other records and was not deleted. Deactivate it instead.");
        }
        return "redirect:/warehouses";
    }

    private void prepareWarehouseForm(Model model, String title, String action) {
        model.addAttribute("pageTitle", title);
        model.addAttribute("formAction", action);
    }

    private void prepareLocationForm(Model model, String title, String action) {
        model.addAttribute("warehouses", warehouseService.findActiveWarehouses());
        model.addAttribute("pageTitle", title);
        model.addAttribute("formAction", action);
    }
}
