package com.tisto.warehouse.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;
import com.tisto.warehouse.ApiTestSupport;

class CatalogApiTest extends ApiTestSupport {

	@Test
	void createsItemWithVariantsAndComputesTotalStock() throws Exception {
		String redSku = uniqueSku("TEE-R");
		String blueSku = uniqueSku("TEE-B");
		String created = mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "Plain tee", "variants": [
				  {"sku": "%s", "name": "Red / M", "price": 99000},
				  {"sku": "%s", "name": "Blue / M", "price": 109000.50}]}
				""".formatted(redSku, blueSku)))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.variants", hasSize(2)))
				.andExpect(jsonPath("$.totalStock").value(0))
				.andReturn().getResponse().getContentAsString();

		Number redId = JsonPath.read(created, "$.variants[0].id");
		restock(redId.longValue(), 4);

		Number itemId = JsonPath.read(created, "$.id");
		mvc.perform(get("/api/items/{id}", itemId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalStock").value(4));
	}

	@Test
	void createsDefaultVariantWhenOnlySkuAndPriceAreGiven() throws Exception {
		mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "Mug", "sku": "%s", "price": 45000}
				""".formatted(uniqueSku("MUG"))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.variants", hasSize(1)))
				.andExpect(jsonPath("$.variants[0].name").value("Default"))
				.andExpect(jsonPath("$.variants[0].stockQuantity").value(0));
	}

	@Test
	void updatesItemAndVariantButNotStock() throws Exception {
		long variantId = createVariant("10000", 3);
		Number itemId = JsonPath.read(mvc.perform(get("/api/variants/{id}", variantId)).andReturn().getResponse()
				.getContentAsString(), "$.itemId");

		mvc.perform(put("/api/items/{id}", itemId).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"Renamed\", \"description\": \"Now with a description\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Renamed"));

		mvc.perform(put("/api/variants/{id}", variantId).contentType(MediaType.APPLICATION_JSON)
				.content("{\"sku\": \"%s\", \"name\": \"Default\", \"price\": 12500}".formatted(uniqueSku("NEW"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.price").value(12500))
				.andExpect(jsonPath("$.stockQuantity").value(3))
				.andExpect(result -> {
					String created = JsonPath.read(result.getResponse().getContentAsString(), "$.createdAt");
					String updated = JsonPath.read(result.getResponse().getContentAsString(), "$.updatedAt");
					assertThat(Instant.parse(updated)).isAfter(Instant.parse(created));
				});
	}

	@Test
	void archivingAnItemHidesItAndArchivesItsVariants() throws Exception {
		long variantId = createVariant("10000", 3);
		Number itemId = JsonPath.read(mvc.perform(get("/api/variants/{id}", variantId)).andReturn().getResponse()
				.getContentAsString(), "$.itemId");

		mvc.perform(delete("/api/items/{id}", itemId)).andExpect(status().isNoContent());

		mvc.perform(get("/api/items/{id}/variants", itemId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
		mvc.perform(get("/api/items/{id}/variants", itemId).param("includeArchived", "true"))
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].active").value(false));
		mvc.perform(get("/api/items").param("size", "100"))
				.andExpect(jsonPath("$.content[?(@.id == %s)]", itemId).isEmpty());
	}

	@Test
	void rejectsInvalidInput() throws Exception {
		mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "", "variants": [{"sku": "X", "name": "Y", "price": -1}]}
				"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors").isArray())
				.andExpect(jsonPath("$.errors", hasSize(2)));
	}

	@Test
	void rejectsDuplicateSku() throws Exception {
		String sku = uniqueSku("DUP");
		String body = "{\"name\": \"Thing\", \"sku\": \"%s\", \"price\": 1000}".formatted(sku);
		mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated());
		mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.sku").value(sku));
	}

	@Test
	void unknownItemIsNotFound() throws Exception {
		mvc.perform(get("/api/items/{id}", 999999))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Not found"));
	}
}
