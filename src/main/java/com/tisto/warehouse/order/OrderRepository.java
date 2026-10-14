package com.tisto.warehouse.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {

	Page<Order> findAllByOrderByIdDesc(Pageable pageable);

	@Query(value = "select nextval('order_number_seq')", nativeQuery = true)
	long nextOrderSequence();
}
