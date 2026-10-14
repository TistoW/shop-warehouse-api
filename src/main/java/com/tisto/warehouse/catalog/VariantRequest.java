package com.tisto.warehouse.catalog;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VariantRequest(
		@NotBlank @Size(max = 64) String sku,
		@NotBlank @Size(max = 200) String name,
		@NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2) BigDecimal price) {
}
