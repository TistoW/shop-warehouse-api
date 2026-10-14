package com.tisto.warehouse.order;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderRequest(@NotEmpty @Size(max = 100) @Valid List<Line> items) {

	public record Line(@NotNull Long variantId, @Min(1) @Max(100000) int quantity) {
	}
}
