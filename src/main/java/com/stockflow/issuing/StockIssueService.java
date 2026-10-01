package com.stockflow.issuing;

import com.stockflow.inventory.InventoryBalance;
import com.stockflow.inventory.InventoryBalanceRepository;
import com.stockflow.inventory.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class StockIssueService {

    private final StockIssueRepository stockIssueRepository;
    private final RecipientRepository recipientRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final InventoryService inventoryService;

    public StockIssueService(StockIssueRepository stockIssueRepository,
                             RecipientRepository recipientRepository,
                             InventoryBalanceRepository inventoryBalanceRepository,
                             InventoryService inventoryService) {
        this.stockIssueRepository = stockIssueRepository;
        this.recipientRepository = recipientRepository;
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional(readOnly = true)
    public List<StockIssue> findAll(String keyword, StockIssueStatus status,
                                    LocalDate fromDate, LocalDate toDate) {
        String key = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return stockIssueRepository.findAllWithDetails().stream()
                .filter(issue -> key.isBlank()
                        || issue.getIssueNumber().toLowerCase(Locale.ROOT).contains(key)
                        || issue.getRecipient().getName().toLowerCase(Locale.ROOT).contains(key)
                        || contains(issue.getExternalReference(), key))
                .filter(issue -> status == null || issue.getStatus() == status)
                .filter(issue -> fromDate == null || !issue.getIssueDate().isBefore(fromDate))
                .filter(issue -> toDate == null || !issue.getIssueDate().isAfter(toDate))
                .toList();
    }

    @Transactional(readOnly = true)
    public StockIssue findById(Long id) {
        return stockIssueRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock issue not found"));
    }

    @Transactional(readOnly = true)
    public List<Recipient> findActiveRecipients() {
        return recipientRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<InventoryBalance> findAvailableStock() {
        return inventoryBalanceRepository.findAllAvailableWithDetails();
    }

    public StockIssue create(StockIssueForm form) {
        Recipient recipient = requireActiveRecipient(form.getRecipientId());
        StockIssue issue = new StockIssue();
        issue.setIssueNumber(generateIssueNumber());
        applyForm(issue, form, recipient);
        return stockIssueRepository.save(issue);
    }

    public StockIssue update(Long id, StockIssueForm form) {
        StockIssue issue = findById(id);
        requireDraft(issue);
        Recipient recipient = requireActiveRecipient(form.getRecipientId());
        applyForm(issue, form, recipient);
        return stockIssueRepository.save(issue);
    }

    public void addItem(Long issueId, StockIssueItemForm form) {
        StockIssue issue = findById(issueId);
        requireDraft(issue);
        InventoryBalance balance = inventoryBalanceRepository
                .findByIdWithDetails(form.getInventoryBalanceId())
                .orElseThrow(() -> new IllegalArgumentException("Selected inventory balance was not found"));
        int quantity = form.getQuantity() == null ? 0 : form.getQuantity();
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        if (quantity > balance.getQuantityAvailable()) {
            throw new IllegalArgumentException("Only " + balance.getQuantityAvailable()
                    + " units are available for the selected stock");
        }
        boolean duplicate = issue.getItems().stream().anyMatch(item ->
                item.getProduct().getId().equals(balance.getProduct().getId())
                        && item.getLocation().getId().equals(balance.getLocation().getId())
                        && sameId(item.getBatch() == null ? null : item.getBatch().getId(),
                        balance.getBatch() == null ? null : balance.getBatch().getId()));
        if (duplicate) {
            throw new IllegalArgumentException("This product, location and batch is already in the issue");
        }
        StockIssueItem item = new StockIssueItem();
        item.setStockIssue(issue);
        item.setProduct(balance.getProduct());
        item.setLocation(balance.getLocation());
        item.setBatch(balance.getBatch());
        item.setQuantity(quantity);
        issue.getItems().add(item);
        stockIssueRepository.save(issue);
    }

    public void removeItem(Long issueId, Long itemId) {
        StockIssue issue = findById(issueId);
        requireDraft(issue);
        boolean removed = issue.getItems().removeIf(item -> item.getId().equals(itemId));
        if (!removed) {
            throw new IllegalArgumentException("Stock-issue item not found");
        }
        stockIssueRepository.save(issue);
    }

    public void confirm(Long issueId) {
        StockIssue issue = findById(issueId);
        requireDraft(issue);
        if (issue.getItems().isEmpty()) {
            throw new IllegalArgumentException("Add at least one product before confirmation");
        }
        for (StockIssueItem item : issue.getItems()) {
            inventoryService.issue(item.getProduct(), item.getLocation(), item.getBatch(),
                    item.getQuantity(), issue.getIssueNumber());
        }
        issue.setStatus(StockIssueStatus.CONFIRMED);
        issue.setConfirmedAt(LocalDateTime.now());
        stockIssueRepository.save(issue);
    }

    public void cancelDraft(Long id) {
        StockIssue issue = findById(id);
        requireDraft(issue);
        issue.setStatus(StockIssueStatus.CANCELLED);
        stockIssueRepository.save(issue);
    }

    public void deleteDraft(Long id) {
        StockIssue issue = findById(id);
        requireDraft(issue);
        if (stockIssueRepository.countReturnsByStockIssueId(id) > 0) {
            throw new IllegalArgumentException("This stock issue has returns and cannot be deleted");
        }
        // Only the draft's own item rows are removed with it; no stock was deducted yet.
        stockIssueRepository.delete(issue);
    }

    public StockIssueForm toForm(StockIssue issue) {
        StockIssueForm form = new StockIssueForm();
        form.setRecipientId(issue.getRecipient().getId());
        form.setIssueDate(issue.getIssueDate());
        form.setExternalReference(issue.getExternalReference());
        form.setNotes(issue.getNotes());
        return form;
    }

    private Recipient requireActiveRecipient(Long id) {
        Recipient recipient = recipientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));
        if (!recipient.isActive()) {
            throw new IllegalArgumentException("Select an active recipient");
        }
        return recipient;
    }

    private void applyForm(StockIssue issue, StockIssueForm form, Recipient recipient) {
        if (form.getIssueDate() != null && form.getIssueDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Issue date cannot be in the future");
        }
        issue.setRecipient(recipient);
        issue.setIssueDate(form.getIssueDate());
        issue.setExternalReference(form.getExternalReference());
        issue.setNotes(form.getNotes());
    }

    private String generateIssueNumber() {
        long next = stockIssueRepository.findTopByOrderByIdDesc()
                .map(issue -> issue.getId() + 1).orElse(1L);
        String number;
        do {
            number = "ISS-" + LocalDate.now().getYear() + "-" + String.format("%05d", next++);
        } while (stockIssueRepository.existsByIssueNumberIgnoreCase(number));
        return number;
    }

    private void requireDraft(StockIssue issue) {
        if (!issue.isEditable()) {
            throw new IllegalArgumentException("Only draft stock issues can be changed");
        }
    }

    private boolean contains(String value, String key) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(key);
    }

    private boolean sameId(Long first, Long second) {
        return first == null ? second == null : first.equals(second);
    }
}
