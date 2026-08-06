package com.stockflow.shared.reporting;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Controller
@RequestMapping("/reports")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OPERATIONS_MANAGER','STORE_SUPERVISOR','FINANCE_EXECUTIVE')")
    public String reports(Model model) {
        model.addAttribute("reportSummary", reportingService.summary());
        model.addAttribute("stockByWarehouse", reportingService.stockByWarehouse());
        model.addAttribute("movementActivity", reportingService.movementActivityLastThirtyDays());
        model.addAttribute("inventoryHealth", reportingService.inventoryHealth());
        return "reports/index";
    }

    @GetMapping("/export/stock.csv")
    @PreAuthorize("hasAnyRole('OPERATIONS_MANAGER','STORE_SUPERVISOR','FINANCE_EXECUTIVE','CUSTOMER_SERVICE_COORDINATOR')")
    public ResponseEntity<byte[]> stockCsv() {
        return csv("stock", reportingService.stockCsv());
    }

    @GetMapping("/export/movements.csv")
    @PreAuthorize("hasAnyRole('OPERATIONS_MANAGER','STORE_SUPERVISOR','FINANCE_EXECUTIVE','CUSTOMER_SERVICE_COORDINATOR')")
    public ResponseEntity<byte[]> movementsCsv() {
        return csv("stock_movements", reportingService.movementsCsv());
    }

    @GetMapping("/export/purchase-orders.csv")
    @PreAuthorize("hasAnyRole('OPERATIONS_MANAGER','FINANCE_EXECUTIVE')")
    public ResponseEntity<byte[]> purchaseOrdersCsv() {
        return csv("purchase_orders", reportingService.purchaseOrdersCsv());
    }

    @GetMapping("/export/goods-receipts.csv")
    @PreAuthorize("hasAnyRole('OPERATIONS_MANAGER','STORE_SUPERVISOR','FINANCE_EXECUTIVE')")
    public ResponseEntity<byte[]> goodsReceiptsCsv() {
        return csv("goods_receipts", reportingService.goodsReceiptsCsv());
    }

    private ResponseEntity<byte[]> csv(String reportName, byte[] content) {
        String filename = "stockflow_" + reportName + "_" + LocalDate.now() + ".csv";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(content.length);
        return ResponseEntity.ok().headers(headers).body(content);
    }
}
