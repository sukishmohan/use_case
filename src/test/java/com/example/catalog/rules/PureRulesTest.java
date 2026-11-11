package com.example.catalog.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.catalog.api.dto.AttributeRequest;
import com.example.catalog.api.dto.CategoryRequest;
import com.example.catalog.diff.ContentHasher;
import com.example.catalog.diff.VariantHashInput;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PureRulesTest {

    @Test void C2_oneAndFourLevelCategoryPathsAreNormalized() {
        assertThat(CategoryRules.path(new CategoryRequest("  bASmati   Rice ", null))).isEqualTo("Basmati Rice");
        CategoryRequest tree = new CategoryRequest(" basmati ",
                new CategoryRequest(" rice ", new CategoryRequest("  staples  ", null)));
        assertThat(CategoryRules.path(tree)).isEqualTo("Staples > Rice > Basmati");
        CategoryRequest four = new CategoryRequest("leaf", new CategoryRequest("level 3",
                new CategoryRequest("level 2", new CategoryRequest("root", null))));
        assertThat(CategoryRules.normalizedNames(four)).hasSize(4);
    }

    @Test void C2_rejectsFiveLevelsAndMatchesPathsWithoutCaseSensitivity() {
        CategoryRequest five = new CategoryRequest("5", new CategoryRequest("4",
                new CategoryRequest("3", new CategoryRequest("2", new CategoryRequest("1", null)))));
        assertThatThrownBy(() -> CategoryRules.path(five)).isInstanceOf(IllegalArgumentException.class);
        assertThat(CategoryRules.samePath("Staples > Rice", " staples   > RICE ")).isTrue();
    }

    @Test void C3_categoryCodeUsesFirstThreeLettersAndFormatsSequence() {
        assertThat(SkuRules.categoryCode("Toor Dal")).isEqualTo("TOO");
        assertThat(SkuRules.categoryCode("R1ce 2")).isEqualTo("RCE");
        assertThat(SkuRules.format("ACF", "Toor Dal", 4)).isEqualTo("ACF-TOO-000004");
    }

    @Test void C3_existingSkusStayPermanentAndRejectedVariantsDoNotConsumeNumbers() {
        var allocation = SkuRules.allocate("ACF", 3, List.of(
                new SkuRules.Candidate("old", "Basmati", true, "ACF-BAS-000001"),
                new SkuRules.Candidate("bad", "Toor Dal", false, null),
                new SkuRules.Candidate("new1", "Toor Dal", true, null),
                new SkuRules.Candidate("new2", "Rice 2", true, null)));
        assertThat(allocation.assignments()).extracting(SkuRules.Assignment::sku)
                .containsExactly("ACF-BAS-000001", null, "ACF-TOO-000003", "ACF-RIC-000004");
        assertThat(allocation.nextValue()).isEqualTo(5);
    }

    @Test void C4_brandAliasHitAndMiss() {
        Map<String, String> aliases = Map.of("goldenharvest", "Golden Harvest");
        assertThat(BrandRules.normalize(" golden  harvest ", aliases)).isEqualTo("Golden Harvest");
        assertThat(BrandRules.normalize("ACME foods", aliases)).isEqualTo("Acme Foods");
        assertThat(BrandRules.normalize("golden harvest", Map.of("Golden Harvest", "Preferred Brand")))
                .isEqualTo("Preferred Brand");
    }

    @Test void C5_missingNullAndBlankLocalesFallBackToEnglish() {
        Map<String, LocaleRules.Text> result = LocaleRules.normalize(
                new java.util.HashMap<>(Map.of("en", "English", "hi", "   ")) {{ put("ta", null); }});
        assertThat(result.get("en")).isEqualTo(new LocaleRules.Text("English", false));
        assertThat(result.get("hi")).isEqualTo(new LocaleRules.Text("English", true));
        assertThat(result.get("ta")).isEqualTo(new LocaleRules.Text("English", true));
    }

    @Test void C6_attributeKeysValuesAndDuplicateLastWins() {
        var result = AttributeRules.normalize(List.of(
                new AttributeRequest("Is Organic", "yes"),
                new AttributeRequest("origin", "India"),
                new AttributeRequest(" Origin ", "Punjab, India"),
                new AttributeRequest("On-Sale", " FALSE ")));
        assertThat(result).containsEntry("is_organic", "Y")
                .containsEntry("origin", "Punjab, India").containsEntry("on_sale", "N");
    }

    @Test void C7_ean13AndActiveCrossSupplierConflict() {
        String good = ean("890123456789");
        assertThat(BarcodeRules.isValidEan13(good)).isTrue();
        assertThat(BarcodeRules.isValidEan13(good.substring(0, 12) + ((good.charAt(12) - '0' + 1) % 10))).isFalse();
        assertThat(BarcodeRules.isValidEan13("123456789012")).isFalse();
        assertThat(BarcodeRules.isValidEan13("12345678901234")).isFalse();
        assertThat(BarcodeRules.isValidEan13("12345678901a2")).isFalse();
        assertThat(BarcodeRules.validate(good, "ACMEFOODS", (code, supplier) -> true))
                .isEqualTo(BarcodeRules.Result.BARCODE_CONFLICT);
        assertThat(BarcodeRules.validate(good, "ACMEFOODS", (code, supplier) -> false))
                .isEqualTo(BarcodeRules.Result.VALID);
        assertThat(BarcodeRules.validate(good, "ACMEFOODS", List.of(
                new BarcodeRules.BarcodeUse(good, "OTHER", true))))
                .isEqualTo(BarcodeRules.Result.BARCODE_CONFLICT);
        assertThat(BarcodeRules.validate(good, "ACMEFOODS", List.of(
                new BarcodeRules.BarcodeUse(good, "OTHER", false),
                new BarcodeRules.BarcodeUse(good, "ACMEFOODS", true))))
                .isEqualTo(BarcodeRules.Result.VALID);
    }

    @ParameterizedTest(name = "C8 inclusive and exclusive tax calculations at GST {0}%")
    @ValueSource(ints = {0, 5, 12, 18, 28})
    void C8_taxSplitWorksAtSupportedRates(int gst) {
        BigDecimal expectedBase = new BigDecimal("100.00");
        BigDecimal exclusiveIncl = expectedBase.multiply(BigDecimal.valueOf(100 + gst))
                .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        var exclusive = PricingRules.calculate(new BigDecimal("200"), expectedBase, false, gst);
        assertThat(exclusive.basePrice()).isEqualByComparingTo(expectedBase);
        assertThat(exclusive.sellingInclTax()).isEqualByComparingTo(exclusiveIncl);
        var inclusive = PricingRules.calculate(new BigDecimal("200"), exclusiveIncl, true, gst);
        assertThat(inclusive.basePrice()).isEqualByComparingTo(expectedBase);
        assertThat(inclusive.taxAmount()).isEqualByComparingTo(exclusiveIncl.subtract(expectedBase));
    }

    @Test void C8_roundsThenSubtractsAndEnforcesMrpBoundary() {
        var rice = PricingRules.calculate(new BigDecimal("180"), new BigDecimal("165"), true, 5);
        assertThat(rice.basePrice()).isEqualByComparingTo("157.14");
        assertThat(rice.taxAmount()).isEqualByComparingTo("7.86");
        assertThat(rice.discountPct()).isEqualByComparingTo("8.33");
        var dal = PricingRules.calculate(new BigDecimal("210"), new BigDecimal("189"), false, 5);
        assertThat(dal.sellingInclTax()).isEqualByComparingTo("198.45");
        assertThat(dal.discountPct()).isEqualByComparingTo("5.50");
        assertThat(PricingRules.calculate(new BigDecimal("100"), new BigDecimal("100"), true, 5)
                .sellingInclTax()).isEqualByComparingTo("100.00");
        assertThatThrownBy(() -> PricingRules.calculate(new BigDecimal("100"), new BigDecimal("100.01"), true, 5))
                .isInstanceOf(PricingRules.PriceAboveMrpException.class);
        assertThatThrownBy(() -> PricingRules.calculate(new BigDecimal("200"), new BigDecimal("100"), true, 7))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void C9_gramAndMilliliterUnitPricesConvertToBaseUnits() {
        assertThat(UnitPriceRules.calculate(new BigDecimal("60"), new BigDecimal("250"), "ML"))
                .isEqualTo(new UnitPriceRules.UnitPrice(new BigDecimal("240.00"), "L"));
        assertThat(UnitPriceRules.calculate(new BigDecimal("165"), new BigDecimal("1000"), "G"))
                .isEqualTo(new UnitPriceRules.UnitPrice(new BigDecimal("165.00"), "KG"));
        assertThat(UnitPriceRules.calculate(new BigDecimal("60"), new BigDecimal("1"), "L"))
                .isEqualTo(new UnitPriceRules.UnitPrice(new BigDecimal("60.00"), "L"));
        assertThat(UnitPriceRules.calculate(new BigDecimal("12"), new BigDecimal("3"), "PCS"))
                .isEqualTo(new UnitPriceRules.UnitPrice(new BigDecimal("4.00"), "PCS"));
    }

    @Test void C10_stockBoundariesAndNegativeCorrection() {
        assertThat(StockRules.normalize(0).status()).isEqualTo(StockRules.Status.OUT_OF_STOCK);
        assertThat(StockRules.normalize(1).status()).isEqualTo(StockRules.Status.LOW);
        assertThat(StockRules.normalize(10).status()).isEqualTo(StockRules.Status.LOW);
        assertThat(StockRules.normalize(11).status()).isEqualTo(StockRules.Status.IN_STOCK);
        assertThat(StockRules.normalize(-5)).isEqualTo(new StockRules.Stock(0,
                StockRules.Status.OUT_OF_STOCK, true, "NEGATIVE_STOCK_CORRECTED"));
    }

    @Test void C11_hashIgnoresStockAndAttributeIterationOrder() {
        VariantHashInput first = hashInput(Map.of("origin", "India", "is_organic", "Y"));
        VariantHashInput reordered = hashInput(new java.util.LinkedHashMap<>() {{
            put("is_organic", "Y"); put("origin", "India");
        }});
        assertThat(ContentHasher.hash(first)).isEqualTo(ContentHasher.hash(reordered));
    }

    private static VariantHashInput hashInput(Map<String, String> attributes) {
        return new VariantHashInput("Basmati rice", "Golden Harvest", "Staples > Rice > Basmati", attributes,
                new BigDecimal("1.000"), "KG", new BigDecimal("180.00"), new BigDecimal("165.00"),
                new BigDecimal("5.00"), "8901234567890");
    }
    private static String ean(String twelveDigits) {
        int sum = 0;
        for (int i = 0; i < twelveDigits.length(); i++) sum += (twelveDigits.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        return twelveDigits + ((10 - sum % 10) % 10);
    }
}
