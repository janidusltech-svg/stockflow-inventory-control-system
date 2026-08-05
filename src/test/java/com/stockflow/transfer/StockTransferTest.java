package com.stockflow.transfer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StockTransferTest {

    @Test
    void calculatesTotalQuantityAndWorkflowPermissions() {
        StockTransfer transfer = new StockTransfer();

        StockTransferItem first = new StockTransferItem();
        first.setStockTransfer(transfer);
        first.setQuantity(4);
        transfer.getItems().add(first);

        StockTransferItem second = new StockTransferItem();
        second.setStockTransfer(transfer);
        second.setQuantity(3);
        transfer.getItems().add(second);

        assertThat(transfer.getTotalQuantity()).isEqualTo(7);
        assertThat(transfer.isEditable()).isTrue();
        assertThat(transfer.canApprove()).isTrue();

        transfer.setStatus(StockTransferStatus.APPROVED);
        assertThat(transfer.canDispatch()).isTrue();
        assertThat(transfer.isEditable()).isFalse();

        transfer.setStatus(StockTransferStatus.DISPATCHED);
        assertThat(transfer.canReceive()).isTrue();
        assertThat(transfer.canCancel()).isFalse();
    }
}
