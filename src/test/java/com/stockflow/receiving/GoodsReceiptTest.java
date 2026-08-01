package com.stockflow.receiving;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GoodsReceiptTest {

    @Test
    void calculatesDeliveredAcceptedAndRejectedTotals() {
        GoodsReceipt receipt = new GoodsReceipt();
        receipt.getItems().add(item(10, 8, 2));
        receipt.getItems().add(item(5, 5, 0));

        assertThat(receipt.getTotalDeliveredQuantity()).isEqualTo(15);
        assertThat(receipt.getTotalAcceptedQuantity()).isEqualTo(13);
        assertThat(receipt.getTotalRejectedQuantity()).isEqualTo(2);
    }

    @Test
    void onlyDraftReceiptsAreEditable() {
        GoodsReceipt receipt = new GoodsReceipt();
        assertThat(receipt.isEditable()).isTrue();

        receipt.setStatus(GoodsReceiptStatus.CONFIRMED);
        assertThat(receipt.isEditable()).isFalse();
    }

    private GoodsReceiptItem item(int delivered, int accepted, int rejected) {
        GoodsReceiptItem item = new GoodsReceiptItem();
        item.setDeliveredQuantity(delivered);
        item.setAcceptedQuantity(accepted);
        item.setRejectedQuantity(rejected);
        return item;
    }
}
