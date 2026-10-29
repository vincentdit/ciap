package tz.go.tirdo.ciap.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.ciap.domain.catalogue.ConsultancyCategory;
import tz.go.tirdo.ciap.domain.catalogue.ConsultancyCategoryRepository;
import tz.go.tirdo.ciap.domain.catalogue.PricingMethod;
import tz.go.tirdo.ciap.domain.catalogue.ServiceOffering;
import tz.go.tirdo.ciap.domain.catalogue.ServiceOfferingRepository;
import tz.go.tirdo.ciap.domain.catalogue.ServicePriceVersion;
import tz.go.tirdo.ciap.domain.catalogue.ServicePriceVersionRepository;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Ensures the four department service lines exist, and publishes the
 * consultancy offers TIRDO describes on its public site. Amounts are
 * indicative planning fees. TIRDO has not published a tariff, so the
 * quotation remains the amount payable.
 */
@Component
@Order(2)
public class CatalogueSeeder implements ApplicationRunner {

    private static final String FEE_NOTE =
            "Indicative planning fee. TIRDO has not published this as a fixed tariff. The quotation is the amount payable.";

    private final ConsultancyCategoryRepository categories;
    private final ServiceOfferingRepository services;
    private final ServicePriceVersionRepository versions;

    public CatalogueSeeder(ConsultancyCategoryRepository categories,
                           ServiceOfferingRepository services,
                           ServicePriceVersionRepository versions) {
        this.categories = categories;
        this.services = services;
        this.versions = versions;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedCategory("INDUSTRIAL", "Industrial Research",
                "Process, manufacturing, and laboratory work routed to Industrial Research.",
                ConsultancyType.INDUSTRIAL, 1);
        seedCategory("ENGINEERING", "Engineering",
                "Plant design and equipment assessment routed to Engineering.",
                ConsultancyType.ENGINEERING, 2);
        seedCategory("ICT", "ICT",
                "Information systems and digital work routed to ICT.",
                ConsultancyType.ICT, 3);
        seedCategory("ENVIRONMENTAL", "Environmental",
                "Pollution and waste work routed to Environmental.",
                ConsultancyType.ENVIRONMENTAL, 4);
        offers().forEach(this::seedOffer);
    }

    private void seedCategory(String code, String name, String description, ConsultancyType type, int order) {
        if (categories.findByCodeIgnoreCase(code).isPresent()) {
            return;
        }
        categories.save(ConsultancyCategory.builder()
                .code(code)
                .name(name)
                .description(description)
                .consultancyType(type)
                .displayOrder(order)
                .active(true)
                .build());
    }

    private void seedOffer(Offer offer) {
        ConsultancyCategory category = categories.findByCodeIgnoreCase(offer.categoryCode()).orElseThrow();
        if (services.findByCategoryIdAndNameIgnoreCase(category.getId(), offer.name()).isPresent()) {
            return;
        }
        ServiceOffering service = services.save(ServiceOffering.builder()
                .category(category)
                .name(offer.name())
                .description(offer.description())
                .pricingMethod(offer.method())
                .amount(offer.amount())
                .currency("TZS")
                .unitLabel(offer.unitLabel())
                .conditions(offer.conditions())
                .published(true)
                .active(true)
                .build());
        versions.save(ServicePriceVersion.builder()
                .service(service)
                .pricingMethod(service.getPricingMethod())
                .amount(service.getAmount())
                .currency(service.getCurrency())
                .unitLabel(service.getUnitLabel())
                .conditions(service.getConditions())
                .note("Opened from TIRDO's published consultancy list.")
                .changedBy("system")
                .changedAt(Instant.now())
                .build());
    }

