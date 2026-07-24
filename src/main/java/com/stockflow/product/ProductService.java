package com.stockflow.product;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> findAll(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return productRepository.findAll();
        }
        String cleanedKeyword = keyword.trim();
        return productRepository.findBySkuContainingIgnoreCaseOrNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(
                cleanedKeyword,
                cleanedKeyword,
                cleanedKeyword
        );
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }

    public Product create(Product product) {
        if (productRepository.existsBySkuIgnoreCase(product.getSku())) {
            throw new IllegalArgumentException("A product with this SKU already exists");
        }
        return productRepository.save(product);
    }

    public Product update(Long id, Product formProduct) {
        Product product = findById(id);
        productRepository.findBySkuIgnoreCase(formProduct.getSku())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("A product with this SKU already exists");
                });

        product.setSku(formProduct.getSku());
        product.setName(formProduct.getName());
        product.setCategory(formProduct.getCategory());
        product.setBrand(formProduct.getBrand());
        product.setUnit(formProduct.getUnit());
        product.setDescription(formProduct.getDescription());
        product.setActive(formProduct.isActive());
        return productRepository.save(product);
    }

    public void deactivate(Long id) {
        Product product = findById(id);
        product.setActive(false);
        productRepository.save(product);
    }
}
