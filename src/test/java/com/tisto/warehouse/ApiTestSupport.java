package com.tisto.warehouse;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class ApiTestSupport {

	@Autowired
	protected MockMvc mvc;

	protected static String uniqueSku(String prefix) {
		return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
	}

	protected long createVariant(String price, int stock) throws Exception {
		String body = """
				{"name": "Test item", "sku": "%s", "price": %s}
				""".formatted(uniqueSku("T"), price);
		String response = mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		long variantId = ((Number) JsonPath.read(response, "$.variants[0].id")).longValue();
		if (stock != 0) {
			restock(variantId, stock);
		}
		return variantId;
	}

	protected void restock(long variantId, int quantity) throws Exception {
		mvc.perform(post("/api/variants/{id}/stock-adjustments", variantId).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"quantity": %d, "reason": "RESTOCK"}
						""".formatted(quantity)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.quantityChange").value(quantity));
	}
}
