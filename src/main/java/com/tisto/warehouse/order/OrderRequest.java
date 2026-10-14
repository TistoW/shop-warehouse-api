package com.tisto.warehouse.order;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record OrderRequest(@NotEmpty @Valid List<Line> items) {

	public record Line(@NotNull Long variantId, @Min(1) int quantity) {
	}
}
