package com.tisto.warehouse.catalog;

public class DuplicateSkuException extends RuntimeException {

	private final String sku;

	public DuplicateSkuException(String sku) {
		super("SKU " + sku + " is already in use");
		this.sku = sku;
	}

	public String getSku() {
		return sku;
	}
}
