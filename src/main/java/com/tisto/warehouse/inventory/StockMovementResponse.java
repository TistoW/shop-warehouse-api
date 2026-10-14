package com.tisto.warehouse.inventory;

import java.time.Instant;

public record StockMovementResponse(
		Long id,
		Long variantId,
		int quantityChange,
		MovementReason reason,
		String reference,
		Instant createdAt) {

	static StockMovementResponse from(StockMovement m) {
		return new StockMovementResponse(m.getId(), m.getVariantId(), m.getQuantityChange(), m.getReason(),
				m.getReference(), m.getCreatedAt());
	}
}
