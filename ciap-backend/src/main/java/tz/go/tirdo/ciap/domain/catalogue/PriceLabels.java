package tz.go.tirdo.ciap.domain.catalogue;

import java.math.BigDecimal;
import java.text.DecimalFormat;

/** Customer-facing wording. A missing amount is never replaced with a sample price. */
public final class PriceLabels {

    private PriceLabels() {}

    public static String customerLabel(PricingMethod method, BigDecimal amount, String currency, String unitLabel) {
        String money = money(amount, currency);
        return switch (method) {
            case FIXED -> "Fixed fee: " + money;
            case STARTING -> "Starting from " + money + ". The final amount may differ.";
            case QUOTATION -> "Price determined after technical assessment. "
                    + "Final price will be determined after TIRDO reviews the consultancy requirements. "
                    + "No final payment amount has been established yet.";
            case VARIABLE -> "Unit price " + money
                    + (unitLabel == null || unitLabel.isBlank() ? "" : " per " + unitLabel.trim())
                    + ". The final amount may differ.";
            case CUSTOM -> "Price set for this engagement after approval. "
                    + "No final payment amount has been established yet.";
        };
    }

    public static String money(BigDecimal amount, String currency) {
        String code = currency == null || currency.isBlank() ? "TZS" : currency;
        if (amount == null) {
            return code;
        }
        return code + " " + new DecimalFormat("#,##0.00").format(amount);
    }
}
