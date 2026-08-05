package com.stockflow.product;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferenceDataServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ReferenceDataService referenceDataService;

    @Test
    void categoryCreationNormalizesCodeAndPreservesDescription() {
        ReferenceDataForm form = form(" food-dry ", " Dry Food ", " Packaged dry foods ");
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Category saved = referenceDataService.createCategory(form);

        assertThat(saved.getCode()).isEqualTo("FOOD-DRY");
        assertThat(saved.getName()).isEqualTo("Dry Food");
        assertThat(saved.getDescription()).isEqualTo("Packaged dry foods");
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void duplicateCategoryCodeIsRejected() {
        ReferenceDataForm form = form("FOOD", "Another Food", null);
        when(categoryRepository.existsByCodeIgnoreCase("FOOD")).thenReturn(true);

        assertThatThrownBy(() -> referenceDataService.createCategory(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("code already exists");
    }

    @Test
    void brandCreationRejectsDuplicateNamesIgnoringCase() {
        ReferenceDataForm form = form(null, "Lanka Fresh", null);
        when(brandRepository.existsByNameIgnoreCase("Lanka Fresh")).thenReturn(true);

        assertThatThrownBy(() -> referenceDataService.createBrand(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("brand with this name");
    }

    @Test
    void unitCreationRejectsNamesLongerThanDatabaseColumn() {
        ReferenceDataForm form = form("LONG", "A".repeat(61), null);

        assertThatThrownBy(() -> referenceDataService.createUnit(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 2 and 60");
    }

    @Test
    void deactivationKeepsUsedCategoryRecordInsteadOfDeletingIt() {
        Category category = new Category();
        category.setCode("FOOD");
        category.setName("Food");
        category.setActive(true);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        referenceDataService.setCategoryActive(1L, false);

        assertThat(category.isActive()).isFalse();
        verify(categoryRepository).save(category);
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    private ReferenceDataForm form(String code, String name, String description) {
        ReferenceDataForm form = new ReferenceDataForm();
        form.setCode(code);
        form.setName(name);
        form.setDescription(description);
        form.setActive(true);
        return form;
    }
}
