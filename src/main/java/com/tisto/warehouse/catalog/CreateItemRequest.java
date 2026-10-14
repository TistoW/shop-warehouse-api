package com.tisto.warehouse.catalog;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Either list the variants, or give sku and price to get a single "Default" variant.
public record CreateItemRequest(
		@NotBlank @Size(max = 200) String name,
		@Size(max = 2000) String description,
		@Size(max = 64) String sku,
		@DecimalMin("0.00") @Digits(integer = 13, fraction = 2) BigDecimal price,
		@Valid List<VariantRequest> variants) {
}
