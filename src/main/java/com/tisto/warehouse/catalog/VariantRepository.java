package com.tisto.warehouse.catalog;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VariantRepository extends JpaRepository<Variant, Long> {

	List<Variant> findByItemIdOrderById(Long itemId);

	List<Variant> findByItemIdAndActiveTrueOrderById(Long itemId);

	boolean existsBySku(String sku);

	boolean existsBySkuAndIdNot(String sku, Long id);

	// One statement for check and write, so concurrent callers cannot oversell. Returns 0 when the
	// variant is missing, archived, or the change would take stock below zero.
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update Variant v
			set v.stockQuantity = v.stockQuantity + :delta, v.version = v.version + 1, v.updatedAt = current_timestamp
			where v.id = :id and v.active = true and v.stockQuantity + :delta >= 0
			""")
	int changeStock(@Param("id") Long id, @Param("delta") int delta);
}
