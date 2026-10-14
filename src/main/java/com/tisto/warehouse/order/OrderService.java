package com.tisto.warehouse.order;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tisto.warehouse.catalog.Variant;
import com.tisto.warehouse.catalog.VariantRepository;
import com.tisto.warehouse.common.NotFoundException;
import com.tisto.warehouse.inventory.MovementReason;
import com.tisto.warehouse.inventory.StockService;

@Service
@Transactional
public class OrderService {

	private final OrderRepository orders;
	private final VariantRepository variants;
	private final StockService stock;

	public OrderService(OrderRepository orders, VariantRepository variants, StockService stock) {
		this.orders = orders;
		this.variants = variants;
		this.stock = stock;
	}

	public OrderResponse place(OrderRequest request) {
		// Lines for the same variant are merged, and the TreeMap walks them in id order. Every
		// order then takes its row locks in the same order, so two orders cannot deadlock.
		Map<Long, Integer> quantities = new TreeMap<>();
		request.items().forEach(line -> quantities.merge(line.variantId(), line.quantity(), Integer::sum));

		Order order = new Order(nextOrderNumber());
		quantities.forEach((variantId, quantity) -> {
			stock.apply(variantId, -quantity, MovementReason.SALE, order.getOrderNumber());
			// Read after the decrement: the row is locked by us now, so the price cannot change under us.
			Variant variant = variants.findById(variantId).orElseThrow();
			order.addItem(new OrderItem(order, variant, quantity));
		});
		return OrderResponse.from(orders.save(order));
	}

	@Transactional(readOnly = true)
	public Page<OrderResponse> list(Pageable pageable) {
		return orders.findAllByOrderByIdDesc(pageable).map(OrderResponse::from);
	}

	@Transactional(readOnly = true)
	public OrderResponse get(Long id) {
		return orders.findById(id).map(OrderResponse::from)
				.orElseThrow(() -> new NotFoundException("Order " + id + " not found"));
	}

	private String nextOrderNumber() {
		String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
		return "ORD-%s-%06d".formatted(day, orders.nextOrderSequence());
	}
}
