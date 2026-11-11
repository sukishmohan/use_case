package com.example.catalog;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.example.catalog.api.FeedRequestValidator;
import com.example.catalog.api.StructuralValidationException;
import com.example.catalog.api.ApiException;
import com.example.catalog.api.dto.FeedResult;
import com.example.catalog.domain.Supplier;
import com.example.catalog.repository.SupplierRepository;
import com.example.catalog.service.FeedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = com.example.catalog.api.FeedController.class)
class FeedControllerWebMvcTest {
    private static final String PATH = "/api/v1/catalog/suppliers/ACMEFOODS/feeds";
    private static final String TOKEN = "acme-test-token";
    private static final String VALID_JSON = """
            {"feedId":"FD-ACMEFOODS-000045","sequenceNo":45,"mode":"DELTA",
             "generatedAt":"2026-11-06T11:30:00+05:30","products":[
               {"supplierProductId":"P1","action":"UPSERT","brand":"Brand",
                "category":{"name":"Food","parent":null},"names":{"en":"Item"},"attributes":[],
                "variants":[{"supplierVariantId":"V1","packSize":{"value":1,"unit":"KG"},
                  "mrp":10,"sellingPrice":9,"priceIncludesTax":true,"gstPercent":5,
                  "barcode":"8901234567890","stock":1}]}]}
            """;

    @Autowired MockMvc mvc;
    @MockitoBean SupplierRepository suppliers;
    @MockitoBean FeedService feedService;
    @MockitoBean FeedRequestValidator validator;

    @BeforeEach void supplierExistsByDefault() {
        Supplier supplier = new Supplier(); supplier.setSupplierCode("ACMEFOODS");
        supplier.setApiToken(TOKEN); supplier.setActive("Y");
        when(suppliers.findById("ACMEFOODS")).thenReturn(Optional.of(supplier));
        when(feedService.process(anyString(), any())).thenReturn(
                new FeedResult("FD-ACMEFOODS-000045", "COMPLETED",
                        new FeedResult.Counts(1, 1, 0, 0, 0, 0, 0), java.util.List.of(), java.util.List.of()));
    }

    @Test void controller200_acceptsAuthenticatedStructurallyValidFeed() throws Exception {
        mvc.perform(post(PATH).header("X-Supplier-Token", TOKEN).contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test void controller400_mapsStructuralValidationFailure() throws Exception {
        doThrow(new StructuralValidationException("invalid feed structure"))
                .when(validator).validateFeedRules(any());
        mvc.perform(post(PATH).header("X-Supplier-Token", TOKEN).contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test void controller400_rejectsMalformedJson() throws Exception {
        mvc.perform(post(PATH).header("X-Supplier-Token", TOKEN).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test void controller401_requiresSupplierToken() throws Exception {
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_SUPPLIER_TOKEN"));
    }

    @Test void controller404_rejectsUnknownSupplier() throws Exception {
        when(suppliers.findById("ACMEFOODS")).thenReturn(Optional.empty());
        mvc.perform(post(PATH).header("X-Supplier-Token", TOKEN).contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SUPPLIER_NOT_FOUND"));
    }

    @Test void controller409_mapsStaleSequence() throws Exception {
        when(feedService.process(anyString(), any())).thenThrow(new ApiException(HttpStatus.CONFLICT,
                "DUPLICATE_OR_STALE", "stale"));
        mvc.perform(post(PATH).header("X-Supplier-Token", TOKEN).contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DUPLICATE_OR_STALE"));
    }

    @Test void controller422_mapsSequenceGap() throws Exception {
        when(feedService.process(anyString(), any())).thenThrow(new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "SEQUENCE_GAP", "gap"));
        mvc.perform(post(PATH).header("X-Supplier-Token", TOKEN).contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("SEQUENCE_GAP"));
    }
}
