package com.example.catalog.api;

import com.example.catalog.api.dto.FeedRequest;
import com.example.catalog.api.dto.FeedResult;
import com.example.catalog.domain.Supplier;
import com.example.catalog.repository.SupplierRepository;
import com.example.catalog.service.FeedService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog/suppliers/{supplierCode}/feeds")
public class FeedController {
    private final SupplierRepository suppliers;
    private final FeedRequestValidator validator;
    private final FeedService feedService;
    public FeedController(SupplierRepository suppliers, FeedRequestValidator validator, FeedService feedService) {
        this.suppliers = suppliers; this.validator = validator; this.feedService = feedService;
    }

    @PostMapping
    public FeedResult receive(@PathVariable String supplierCode,
            @RequestHeader(value = "X-Supplier-Token", required = false) String token,
            @Valid @RequestBody FeedRequest request) {
        Supplier supplier = suppliers.findById(supplierCode.toUpperCase())
                .filter(s -> "Y".equals(s.getActive()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SUPPLIER_NOT_FOUND", "Unknown or inactive supplier"));
        if (token == null || !token.equals(supplier.getApiToken()))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_SUPPLIER_TOKEN", "Missing or invalid X-Supplier-Token");
        validator.validateFeedRules(request);
        return feedService.process(supplier.getSupplierCode(), request);
    }
}
