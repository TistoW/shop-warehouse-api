package com.tisto.warehouse.catalog;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tisto.warehouse.common.BadRequestException;
import com.tisto.warehouse.common.NotFoundException;

@Service
@Transactional
public class ItemService {

	private final ItemRepository items;
	private final VariantService variants;

	public ItemService(ItemRepository items, VariantService variants) {
		this.items = items;
		this.variants = variants;
	}

	public ItemResponse create(CreateItemRequest request) {
		Item item = new Item(request.name(), request.description());
		for (VariantRequest v : variantsOf(request)) {
			variants.checkSkuFree(v.sku(), null);
			item.addVariant(new Variant(item, v.sku(), v.name(), v.price()));
		}
		return ItemResponse.from(items.save(item));
	}

	@Transactional(readOnly = true)
	public Page<ItemResponse> list(String q, boolean includeArchived, Pageable pageable) {
		String pattern = q == null || q.isBlank() ? "%" : "%" + q.trim().toLowerCase() + "%";
		return items.search(pattern, includeArchived, pageable).map(ItemResponse::from);
	}

	@Transactional(readOnly = true)
	public ItemResponse get(Long id) {
		return ItemResponse.from(find(id));
	}

	public ItemResponse update(Long id, UpdateItemRequest request) {
		Item item = find(id);
		item.rename(request.name(), request.description());
		return ItemResponse.from(item);
	}

	public void archive(Long id) {
		find(id).archive();
	}

	private Item find(Long id) {
		return items.findById(id).orElseThrow(() -> new NotFoundException("Item " + id + " not found"));
	}

	private List<VariantRequest> variantsOf(CreateItemRequest request) {
		if (request.variants() != null && !request.variants().isEmpty()) {
			return request.variants();
		}
		if (request.sku() == null || request.sku().isBlank() || request.price() == null) {
			throw new BadRequestException("Provide variants, or sku and price for the default variant");
		}
		return List.of(new VariantRequest(request.sku(), "Default", request.price()));
	}
}
