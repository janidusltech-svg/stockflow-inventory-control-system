package com.stockflow.issuing;

import com.stockflow.inventory.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@Transactional
public class StockReturnService {

    private final StockReturnRepository stockReturnRepository;
    private final StockIssueRepository stockIssueRepository;
    private final InventoryService inventoryService;

    public StockReturnService(StockReturnRepository stockReturnRepository,
                              StockIssueRepository stockIssueRepository,
                              InventoryService inventoryService) {
        this.stockReturnRepository = stockReturnRepository;
        this.stockIssueRepository = stockIssueRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional(readOnly = true)
    public List<StockReturn> findAll(String keyword, StockReturnStatus status,
                                     LocalDate fromDate, LocalDate toDate) {
        String key = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return stockReturnRepository.findAllWithDetails().stream()
                .filter(returned -> key.isBlank()
                        || returned.getReturnNumber().toLowerCase(Locale.ROOT).contains(key)
                        || returned.getStockIssue().getIssueNumber().toLowerCase(Locale.ROOT).contains(key)
                        || returned.getStockIssue().getRecipient().getName()
                        .toLowerCase(Locale.ROOT).contains(key))
                .filter(returned -> status == null || returned.getStatus() == status)
                .filter(returned -> fromDate == null || !returned.getReturnDate().isBefore(fromDate))
                .filter(returned -> toDate == null || !returned.getReturnDate().isAfter(toDate))
                .toList();
    }

    @Transactional(readOnly = true)
    public StockReturn findById(Long id) {
        return stockReturnRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock return not found"));
    }

    @Transactional(readOnly = true)
    public StockIssue findSourceIssue(Long id) {
        return stockIssueRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Related stock issue not found"));
    }

    @Transactional(readOnly = true)
    public List<StockIssue> findReturnableIssues() {
        return stockIssueRepository.findAllWithDetails().stream()
                .filter(StockIssue::isReturnable)
                .filter(issue -> issue.getItems().stream()
                        .anyMatch(item -> item.getRemainingReturnableQuantity() > 0))
                .toList();
    }

    public StockReturn create(StockReturnForm form) {
        StockIssue issue = requireReturnableIssue(form.getStockIssueId());
        if (stockReturnRepository.existsByStockIssue_IdAndStatus(
                issue.getId(), StockReturnStatus.DRAFT)) {
            throw new IllegalArgumentException("Complete or cancel the existing draft return for this issue");
        }
        validateReturnDate(form.getReturnDate(), issue);
        StockReturn returned = new StockReturn();
        returned.setReturnNumber(generateReturnNumber());
        applyForm(returned, form, issue);
        return stockReturnRepository.save(returned);
    }

    public StockReturn update(Long id, StockReturnForm form) {
        StockReturn returned = findById(id);
        requireDraft(returned);
        StockIssue issue = requireReturnableIssue(form.getStockIssueId());
        if (!returned.getItems().isEmpty()
                && !returned.getStockIssue().getId().equals(issue.getId())) {
            throw new IllegalArgumentException("Remove all return items before changing the stock issue");
        }
        validateReturnDate(form.getReturnDate(), issue);
        applyForm(returned, form, issue);
        return stockReturnRepository.save(returned);
    }

    public void addItem(Long returnId, StockReturnItemForm form) {
        StockReturn returned = findById(returnId);
        requireDraft(returned);
        StockIssue issue = stockIssueRepository
                .findByIdWithDetails(returned.getStockIssue().getId())
                .orElseThrow(() -> new IllegalArgumentException("Related stock issue not found"));
        StockIssueItem issueItem = issue.getItems().stream()
                .filter(item -> item.getId().equals(form.getIssueItemId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Select a product from the related stock issue"));
        boolean duplicate = returned.getItems().stream()
                .anyMatch(item -> item.getIssueItem().getId().equals(issueItem.getId()));
        if (duplicate) {
            throw new IllegalArgumentException("This issued product is already in the return");
        }
        validateItem(form, issueItem);
        StockReturnItem item = new StockReturnItem();
        item.setStockReturn(returned);
        item.setIssueItem(issueItem);
        item.setReturnedQuantity(form.getReturnedQuantity());
        item.setRestockQuantity(form.getRestockQuantity());
        item.setRejectedQuantity(form.getRejectedQuantity());
        item.setReturnReason(form.getReturnReason());
        item.setConditionNote(form.getConditionNote());
        returned.getItems().add(item);
        stockReturnRepository.save(returned);
    }

    public void removeItem(Long returnId, Long itemId) {
        StockReturn returned = findById(returnId);
        requireDraft(returned);
        boolean removed = returned.getItems().removeIf(item -> item.getId().equals(itemId));
        if (!removed) {
            throw new IllegalArgumentException("Stock-return item not found");
        }
        stockReturnRepository.save(returned);
    }

    public void confirm(Long returnId) {
        StockReturn returned = findById(returnId);
        requireDraft(returned);
        if (returned.getItems().isEmpty()) {
            throw new IllegalArgumentException("Add at least one returned product before confirmation");
        }
        StockIssue issue = stockIssueRepository
                .findByIdWithDetails(returned.getStockIssue().getId())
                .orElseThrow(() -> new IllegalArgumentException("Related stock issue not found"));
        for (StockReturnItem returnItem : returned.getItems()) {
            StockIssueItem issueItem = issue.getItems().stream()
                    .filter(item -> Objects.equals(item.getId(), returnItem.getIssueItem().getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "A return item no longer belongs to this stock issue"));
            validateStoredItem(returnItem, issueItem);
            issueItem.addReturnedQuantity(returnItem.getReturnedQuantity());
            if (returnItem.getRestockQuantity() > 0) {
                inventoryService.returnToStock(issueItem.getProduct(), issueItem.getLocation(),
                        issueItem.getBatch(), returnItem.getRestockQuantity(),
                        returned.getReturnNumber());
            }
        }
        updateIssueStatus(issue);
        stockIssueRepository.save(issue);
        returned.setStatus(StockReturnStatus.CONFIRMED);
        returned.setConfirmedAt(LocalDateTime.now());
        stockReturnRepository.save(returned);
    }

    public void cancelDraft(Long id) {
        StockReturn returned = findById(id);
        requireDraft(returned);
        returned.setStatus(StockReturnStatus.CANCELLED);
        stockReturnRepository.save(returned);
    }

    public void deleteDraft(Long id) {
        StockReturn returned = findById(id);
        requireDraft(returned);
        stockReturnRepository.delete(returned);
    }

    public StockReturnForm toForm(StockReturn returned) {
        StockReturnForm form = new StockReturnForm();
        form.setStockIssueId(returned.getStockIssue().getId());
        form.setReturnDate(returned.getReturnDate());
        form.setNotes(returned.getNotes());
        return form;
    }

    private void applyForm(StockReturn returned, StockReturnForm form, StockIssue issue) {
        returned.setStockIssue(issue);
        returned.setReturnDate(form.getReturnDate());
        returned.setNotes(form.getNotes());
    }

    private void validateItem(StockReturnItemForm form, StockIssueItem issueItem) {
        int returned = form.getReturnedQuantity() == null ? 0 : form.getReturnedQuantity();
        int restock = form.getRestockQuantity() == null ? 0 : form.getRestockQuantity();
        int rejected = form.getRejectedQuantity() == null ? 0 : form.getRejectedQuantity();
        if (returned <= 0) {
            throw new IllegalArgumentException("Returned quantity must be greater than zero");
        }
        if (restock < 0 || rejected < 0 || restock + rejected != returned) {
            throw new IllegalArgumentException(
                    "Restock quantity plus rejected quantity must equal returned quantity");
        }
        if (returned > issueItem.getRemainingReturnableQuantity()) {
            throw new IllegalArgumentException("Only "
                    + issueItem.getRemainingReturnableQuantity()
                    + " units remain returnable for this issued product");
        }
        if (form.getReturnReason() == null || form.getReturnReason().isBlank()) {
            throw new IllegalArgumentException("Return reason is required");
        }
        if (rejected > 0 && (form.getConditionNote() == null
                || form.getConditionNote().isBlank())) {
            throw new IllegalArgumentException("Describe the condition of rejected products");
        }
    }

    private void validateStoredItem(StockReturnItem item, StockIssueItem issueItem) {
        StockReturnItemForm form = new StockReturnItemForm();
        form.setReturnedQuantity(item.getReturnedQuantity());
        form.setRestockQuantity(item.getRestockQuantity());
        form.setRejectedQuantity(item.getRejectedQuantity());
        form.setReturnReason(item.getReturnReason());
        form.setConditionNote(item.getConditionNote());
        validateItem(form, issueItem);
    }

    private StockIssue requireReturnableIssue(Long id) {
        StockIssue issue = stockIssueRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock issue not found"));
        if (!issue.isReturnable()) {
            throw new IllegalArgumentException("Select a confirmed issue with returnable products");
        }
        return issue;
    }

    private void validateReturnDate(LocalDate returnDate, StockIssue issue) {
        if (returnDate != null && returnDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Return date cannot be in the future");
        }
        if (returnDate != null && issue.getIssueDate() != null
                && returnDate.isBefore(issue.getIssueDate())) {
            throw new IllegalArgumentException("Return date cannot be before the issue date");
        }
    }

    private void updateIssueStatus(StockIssue issue) {
        boolean allReturned = issue.getItems().stream()
                .allMatch(item -> item.getRemainingReturnableQuantity() == 0);
        boolean anyReturned = issue.getItems().stream()
                .anyMatch(item -> item.getReturnedQuantity() > 0);
        issue.setStatus(allReturned ? StockIssueStatus.RETURNED
                : anyReturned ? StockIssueStatus.PARTIALLY_RETURNED
                  : StockIssueStatus.CONFIRMED);
    }

    private String generateReturnNumber() {
        long next = stockReturnRepository.findTopByOrderByIdDesc()
                .map(returned -> returned.getId() + 1).orElse(1L);
        String number;
        do {
            number = "RTN-" + LocalDate.now().getYear() + "-" + String.format("%05d", next++);
        } while (stockReturnRepository.existsByReturnNumberIgnoreCase(number));
        return number;
    }

    private void requireDraft(StockReturn returned) {
        if (!returned.isEditable()) {
            throw new IllegalArgumentException("Only draft stock returns can be changed");
        }
    }
}
