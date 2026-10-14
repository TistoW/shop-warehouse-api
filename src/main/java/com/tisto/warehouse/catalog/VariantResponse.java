package com.tisto.warehouse.catalog;

import java.math.BigDecimal;
import java.time.Instant;

public record VariantResponse(
		Long id,
		Long itemId,
		String sku,
		String name,
		BigDecimal price,
		int stockQuantity,
		boolean active,
		Instant createdAt,
		Instant updatedAt) {

	static VariantResponse from(Variant v) {
		return new VariantResponse(v.getId(), v.getItem().getId(), v.getSku(), v.getName(), v.getPrice(),
				v.getStockQuantity(), v.isActive(), v.getCreatedAt(), v.getUpdatedAt());
	}
}
