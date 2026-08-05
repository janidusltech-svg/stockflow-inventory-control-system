package com.stockflow.issuing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecipientServiceTest {

    private RecipientRepository recipientRepository;
    private RecipientService recipientService;

    @BeforeEach
    void setUp() {
        recipientRepository = mock(RecipientRepository.class);
        recipientService = new RecipientService(recipientRepository);
    }

    @Test
    void createsActiveRecipientWithUniqueCode() {
        when(recipientRepository.existsByCodeIgnoreCase("CUS-010")).thenReturn(false);
        when(recipientRepository.save(any(Recipient.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Recipient created = recipientService.create(form("CUS-010"));

        assertThat(created.getCode()).isEqualTo("CUS-010");
        assertThat(created.getName()).isEqualTo("Green Mart");
        assertThat(created.isActive()).isTrue();
        verify(recipientRepository).save(created);
    }

    @Test
    void rejectsDuplicateRecipientCode() {
        when(recipientRepository.existsByCodeIgnoreCase("CUS-010")).thenReturn(true);

        assertThatThrownBy(() -> recipientService.create(form("CUS-010")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("code already exists");
    }

    @Test
    void deactivatesRecipientWithoutDeletingHistory() {
        Recipient recipient = new Recipient();
        recipient.setCode("CUS-010");
        recipient.setName("Green Mart");
        recipient.setRecipientType(RecipientType.CUSTOMER);
        recipient.setActive(true);
        when(recipientRepository.findById(1L)).thenReturn(Optional.of(recipient));

        recipientService.setActive(1L, false);

        assertThat(recipient.isActive()).isFalse();
        verify(recipientRepository).save(recipient);
    }

    private RecipientForm form(String code) {
        RecipientForm form = new RecipientForm();
        form.setCode(code);
        form.setName("Green Mart");
        form.setRecipientType(RecipientType.CUSTOMER);
        form.setActive(true);
        return form;
    }
}
