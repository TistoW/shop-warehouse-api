package com.tisto.warehouse.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import com.tisto.warehouse.catalog.CreateItemRequest;
import com.tisto.warehouse.catalog.ItemService;
import com.tisto.warehouse.catalog.VariantRepository;
import com.tisto.warehouse.inventory.InsufficientStockException;
import com.tisto.warehouse.inventory.MovementReason;
import com.tisto.warehouse.inventory.StockAdjustmentRequest;
import com.tisto.warehouse.inventory.StockMovementResponse;
import com.tisto.warehouse.inventory.StockService;

@SpringBootTest
abstract class ConcurrentOrderTests {

	private static final int STOCK = 5;
	private static final int BUYERS = 10;

	@Autowired
	ItemService items;
	@Autowired
	StockService stock;
	@Autowired
	OrderService orders;
	@Autowired
	VariantRepository variants;

	@Test
	void tenBuyersCompeteForFiveUnits() throws Exception {
		CreateItemRequest request = new CreateItemRequest("Last chance tee", null, "RACE-" + System.nanoTime(),
				new BigDecimal("50000"), null);
		long variantId = items.create(request).variants().get(0).id();
		stock.adjust(variantId, new StockAdjustmentRequest(STOCK, MovementReason.RESTOCK, null));

		OrderRequest oneUnit = new OrderRequest(List.of(new OrderRequest.Line(variantId, 1)));
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(BUYERS);
		List<Future<Boolean>> results = new ArrayList<>();
		for (int i = 0; i < BUYERS; i++) {
			results.add(pool.submit(() -> {
				start.await();
				try {
					orders.place(oneUnit);
					return true;
				}
				catch (InsufficientStockException e) {
					return false;
				}
			}));
		}
		start.countDown();
		int succeeded = 0;
		for (Future<Boolean> result : results) {
			if (result.get()) {
				succeeded++;
			}
		}
		pool.shutdown();

		assertThat(succeeded).isEqualTo(STOCK);
		assertThat(variants.findById(variantId).orElseThrow().getStockQuantity()).isZero();
		List<StockMovementResponse> movements = stock.movements(variantId, PageRequest.of(0, 50)).getContent();
		assertThat(movements.stream().filter(m -> m.reason() == MovementReason.SALE)).hasSize(STOCK);
	}
}
