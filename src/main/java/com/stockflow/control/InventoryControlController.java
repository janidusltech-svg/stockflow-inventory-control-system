package com.stockflow.control;

import com.stockflow.inventory.StockMovementType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/inventory-control")
public class InventoryControlController {

    private final InventoryControlService inventoryControlService;

    public InventoryControlController(InventoryControlService inventoryControlService) {
        this.inventoryControlService = inventoryControlService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("summary", inventoryControlService.getSummary());
        model.addAttribute("priorityAlerts", inventoryControlService.findPriorityAlerts());
        model.addAttribute("expiringBatches", inventoryControlService.findExpiringBatches());
        model.addAttribute("recentMovements", inventoryControlService.findRecentMovements());
        return "inventory-control/dashboard";
    }

    @GetMapping("/stock")
    public String stock(@RequestParam(required = false) String keyword, Model model) {
        model.addAttribute("balances", inventoryControlService.findStock(keyword));
        model.addAttribute("keyword", keyword);
        return "inventory-control/stock";
    }

    @GetMapping("/movements")
    public String movements(@RequestParam(required = false) StockMovementType movementType,
                            @RequestParam(required = false) String keyword,
                            Model model) {
        model.addAttribute("movements", inventoryControlService.findMovements(movementType, keyword));
        model.addAttribute("movementTypes", StockMovementType.values());
        model.addAttribute("selectedMovementType", movementType);
        model.addAttribute("keyword", keyword);
        return "inventory-control/movements";
    }
}
