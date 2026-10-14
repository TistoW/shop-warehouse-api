package com.tisto.warehouse.catalog;

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/items")
public class ItemController {

	private final ItemService items;
	private final VariantService variants;

	public ItemController(ItemService items, VariantService variants) {
		this.items = items;
		this.variants = variants;
	}

	@GetMapping
	Page<ItemResponse> list(@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "false") boolean includeArchived, Pageable pageable) {
		return items.list(q, includeArchived, pageable);
	}

	@PostMapping
	ResponseEntity<ItemResponse> create(@Valid @RequestBody CreateItemRequest request) {
		ItemResponse created = items.create(request);
		return ResponseEntity.created(URI.create("/api/items/" + created.id())).body(created);
	}

	@GetMapping("/{id}")
	ItemResponse get(@PathVariable Long id) {
		return items.get(id);
	}

	@PutMapping("/{id}")
	ItemResponse update(@PathVariable Long id, @Valid @RequestBody UpdateItemRequest request) {
		return items.update(id, request);
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> archive(@PathVariable Long id) {
		items.archive(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{itemId}/variants")
	List<VariantResponse> listVariants(@PathVariable Long itemId,
			@RequestParam(defaultValue = "false") boolean includeArchived) {
		return variants.listByItem(itemId, includeArchived);
	}

	@PostMapping("/{itemId}/variants")
	ResponseEntity<VariantResponse> createVariant(@PathVariable Long itemId,
			@Valid @RequestBody VariantRequest request) {
		VariantResponse created = variants.create(itemId, request);
		return ResponseEntity.created(URI.create("/api/variants/" + created.id())).body(created);
	}
}
