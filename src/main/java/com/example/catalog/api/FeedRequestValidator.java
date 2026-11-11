package com.example.catalog.api;

import com.example.catalog.api.dto.FeedMode;
import com.example.catalog.api.dto.FeedRequest;
import com.example.catalog.api.dto.ProductAction;
import com.example.catalog.api.dto.ProductRequest;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class FeedRequestValidator {
    public void validateFeedRules(FeedRequest request) {
        Set<String> products = new HashSet<>();
        Set<String> variants = new HashSet<>();
        for (ProductRequest product : request.products()) {
            if (!products.add(product.supplierProductId())) {
                throw new StructuralValidationException("supplierProductId must be unique in the feed");
            }
            ProductAction action = product.action() == null ? ProductAction.UPSERT : product.action();
            if (request.mode() == FeedMode.FULL && action != ProductAction.UPSERT) {
                throw new StructuralValidationException("FULL feeds allow only UPSERT products");
            }
            if (action == ProductAction.DELETE) continue;
            if (isBlank(product.brand()) || product.category() == null || product.names() == null
                    || isBlank(product.names().get("en")) || product.variants() == null
                    || product.variants().isEmpty() || product.variants().size() > 20) {
                throw new StructuralValidationException("UPSERT requires brand, category, names.en and 1 to 20 variants");
            }
            int depth = 0;
            for (var node = product.category(); node != null; node = node.parent()) depth++;
            if (depth > 4) throw new StructuralValidationException("category nesting must not exceed 4 levels");
            for (var variant : product.variants()) {
                if (!variants.add(variant.supplierVariantId())) {
                    throw new StructuralValidationException("supplierVariantId must be unique in the feed");
                }
            }
        }
    }
    private boolean isBlank(String value) { return value == null || value.isBlank(); }
}
