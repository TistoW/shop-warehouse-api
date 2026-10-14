package com.tisto.warehouse.order;

import java.math.BigDecimal;

import com.tisto.warehouse.catalog.Variant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items")
public class OrderItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id")
	private Order order;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "variant_id")
	private Variant variant;

	private int quantity;

	// Price at the time of sale, so later price edits do not rewrite history.
	@Column(name = "unit_price")
	private BigDecimal unitPrice;

	@Column(name = "line_total")
	private BigDecimal lineTotal;

	protected OrderItem() {
	}

	public OrderItem(Order order, Variant variant, int quantity) {
		this.order = order;
		this.variant = variant;
		this.quantity = quantity;
		this.unitPrice = variant.getPrice();
		this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
	}

	public Variant getVariant() {
		return variant;
	}

	public int getQuantity() {
		return quantity;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public BigDecimal getLineTotal() {
		return lineTotal;
	}
}