    private static List<Offer> offers() {
        return List.of(
                new Offer("INDUSTRIAL", "Food microbiological analysis",
                        "Tests on foods of plant and animal origin, including fish, meat, cereals, dairy, edible oils, honey, and fruit. "
                                + "Accredited work covers total aerobic count, Staphylococcus aureus, coliforms, E. coli, and Salmonella.",
                        PricingMethod.VARIABLE, new BigDecimal("85000.00"), "sample", FEE_NOTE),
                new Offer("INDUSTRIAL", "Food proximate analysis",
                        "Ash, moisture, energy, fat, carbohydrate, and protein in raw materials, industrial products, and food.",
                        PricingMethod.VARIABLE, new BigDecimal("120000.00"), "sample", FEE_NOTE),
                new Offer("INDUSTRIAL", "Coal quality analysis",
                        "Proximate, ultimate, and coking-characteristic tests, used for Tanzania's coal quality records and utilisation studies.",
                        PricingMethod.VARIABLE, new BigDecimal("150000.00"), "sample", FEE_NOTE),
                new Offer("INDUSTRIAL", "Salt, fertiliser, and chalk quality analysis",
                        "Physicochemical checks on salt, fertiliser, and chalk, including heavy metals in raw materials and industrial products.",
                        PricingMethod.VARIABLE, new BigDecimal("95000.00"), "sample", FEE_NOTE),
                new Offer("INDUSTRIAL", "Industrial process assessment",
                        "A visit to review production, raw-material use, plant layout, quality assurance, and food-safety practice in a factory or SME.",
                        PricingMethod.QUOTATION, null, null,
                        "Scope is agreed on site. No final payment amount is set until the quotation is accepted."),

                new Offer("ENGINEERING", "Non-destructive testing",
                        "Radiographic, ultrasonic, magnetic-particle, liquid-penetrant, and visual inspection of welds, tanks, pressure vessels, pipelines, and castings.",
                        PricingMethod.STARTING, new BigDecimal("750000.00"), null, FEE_NOTE),
                new Offer("ENGINEERING", "Energy efficiency audit",
                        "A site audit for a factory, commercial building, or household, following ASHRAE and ISO practice, to find practical ways to cut energy use.",
                        PricingMethod.STARTING, new BigDecimal("1500000.00"), null, FEE_NOTE),
                new Offer("ENGINEERING", "Energy system performance test",
                        "Performance checks on ovens, cookstoves, boilers, tobacco barns, and solar dryers against national and international methods.",
                        PricingMethod.VARIABLE, new BigDecimal("250000.00"), "system", FEE_NOTE),
                new Offer("ENGINEERING", "Factory establishment support",
                        "Technical support for an investor setting up a process or a factory. The fee is set after the scope is approved.",
                        PricingMethod.CUSTOM, null, null,
                        "No final payment amount is set until TIRDO approves the engagement."),

                new Offer("ICT", "ICT support for SME operations",
                        "Advice on using information systems in the day-to-day work of small and medium industries.",
                        PricingMethod.QUOTATION, null, null,
                        "Scope is agreed first. No final payment amount is set until the quotation is accepted."),
                new Offer("ICT", "Cybersecurity advisory",
                        "Advisory work on cybersecurity and digital forensics for an industrial organisation.",
                        PricingMethod.QUOTATION, null, null,
                        "Scope is agreed first. No final payment amount is set until the quotation is accepted."),
                new Offer("ICT", "E-waste management advisory",
                        "Advice on handling electronic waste from industrial and institutional equipment.",
                        PricingMethod.QUOTATION, null, null,
                        "Scope is agreed first. No final payment amount is set until the quotation is accepted."),
                new Offer("ICT", "Industrial information systems advisory",
                        "Advice on research databases, sensor use, and mobile tools for industry. The fee is set after the scope is approved.",
                        PricingMethod.CUSTOM, null, null,
                        "No final payment amount is set until TIRDO approves the engagement."),

                new Offer("ENVIRONMENTAL", "Environmental and social impact assessment",
                        "An ESIA for an industrial project, covering the environmental and social effects before a decision is made.",
                        PricingMethod.QUOTATION, null, null,
                        "Scope is agreed first. No final payment amount is set until the quotation is accepted."),
                new Offer("ENVIRONMENTAL", "Environmental audit",
                        "A review of how an operating site manages pollution, waste, and its earlier environmental commitments.",
                        PricingMethod.QUOTATION, null, null,
                        "Scope is agreed first. No final payment amount is set until the quotation is accepted."),
                new Offer("ENVIRONMENTAL", "Air quality monitoring",
                        "Measurement of particulate matter and pollutant gases at industrial sites such as cement, steel, tobacco, and mines.",
                        PricingMethod.VARIABLE, new BigDecimal("180000.00"), "monitoring point", FEE_NOTE),
                new Offer("ENVIRONMENTAL", "Noise, light, and vibration monitoring",
                        "Noise level, light intensity, heat, and ground vibration checks for indoor and outdoor industrial sites.",
                        PricingMethod.VARIABLE, new BigDecimal("120000.00"), "monitoring point", FEE_NOTE),
                new Offer("ENVIRONMENTAL", "Water quality monitoring",
                        "Wastewater and potable-water checks, including sites such as beverage plants, breweries, and mines.",
                        PricingMethod.VARIABLE, new BigDecimal("75000.00"), "sample", FEE_NOTE),
                new Offer("ENVIRONMENTAL", "Industrial waste advisory",
                        "Advice on wastewater treatment, on-site systems, and solid waste from factory streams.",
                        PricingMethod.QUOTATION, null, null,
                        "Scope is agreed first. No final payment amount is set until the quotation is accepted.")
        );
    }

    private record Offer(String categoryCode, String name, String description,
                         PricingMethod method, BigDecimal amount, String unitLabel, String conditions) {}
}
