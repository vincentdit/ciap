package tz.go.tirdo.ciap.domain.catalogue;

/** How a published consultancy service explains its price to a customer. */
public enum PricingMethod {
    FIXED,
    STARTING,
    QUOTATION,
    VARIABLE,
    CUSTOM;

    public boolean requiresAmount() {
        return this == FIXED || this == STARTING || this == VARIABLE;
    }
}
