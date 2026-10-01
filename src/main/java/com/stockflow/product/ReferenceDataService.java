package com.stockflow.product;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional
public class ReferenceDataService {

    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final UnitRepository unitRepository;
    private final ProductRepository productRepository;

    public ReferenceDataService(CategoryRepository categoryRepository,
                                BrandRepository brandRepository,
                                UnitRepository unitRepository,
                                ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.unitRepository = unitRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Category> findCategories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Brand> findBrands() {
        return brandRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Unit> findUnits() {
        return unitRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> categoryUsage() {
        Map<Long, Long> usage = new LinkedHashMap<>();
        findCategories().forEach(category ->
                usage.put(category.getId(), productRepository.countByCategoryId(category.getId())));
        return usage;
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> brandUsage() {
        Map<Long, Long> usage = new LinkedHashMap<>();
        findBrands().forEach(brand ->
                usage.put(brand.getId(), productRepository.countByBrandId(brand.getId())));
        return usage;
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> unitUsage() {
        Map<Long, Long> usage = new LinkedHashMap<>();
        findUnits().forEach(unit ->
                usage.put(unit.getId(), productRepository.countByUnitId(unit.getId())));
        return usage;
    }

    @Transactional(readOnly = true)
    public ReferenceDataForm getCategoryForm(Long id) {
        return ReferenceDataForm.from(findCategory(id));
    }

    @Transactional(readOnly = true)
    public ReferenceDataForm getBrandForm(Long id) {
        return ReferenceDataForm.from(findBrand(id));
    }

    @Transactional(readOnly = true)
    public ReferenceDataForm getUnitForm(Long id) {
        return ReferenceDataForm.from(findUnit(id));
    }

    public Category createCategory(ReferenceDataForm form) {
        validateCategory(form, null);
        Category category = new Category();
        applyCategory(category, form);
        return categoryRepository.save(category);
    }

    public Category updateCategory(Long id, ReferenceDataForm form) {
        Category category = findCategory(id);
        validateCategory(form, id);
        applyCategory(category, form);
        return categoryRepository.save(category);
    }

    public Brand createBrand(ReferenceDataForm form) {
        validateBrand(form, null);
        Brand brand = new Brand();
        applyBrand(brand, form);
        return brandRepository.save(brand);
    }

    public Brand updateBrand(Long id, ReferenceDataForm form) {
        Brand brand = findBrand(id);
        validateBrand(form, id);
        applyBrand(brand, form);
        return brandRepository.save(brand);
    }

    public Unit createUnit(ReferenceDataForm form) {
        validateUnit(form, null);
        Unit unit = new Unit();
        applyUnit(unit, form);
        return unitRepository.save(unit);
    }

    public Unit updateUnit(Long id, ReferenceDataForm form) {
        Unit unit = findUnit(id);
        validateUnit(form, id);
        applyUnit(unit, form);
        return unitRepository.save(unit);
    }

    public void setCategoryActive(Long id, boolean active) {
        Category category = findCategory(id);
        category.setActive(active);
        categoryRepository.save(category);
    }

    public void setBrandActive(Long id, boolean active) {
        Brand brand = findBrand(id);
        brand.setActive(active);
        brandRepository.save(brand);
    }

    public void setUnitActive(Long id, boolean active) {
        Unit unit = findUnit(id);
        unit.setActive(active);
        unitRepository.save(unit);
    }

    // Permanent delete is allowed only for values no product has ever used.
    // Used values must be deactivated so existing product links stay valid.

    public void deleteCategory(Long id) {
        Category category = findCategory(id);
        requireUnused(productRepository.countByCategoryId(id), "Category " + category.getName());
        categoryRepository.delete(category);
        categoryRepository.flush();
    }

    public void deleteBrand(Long id) {
        Brand brand = findBrand(id);
        requireUnused(productRepository.countByBrandId(id), "Brand " + brand.getName());
        brandRepository.delete(brand);
        brandRepository.flush();
    }

    public void deleteUnit(Long id) {
        Unit unit = findUnit(id);
        requireUnused(productRepository.countByUnitId(id), "Measurement unit " + unit.getName());
        unitRepository.delete(unit);
        unitRepository.flush();
    }

    private void requireUnused(long productCount, String label) {
        if (productCount > 0) {
            throw new IllegalArgumentException(label + " cannot be deleted because "
                    + productCount + " product(s) use it. Deactivate it instead.");
        }
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category was not found"));
    }

    private Brand findBrand(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Brand was not found"));
    }

    private Unit findUnit(Long id) {
        return unitRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Measurement unit was not found"));
    }

    private void validateCategory(ReferenceDataForm form, Long currentId) {
        String code = requiredCode(form.getCode(), "Category code is required", 30);
        String name = requiredName(form.getName(), 80);
        if (existsCategoryCode(code, currentId)) {
            throw new IllegalArgumentException("A category with this code already exists");
        }
        if (existsCategoryName(name, currentId)) {
            throw new IllegalArgumentException("A category with this name already exists");
        }
    }

    private void validateBrand(ReferenceDataForm form, Long currentId) {
        String name = requiredName(form.getName(), 80);
        boolean exists = currentId == null
                ? brandRepository.existsByNameIgnoreCase(name)
                : brandRepository.existsByNameIgnoreCaseAndIdNot(name, currentId);
        if (exists) {
            throw new IllegalArgumentException("A brand with this name already exists");
        }
    }

    private void validateUnit(ReferenceDataForm form, Long currentId) {
        String code = requiredCode(form.getCode(), "Unit code is required", 20);
        String name = requiredName(form.getName(), 60);
        boolean codeExists = currentId == null
                ? unitRepository.existsByCodeIgnoreCase(code)
                : unitRepository.existsByCodeIgnoreCaseAndIdNot(code, currentId);
        if (codeExists) {
            throw new IllegalArgumentException("A measurement unit with this code already exists");
        }
        boolean nameExists = currentId == null
                ? unitRepository.existsByNameIgnoreCase(name)
                : unitRepository.existsByNameIgnoreCaseAndIdNot(name, currentId);
        if (nameExists) {
            throw new IllegalArgumentException("A measurement unit with this name already exists");
        }
    }

    private boolean existsCategoryCode(String code, Long currentId) {
        return currentId == null
                ? categoryRepository.existsByCodeIgnoreCase(code)
                : categoryRepository.existsByCodeIgnoreCaseAndIdNot(code, currentId);
    }

    private boolean existsCategoryName(String name, Long currentId) {
        return currentId == null
                ? categoryRepository.existsByNameIgnoreCase(name)
                : categoryRepository.existsByNameIgnoreCaseAndIdNot(name, currentId);
    }

    private void applyCategory(Category category, ReferenceDataForm form) {
        category.setCode(normalizeCode(form.getCode()));
        category.setName(normalizeName(form.getName()));
        category.setDescription(normalizeOptional(form.getDescription()));
        category.setActive(form.isActive());
    }

    private void applyBrand(Brand brand, ReferenceDataForm form) {
        brand.setName(normalizeName(form.getName()));
        brand.setActive(form.isActive());
    }

    private void applyUnit(Unit unit, ReferenceDataForm form) {
        unit.setCode(normalizeCode(form.getCode()));
        unit.setName(normalizeName(form.getName()));
        unit.setActive(form.isActive());
    }

    private String requiredCode(String value, String message, int maxLength) {
        String normalized = normalizeCode(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException("Code is too long");
        }
        return normalized;
    }

    private String requiredName(String value, int maxLength) {
        String normalized = normalizeName(value);
        if (normalized.length() < 2 || normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    "Name must contain between 2 and " + maxLength + " characters");
        }
        return normalized;
    }

    private String normalizeCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
