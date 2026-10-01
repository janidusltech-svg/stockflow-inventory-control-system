package com.stockflow.product;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final UnitRepository unitRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            BrandRepository brandRepository,
            UnitRepository unitRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.unitRepository = unitRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> findAll(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return productRepository.findAllWithDetails();
        }
        return productRepository.search(keyword.trim());
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findByIdWithDetails(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Product not found")
                );
    }

    @Transactional(readOnly = true)
    public ProductForm getForm(Long id) {
        return ProductForm.from(findById(id));
    }

    @Transactional(readOnly = true)
    public List<Category> findActiveCategories() {
        return categoryRepository.findAllByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Brand> findActiveBrands() {
        return brandRepository.findAllByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Unit> findActiveUnits() {
        return unitRepository.findAllByActiveTrueOrderByNameAsc();
    }

    public Product create(ProductForm form) {
        validateUniqueValues(form, null);
        Product product = new Product();
        applyForm(product, form);
        return productRepository.save(product);
    }

    public Product update(Long id, ProductForm form) {
        Product product = findById(id);
        validateUniqueValues(form, id);
        applyForm(product, form);
        return productRepository.save(product);
    }

    public void deactivate(Long id) {
        Product product = findById(id);
        product.setActive(false);
        productRepository.save(product);
    }

    /**
     * Permanently removes a product that was never used anywhere else.
     * Used products must be deactivated instead so their history stays valid.
     */
    public void delete(Long id) {
        Product product = findById(id);
        List<String> usedIn = findDeleteBlockers(id);
        if (!usedIn.isEmpty()) {
            throw new IllegalArgumentException(
                    "Product " + product.getSku() + " cannot be deleted because it is used in "
                            + String.join(", ", usedIn) + ". Deactivate it instead."
            );
        }
        productRepository.delete(product);
        productRepository.flush();
    }

    @Transactional(readOnly = true)
    public List<String> findDeleteBlockers(Long id) {
        return usageByRecordType().entrySet().stream()
                .filter(entry -> entry.getValue().contains(id))
                .map(Map.Entry::getKey)
                .toList();
    }

    @Transactional(readOnly = true)
    public Set<Long> findLinkedProductIds() {
        Set<Long> linked = new HashSet<>();
        usageByRecordType().values().forEach(linked::addAll);
        return linked;
    }

    private Map<String, List<Long>> usageByRecordType() {
        Map<String, List<Long>> usage = new LinkedHashMap<>();
        usage.put("purchase orders", productRepository.findProductIdsUsedInPurchaseOrders());
        usage.put("goods receipts", productRepository.findProductIdsUsedInGoodsReceipts());
        usage.put("batches", productRepository.findProductIdsUsedInBatches());
        usage.put("inventory balances", productRepository.findProductIdsUsedInInventoryBalances());
        usage.put("stock movements", productRepository.findProductIdsUsedInStockMovements());
        usage.put("stock transfers", productRepository.findProductIdsUsedInStockTransfers());
        usage.put("stock issues", productRepository.findProductIdsUsedInStockIssues());
        usage.put("reorder rules", productRepository.findProductIdsUsedInReorderRules());
        return usage;
    }

    private void applyForm(Product product, ProductForm form) {
        Category category = categoryRepository.findById(form.getCategoryId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid category")
                );

        Unit unit = unitRepository.findById(form.getUnitId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid unit")
                );

        Brand brand = null;
        if (form.getBrandId() != null) {
            brand = brandRepository.findById(form.getBrandId())
                    .orElseThrow(() ->
                            new IllegalArgumentException("Invalid brand")
                    );
        }

        product.setSku(normalizeRequired(form.getSku()).toUpperCase());
        product.setBarcode(normalizeOptional(form.getBarcode()));
        product.setName(normalizeRequired(form.getName()));
        product.setCategory(category);
        product.setBrand(brand);
        product.setUnit(unit);
        product.setDescription(normalizeOptional(form.getDescription()));
        product.setBatchTracked(form.isBatchTracked());
        product.setActive(form.isActive());
    }

    private void validateUniqueValues(ProductForm form, Long currentId) {
        String sku = normalizeRequired(form.getSku()).toUpperCase();
        productRepository.findBySkuIgnoreCase(sku)
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "A product with this SKU already exists"
                    );
                });

        String barcode = normalizeOptional(form.getBarcode());
        if (barcode != null) {
            productRepository.findByBarcode(barcode)
                    .filter(existing -> !existing.getId().equals(currentId))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException(
                                "A product with this barcode already exists"
                        );
                    });
        }
    }

    private String normalizeRequired(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
