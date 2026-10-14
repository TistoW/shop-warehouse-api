package com.tisto.warehouse.order;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.tisto.warehouse.ApiTestSupport;

class OrderApiTest extends ApiTestSupport {

	@Test
	void orderDecrementsStockWritesMovementAndSnapshotsPrice() throws Exception {
		long variantId = createVariant("25000.00", 10);

		mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(order(variantId, 3)))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.totalAmount").value(75000.00))
				.andExpect(jsonPath("$.items[0].unitPrice").value(25000.00));

		mvc.perform(get("/api/variants/{id}", variantId)).andExpect(jsonPath("$.stockQuantity").value(7));
		mvc.perform(get("/api/variants/{id}/stock-movements", variantId))
				.andExpect(jsonPath("$.content[0].reason").value("SALE"))
				.andExpect(jsonPath("$.content[0].quantityChange").value(-3))
				.andExpect(jsonPath("$.content[0].reference").value(startsWith("ORD-")));

		mvc.perform(put("/api/variants/{id}", variantId).contentType(MediaType.APPLICATION_JSON)
				.content("{\"sku\": \"%s\", \"name\": \"Default\", \"price\": 99000}".formatted(uniqueSku("P"))))
				.andExpect(status().isOk());
		mvc.perform(get("/api/orders").param("size", "1"))
				.andExpect(jsonPath("$.content[0].items[0].unitPrice").value(25000.00));
	}

	@Test
	void orderForMoreThanAvailableIsRejectedAndStockIsUntouched() throws Exception {
		long variantId = createVariant("10000", 2);

		mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(order(variantId, 3)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title").value("Insufficient stock"))
				.andExpect(jsonPath("$.requested").value(3))
				.andExpect(jsonPath("$.available").value(2))
				.andExpect(jsonPath("$.sku").exists());

		mvc.perform(get("/api/variants/{id}", variantId)).andExpect(jsonPath("$.stockQuantity").value(2));
	}

	@Test
	void duplicateLinesAreMergedBeforeTheStockCheck() throws Exception {
		long variantId = createVariant("10000", 5);
		String body = """
				{"items": [{"variantId": %1$d, "quantity": 3}, {"variantId": %1$d, "quantity": 3}]}
				""".formatted(variantId);

		mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.requested").value(6));
	}

	@Test
	void failingLineLeavesEveryLineUntouched() throws Exception {
		long plentiful = createVariant("10000", 10);
		long scarce = createVariant("20000", 1);
		String body = """
				{"items": [{"variantId": %d, "quantity": 2}, {"variantId": %d, "quantity": 5}]}
				""".formatted(plentiful, scarce);

		mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isConflict());

		mvc.perform(get("/api/variants/{id}", plentiful)).andExpect(jsonPath("$.stockQuantity").value(10));
		mvc.perform(get("/api/variants/{id}", scarce)).andExpect(jsonPath("$.stockQuantity").value(1));
		mvc.perform(get("/api/variants/{id}/stock-movements", plentiful))
				.andExpect(jsonPath("$.content", hasSize(1)));
	}

	@Test
	void outOfStockVariantCannotBeSold() throws Exception {
		long variantId = createVariant("10000", 0);

		mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(order(variantId, 1)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.available").value(0));
	}

	@Test
	void unknownVariantIsNotFound() throws Exception {
		mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(order(999999, 1)))
				.andExpect(status().isNotFound());
	}

	@Test
	void rejectsEmptyOrderAndZeroQuantity() throws Exception {
		mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("{\"items\": []}"))
				.andExpect(status().isBadRequest());
		mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(order(1, 0)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void rejectsQuantitiesThatCouldOverflow() throws Exception {
		long variantId = createVariant("10000", 5);

		mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(order(variantId, Integer.MAX_VALUE)))
				.andExpect(status().isBadRequest());
		mvc.perform(post("/api/variants/{id}/stock-adjustments", variantId).contentType(MediaType.APPLICATION_JSON)
				.content("{\"quantity\": 2147483647, \"reason\": \"RESTOCK\"}"))
				.andExpect(status().isBadRequest());
	}

	private static String order(long variantId, int quantity) {
		return "{\"items\": [{\"variantId\": %d, \"quantity\": %d}]}".formatted(variantId, quantity);
	}
}
