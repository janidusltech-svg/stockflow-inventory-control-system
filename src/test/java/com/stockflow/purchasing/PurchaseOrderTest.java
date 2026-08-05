package com.stockflow.purchasing;

import com.stockflow.product.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PurchaseOrderTest {

    @Test
    void calculatesOrderTotalFromAllProductLines() {
        PurchaseOrder order = new PurchaseOrder();
        order.getItems().add(item("Rice 5kg", 2, "2500.00"));
        order.getItems().add(item("Milk Powder 400g", 3, "1250.50"));

        assertThat(order.getTotalAmount()).isEqualByComparingTo("8751.50");
    }

    @Test
    void onlyDraftOrdersAreEditable() {
        PurchaseOrder order = new PurchaseOrder();

        assertThat(order.isEditable()).isTrue();

        order.setStatus(PurchaseOrderStatus.APPROVED);

        assertThat(order.isEditable()).isFalse();
    }

    private PurchaseOrderItem item(String productName, int quantity, String unitPrice) {
        Product product = new Product();
        product.setName(productName);

        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal(unitPrice));
        return item;
    }
}
