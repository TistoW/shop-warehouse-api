package com.tisto.warehouse.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tisto.warehouse.catalog.Variant;
import com.tisto.warehouse.catalog.VariantRepository;
import com.tisto.warehouse.common.BadRequestException;
import com.tisto.warehouse.common.ConflictException;
import com.tisto.warehouse.common.NotFoundException;

@Service
@Transactional
public class StockService {

	private final VariantRepository variants;
	private final StockMovementRepository movements;

	public StockService(VariantRepository variants, StockMovementRepository movements) {
		this.variants = variants;
		this.movements = movements;
	}

	public StockMovementResponse adjust(Long variantId, StockAdjustmentRequest request) {
		int quantity = request.quantity();
		if (quantity == 0) {
			throw new BadRequestException("quantity must not be zero");
		}
		if (request.reason() == MovementReason.SALE) {
			throw new BadRequestException("SALE movements are created by orders only");
		}
		if (request.reason() == MovementReason.RESTOCK && quantity < 0) {
			throw new BadRequestException("A RESTOCK must have a positive quantity");
		}
		return StockMovementResponse.from(apply(variantId, quantity, request.reason(), request.note()));
	}

	// Joins the caller's transaction, so a later failure (another line of the same order) rolls
	// back both the stock change and the ledger row.
	public StockMovement apply(Long variantId, int delta, MovementReason reason, String reference) {
		if (variants.changeStock(variantId, delta) == 0) {
			throw explainFailure(variantId, delta);
		}
		return movements.save(new StockMovement(variantId, delta, reason, reference));
	}

	@Transactional(readOnly = true)
	public Page<StockMovementResponse> movements(Long variantId, Pageable pageable) {
		if (!variants.existsById(variantId)) {
			throw new NotFoundException("Variant " + variantId + " not found");
		}
		return movements.findByVariantIdOrderByIdDesc(variantId, pageable).map(StockMovementResponse::from);
	}

	private RuntimeException explainFailure(Long variantId, int delta) {
		Variant variant = variants.findById(variantId)
				.orElseThrow(() -> new NotFoundException("Variant " + variantId + " not found"));
		if (!variant.isActive()) {
			return new ConflictException("Variant " + variant.getSku() + " is archived");
		}
		return new InsufficientStockException(variant.getSku(), -delta, variant.getStockQuantity());
	}
}
