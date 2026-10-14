package com.tisto.warehouse.inventory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.tisto.warehouse.ApiTestSupport;

class StockApiTest extends ApiTestSupport {

	@Test
	void correctionCannotTakeStockBelowZero() throws Exception {
		long variantId = createVariant("10000", 4);

		mvc.perform(adjustment(variantId, -5, "ADJUSTMENT"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.available").value(4));
		mvc.perform(adjustment(variantId, -1, "ADJUSTMENT"))
				.andExpect(status().isCreated());

		mvc.perform(get("/api/variants/{id}", variantId)).andExpect(jsonPath("$.stockQuantity").value(3));
	}

	@Test
	void rejectsZeroQuantityAndManualSaleMovements() throws Exception {
		long variantId = createVariant("10000", 1);

		mvc.perform(adjustment(variantId, 0, "ADJUSTMENT")).andExpect(status().isBadRequest());
		mvc.perform(adjustment(variantId, 1, "SALE")).andExpect(status().isBadRequest());
		mvc.perform(adjustment(variantId, -1, "RESTOCK")).andExpect(status().isBadRequest());
	}

	@Test
	void archivedVariantCannotBeAdjusted() throws Exception {
		long variantId = createVariant("10000", 1);
		mvc.perform(delete("/api/variants/{id}", variantId)).andExpect(status().isNoContent());

		mvc.perform(adjustment(variantId, 1, "RESTOCK")).andExpect(status().isConflict());
	}

	private static MockHttpServletRequestBuilder adjustment(long variantId, int quantity, String reason) {
		return post("/api/variants/{id}/stock-adjustments", variantId).contentType(MediaType.APPLICATION_JSON)
				.content("{\"quantity\": %d, \"reason\": \"%s\"}".formatted(quantity, reason));
	}
}
