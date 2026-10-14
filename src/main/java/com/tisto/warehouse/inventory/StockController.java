package com.tisto.warehouse.inventory;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/variants/{variantId}")
public class StockController {

	private final StockService stock;

	public StockController(StockService stock) {
		this.stock = stock;
	}

	@PostMapping("/stock-adjustments")
	ResponseEntity<StockMovementResponse> adjust(@PathVariable Long variantId,
			@Valid @RequestBody StockAdjustmentRequest request) {
		StockMovementResponse movement = stock.adjust(variantId, request);
		return ResponseEntity.created(URI.create("/api/variants/" + variantId + "/stock-movements")).body(movement);
	}

	@GetMapping("/stock-movements")
	Page<StockMovementResponse> movements(@PathVariable Long variantId, Pageable pageable) {
		return stock.movements(variantId, pageable);
	}
}
