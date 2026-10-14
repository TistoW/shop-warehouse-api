package com.tisto.warehouse.catalog;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/variants")
public class VariantController {

	private final VariantService variants;

	public VariantController(VariantService variants) {
		this.variants = variants;
	}

	@GetMapping("/{id}")
	VariantResponse get(@PathVariable Long id) {
		return variants.get(id);
	}

	@PutMapping("/{id}")
	VariantResponse update(@PathVariable Long id, @Valid @RequestBody VariantRequest request) {
		return variants.update(id, request);
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> archive(@PathVariable Long id) {
		variants.archive(id);
		return ResponseEntity.noContent().build();
	}
}
