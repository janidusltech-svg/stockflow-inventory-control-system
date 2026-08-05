package com.stockflow.issuing;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class RecipientService {

    private final RecipientRepository recipientRepository;

    public RecipientService(RecipientRepository recipientRepository) {
        this.recipientRepository = recipientRepository;
    }

    @Transactional(readOnly = true)
    public List<Recipient> findAll(String keyword, Boolean active) {
        String cleanedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return recipientRepository.findAll().stream()
                .filter(recipient -> cleanedKeyword.isBlank()
                        || contains(recipient.getCode(), cleanedKeyword)
                        || contains(recipient.getName(), cleanedKeyword)
                        || contains(recipient.getContactName(), cleanedKeyword)
                        || contains(recipient.getPhone(), cleanedKeyword))
                .filter(recipient -> active == null || recipient.isActive() == active)
                .sorted((first, second) -> first.getName().compareToIgnoreCase(second.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Recipient findById(Long id) {
        return recipientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));
    }

    public Recipient create(RecipientForm form) {
        String code = normalizeCode(form.getCode());
        if (recipientRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Recipient code already exists");
        }

        Recipient recipient = new Recipient();
        applyForm(recipient, form);
        return recipientRepository.save(recipient);
    }

    public Recipient update(Long id, RecipientForm form) {
        Recipient recipient = findById(id);
        String code = normalizeCode(form.getCode());
        if (recipientRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new IllegalArgumentException("Recipient code already exists");
        }
        applyForm(recipient, form);
        return recipientRepository.save(recipient);
    }

    public void setActive(Long id, boolean active) {
        Recipient recipient = findById(id);
        recipient.setActive(active);
        recipientRepository.save(recipient);
    }

    public RecipientForm toForm(Recipient recipient) {
        RecipientForm form = new RecipientForm();
        form.setCode(recipient.getCode());
        form.setName(recipient.getName());
        form.setRecipientType(recipient.getRecipientType());
        form.setContactName(recipient.getContactName());
        form.setPhone(recipient.getPhone());
        form.setEmail(recipient.getEmail());
        form.setAddress(recipient.getAddress());
        form.setActive(recipient.isActive());
        return form;
    }

    private void applyForm(Recipient recipient, RecipientForm form) {
        recipient.setCode(normalizeCode(form.getCode()));
        recipient.setName(form.getName());
        recipient.setRecipientType(form.getRecipientType());
        recipient.setContactName(form.getContactName());
        recipient.setPhone(form.getPhone());
        recipient.setEmail(form.getEmail());
        recipient.setAddress(form.getAddress());
        recipient.setActive(form.isActive());
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }
}
