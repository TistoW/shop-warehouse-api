package com.tisto.warehouse.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "order_number")
	private String orderNumber;

	@Column(name = "total_amount")
	private BigDecimal totalAmount = BigDecimal.ZERO;

	@Column(name = "created_at")
	private Instant createdAt = Instant.now();

	@OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST)
	private List<OrderItem> items = new ArrayList<>();

	protected Order() {
	}

	public Order(String orderNumber) {
		this.orderNumber = orderNumber;
	}

	public void addItem(OrderItem item) {
		items.add(item);
		totalAmount = totalAmount.add(item.getLineTotal());
	}

	public Long getId() {
		return id;
	}

	public String getOrderNumber() {
		return orderNumber;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public List<OrderItem> getItems() {
		return items;
	}
}
