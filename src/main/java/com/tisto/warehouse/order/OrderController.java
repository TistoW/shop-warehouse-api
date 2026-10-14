package com.tisto.warehouse.order;

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
@RequestMapping("/api/orders")
public class OrderController {

	private final OrderService orders;

	public OrderController(OrderService orders) {
		this.orders = orders;
	}

	@PostMapping
	ResponseEntity<OrderResponse> place(@Valid @RequestBody OrderRequest request) {
		OrderResponse placed = orders.place(request);
		return ResponseEntity.created(URI.create("/api/orders/" + placed.id())).body(placed);
	}

	@GetMapping
	Page<OrderResponse> list(Pageable pageable) {
		return orders.list(pageable);
	}

	@GetMapping("/{id}")
	OrderResponse get(@PathVariable Long id) {
		return orders.get(id);
	}
}
