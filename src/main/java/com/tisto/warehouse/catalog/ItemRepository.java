package com.tisto.warehouse.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, Long> {

	@Query("""
			select i from Item i
			where (:includeArchived = true or i.active = true)
			  and lower(i.name) like :pattern
			""")
	Page<Item> search(@Param("pattern") String pattern, @Param("includeArchived") boolean includeArchived,
			Pageable pageable);
}
