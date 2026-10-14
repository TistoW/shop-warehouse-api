package com.tisto.warehouse.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
		Long id,
		String orderNumber,
		BigDecimal totalAmount,
		Instant createdAt,
		List<Line> items) {

	public record Line(Long variantId, String sku, String name, int quantity, BigDecimal unitPrice,
			BigDecimal lineTotal) {
	}

	static OrderResponse from(Order order) {
		List<Line> lines = order.getItems().stream()
				.map(i -> new Line(i.getVariant().getId(), i.getVariant().getSku(), i.getVariant().getName(),
						i.getQuantity(), i.getUnitPrice(), i.getLineTotal()))
				.toList();
		return new OrderResponse(order.getId(), order.getOrderNumber(), order.getTotalAmount(), order.getCreatedAt(),
				lines);
	}
}
