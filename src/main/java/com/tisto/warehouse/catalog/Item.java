package com.tisto.warehouse.catalog;

import java.util.ArrayList;
import java.util.List;

import com.tisto.warehouse.common.Auditable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "items")
public class Item extends Auditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;

	private String description;

	private boolean active = true;

	@OneToMany(mappedBy = "item", cascade = CascadeType.PERSIST)
	private List<Variant> variants = new ArrayList<>();

	protected Item() {
	}

	public Item(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public void rename(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public void addVariant(Variant variant) {
		variants.add(variant);
	}

	public void archive() {
		active = false;
		variants.forEach(Variant::archive);
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public boolean isActive() {
		return active;
	}

	public List<Variant> getVariants() {
		return variants;
	}
}
