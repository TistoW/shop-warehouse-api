package com.tisto.warehouse.inventory;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock_movements")
public class StockMovement {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "variant_id")
	private Long variantId;

	@Column(name = "quantity_change")
	private int quantityChange;

	@Enumerated(EnumType.STRING)
	private MovementReason reason;

	private String reference;

	@Column(name = "created_at")
	private Instant createdAt = Instant.now();

	protected StockMovement() {
	}

	public StockMovement(Long variantId, int quantityChange, MovementReason reason, String reference) {
		this.variantId = variantId;
		this.quantityChange = quantityChange;
		this.reason = reason;
		this.reference = reference;
	}

	public Long getId() {
		return id;
	}

	public Long getVariantId() {
		return variantId;
	}

	public int getQuantityChange() {
		return quantityChange;
	}

	public MovementReason getReason() {
		return reason;
	}

	public String getReference() {
		return reference;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
