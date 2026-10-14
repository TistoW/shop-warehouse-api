package com.tisto.warehouse.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateItemRequest(
		@NotBlank @Size(max = 200) String name,
		@Size(max = 2000) String description) {
}
