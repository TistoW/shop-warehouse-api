package com.tisto.warehouse.catalog;

import java.math.BigDecimal;

import com.tisto.warehouse.common.Auditable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "variants")
public class Variant extends Auditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "item_id")
	private Item item;

	private String sku;

	private String name;

	private BigDecimal price;

	// Read-only here. Stock only moves through StockService so every change leaves a ledger row.
	@Column(name = "stock_quantity", insertable = false, updatable = false)
	private int stockQuantity;

	private boolean active = true;

	@Version
	private long version;

	protected Variant() {
	}

	public Variant(Item item, String sku, String name, BigDecimal price) {
		this.item = item;
		this.sku = sku;
		this.name = name;
		this.price = price;
	}

	public void update(String sku, String name, BigDecimal price) {
		this.sku = sku;
		this.name = name;
		this.price = price;
	}

	public void archive() {
		active = false;
	}

	public Long getId() {
		return id;
	}

	public Item getItem() {
		return item;
	}

	public String getSku() {
		return sku;
	}

	public String getName() {
		return name;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public int getStockQuantity() {
		return stockQuantity;
	}

	public boolean isActive() {
		return active;
	}
}
