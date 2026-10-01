package com.stockflow.control;

import com.stockflow.inventory.InventoryBalance;
import com.stockflow.inventory.InventoryBalanceRepository;
import com.stockflow.product.Product;
import com.stockflow.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReorderRuleService {

    private final ReorderRuleRepository reorderRuleRepository;
    private final ProductRepository productRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;

    public ReorderRuleService(ReorderRuleRepository reorderRuleRepository,
                              ProductRepository productRepository,
                              InventoryBalanceRepository inventoryBalanceRepository) {
        this.reorderRuleRepository = reorderRuleRepository;
        this.productRepository = productRepository;
        this.inventoryBalanceRepository = inventoryBalanceRepository;
    }

    @Transactional(readOnly = true)
    public List<ReorderRule> findAll() {
        return reorderRuleRepository.findAllWithProduct();
    }

    @Transactional(readOnly = true)
    public ReorderRule findById(Long id) {
        return reorderRuleRepository.findByIdWithProduct(id)
                .orElseThrow(() -> new IllegalArgumentException("Reorder rule not found"));
    }

    @Transactional(readOnly = true)
    public List<Product> findProducts() {
        return productRepository.findAllWithDetails().stream()
                .filter(Product::isActive)
                .toList();
    }

    public ReorderRule create(ReorderRuleForm form) {
        validateLevels(form);
        if (reorderRuleRepository.existsByProductId(form.getProductId())) {
            throw new IllegalArgumentException("A reorder rule already exists for this product");
        }
        ReorderRule rule = new ReorderRule();
        applyForm(rule, form);
        return reorderRuleRepository.save(rule);
    }

    public ReorderRule update(Long id, ReorderRuleForm form) {
        validateLevels(form);
        ReorderRule rule = findById(id);
        if (reorderRuleRepository.existsByProductIdAndIdNot(form.getProductId(), id)) {
            throw new IllegalArgumentException("A reorder rule already exists for this product");
        }
        applyForm(rule, form);
        return reorderRuleRepository.save(rule);
    }

    /**
     * Permanently removes a reorder rule. A rule is a stock-level setting, not
     * transaction history, and no other table refers to it.
     */
    public void delete(Long id) {
        ReorderRule rule = findById(id);
        reorderRuleRepository.delete(rule);
        reorderRuleRepository.flush();
    }

    public void setActive(Long id, boolean active) {
        ReorderRule rule = findById(id);
        rule.setActive(active);
        reorderRuleRepository.save(rule);
    }

    @Transactional(readOnly = true)
    public ReorderRuleForm toForm(ReorderRule rule) {
        ReorderRuleForm form = new ReorderRuleForm();
        form.setProductId(rule.getProduct().getId());
        form.setMinimumLevel(rule.getMinimumLevel());
        form.setMaximumLevel(rule.getMaximumLevel());
        form.setReorderQuantity(rule.getReorderQuantity());
        form.setActive(rule.isActive());
        return form;
    }

    @Transactional(readOnly = true)
    public List<ReorderAlert> calculateAlerts() {
        Map<Long, Long> quantities = inventoryBalanceRepository.findAllWithDetails().stream()
                .collect(Collectors.groupingBy(
                        balance -> balance.getProduct().getId(),
                        Collectors.summingLong(InventoryBalance::getQuantityAvailable)
                ));

        return reorderRuleRepository.findAllActiveWithProduct().stream()
                .map(rule -> toAlert(rule, quantities.getOrDefault(rule.getProduct().getId(), 0L)))
                .sorted((first, second) -> Integer.compare(priority(first), priority(second)))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, ReorderAlert> calculateAlertMap() {
        return calculateAlerts().stream()
                .collect(Collectors.toMap(alert -> alert.rule().getId(), Function.identity()));
    }

    private ReorderAlert toAlert(ReorderRule rule, long quantity) {
        String status;
        if (quantity == 0) {
            status = "OUT_OF_STOCK";
        } else if (quantity <= rule.getMinimumLevel()) {
            status = "LOW_STOCK";
        } else if (quantity > rule.getMaximumLevel()) {
            status = "OVERSTOCK";
        } else {
            status = "NORMAL";
        }
        int shortage = Math.max(0, rule.getMaximumLevel() - (int) quantity);
        return new ReorderAlert(rule, quantity, status, shortage);
    }

    private int priority(ReorderAlert alert) {
        return switch (alert.levelStatus()) {
            case "OUT_OF_STOCK" -> 0;
            case "LOW_STOCK" -> 1;
            case "OVERSTOCK" -> 2;
            default -> 3;
        };
    }

    private void validateLevels(ReorderRuleForm form) {
        if (form.getMinimumLevel() != null && form.getMaximumLevel() != null
                && form.getMaximumLevel() <= form.getMinimumLevel()) {
            throw new IllegalArgumentException("Maximum level must be greater than minimum level");
        }
    }

    private void applyForm(ReorderRule rule, ReorderRuleForm form) {
        Product product = productRepository.findById(form.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        if (!product.isActive()) {
            throw new IllegalArgumentException("Only active products can have reorder rules");
        }
        rule.setProduct(product);
        rule.setMinimumLevel(form.getMinimumLevel());
        rule.setMaximumLevel(form.getMaximumLevel());
        rule.setReorderQuantity(form.getReorderQuantity());
        rule.setActive(form.isActive());
    }
}
