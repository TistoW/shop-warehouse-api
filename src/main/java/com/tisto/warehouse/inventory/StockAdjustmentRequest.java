package com.tisto.warehouse.inventory;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// quantity is signed: positive adds stock, negative removes it (shrinkage, corrections).
public record StockAdjustmentRequest(
		@NotNull @Min(-100000) @Max(100000) Integer quantity,
		@NotNull MovementReason reason,
		@Size(max = 255) String note) {
}
