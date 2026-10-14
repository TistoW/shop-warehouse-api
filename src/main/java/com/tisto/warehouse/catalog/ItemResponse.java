package com.tisto.warehouse.catalog;

import java.time.Instant;
import java.util.List;

public record ItemResponse(
		Long id,
		String name,
		String description,
		boolean active,
		int totalStock,
		List<VariantResponse> variants,
		Instant createdAt,
		Instant updatedAt) {

	static ItemResponse from(Item item) {
		List<VariantResponse> variants = item.getVariants().stream().map(VariantResponse::from).toList();
		int totalStock = variants.stream().filter(VariantResponse::active).mapToInt(VariantResponse::stockQuantity).sum();
		return new ItemResponse(item.getId(), item.getName(), item.getDescription(), item.isActive(), totalStock,
				variants, item.getCreatedAt(), item.getUpdatedAt());
	}
}
