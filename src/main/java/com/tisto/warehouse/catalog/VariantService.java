package com.tisto.warehouse.catalog;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tisto.warehouse.common.ConflictException;
import com.tisto.warehouse.common.NotFoundException;

@Service
@Transactional
public class VariantService {

	private final VariantRepository variants;
	private final ItemRepository items;

	public VariantService(VariantRepository variants, ItemRepository items) {
		this.variants = variants;
		this.items = items;
	}

	public VariantResponse create(Long itemId, VariantRequest request) {
		Item item = items.findById(itemId).orElseThrow(() -> new NotFoundException("Item " + itemId + " not found"));
		if (!item.isActive()) {
			throw new ConflictException("Item " + itemId + " is archived");
		}
		checkSkuFree(request.sku(), null);
		return VariantResponse.from(variants.save(new Variant(item, request.sku(), request.name(), request.price())));
	}

	@Transactional(readOnly = true)
	public List<VariantResponse> listByItem(Long itemId, boolean includeArchived) {
		if (!items.existsById(itemId)) {
			throw new NotFoundException("Item " + itemId + " not found");
		}
		List<Variant> found = includeArchived ? variants.findByItemIdOrderById(itemId)
				: variants.findByItemIdAndActiveTrueOrderById(itemId);
		return found.stream().map(VariantResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public VariantResponse get(Long id) {
		return VariantResponse.from(find(id));
	}

	public VariantResponse update(Long id, VariantRequest request) {
		Variant variant = find(id);
		checkSkuFree(request.sku(), id);
		variant.update(request.sku(), request.name(), request.price());
		variants.flush();
		return VariantResponse.from(variant);
	}

	public void archive(Long id) {
		find(id).archive();
	}

	void checkSkuFree(String sku, Long ownId) {
		boolean taken = ownId == null ? variants.existsBySku(sku) : variants.existsBySkuAndIdNot(sku, ownId);
		if (taken) {
			throw new DuplicateSkuException(sku);
		}
	}

	private Variant find(Long id) {
		return variants.findById(id).orElseThrow(() -> new NotFoundException("Variant " + id + " not found"));
	}
}
