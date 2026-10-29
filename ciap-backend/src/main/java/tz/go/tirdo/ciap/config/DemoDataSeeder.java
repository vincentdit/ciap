package tz.go.tirdo.ciap.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.ciap.domain.billing.BillingService;
import tz.go.tirdo.ciap.domain.billing.QuotationService;
import tz.go.tirdo.ciap.domain.billing.dto.CreateQuotationRequest;
import tz.go.tirdo.ciap.domain.billing.dto.PaymentResponse;
import tz.go.tirdo.ciap.domain.billing.dto.QuotationItemDto;
import tz.go.tirdo.ciap.domain.billing.dto.QuotationResponse;
import tz.go.tirdo.ciap.domain.client.Client;
import tz.go.tirdo.ciap.domain.client.ClientService;
import tz.go.tirdo.ciap.domain.client.ClientType;
import tz.go.tirdo.ciap.domain.client.IndustryCategory;
import tz.go.tirdo.ciap.domain.client.dto.ClientRegistrationRequest;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyRequestService;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyType;
import tz.go.tirdo.ciap.domain.consultancy.dto.AssignDto;
import tz.go.tirdo.ciap.domain.consultancy.dto.CreateRequestDto;
import tz.go.tirdo.ciap.domain.consultancy.dto.RequestResponse;
import tz.go.tirdo.ciap.domain.consultancy.dto.ReviewDto;
import tz.go.tirdo.ciap.domain.consultant.Consultant;
import tz.go.tirdo.ciap.domain.consultant.ConsultantService;
import tz.go.tirdo.ciap.domain.consultant.ExpertCategory;
import tz.go.tirdo.ciap.domain.consultant.dto.CreateConsultantRequest;
import tz.go.tirdo.ciap.domain.department.DepartmentService;
import tz.go.tirdo.ciap.domain.knowledge.KnowledgeService;
import tz.go.tirdo.ciap.domain.knowledge.dto.CreateArticleRequest;
import tz.go.tirdo.ciap.domain.project.ProjectService;
import tz.go.tirdo.ciap.domain.project.TaskStatus;
import tz.go.tirdo.ciap.domain.project.dto.CreateProjectRequest;
import tz.go.tirdo.ciap.domain.project.dto.CreateTaskRequest;
import tz.go.tirdo.ciap.domain.project.dto.ProjectResponse;
import tz.go.tirdo.ciap.domain.project.dto.TaskResponse;
import tz.go.tirdo.ciap.domain.training.TrainingMode;
import tz.go.tirdo.ciap.domain.training.TrainingService;
import tz.go.tirdo.ciap.domain.training.dto.CreateEventRequest;
import tz.go.tirdo.ciap.domain.user.RoleName;
import tz.go.tirdo.ciap.domain.user.User;
import tz.go.tirdo.ciap.domain.user.UserRepository;
import tz.go.tirdo.ciap.domain.user.UserService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Loads a demonstration workspace the first time the Docker database has no
 * demo staff. Figures follow TIRDO's published services and the 18% VAT rate.
 * Client names and TINs are demonstration records, not live TRA registrations.
 */
@Component
@Profile("docker")
@Order(100)
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    static final String DEMO_PASSWORD = "TirdoDemo#2026";
    private static final String MARKER = "manager.consultancy@tirdo.go.tz";
    private static final String SWITCHBOARD = "+255222666034";
    private static final BigDecimal VAT = new BigDecimal("0.18");

    private final UserRepository userRepository;
    private final UserService userService;
    private final ClientService clientService;
    private final ConsultantService consultantService;
    private final ConsultancyRequestService requestService;
    private final QuotationService quotationService;
    private final BillingService billingService;
    private final ProjectService projectService;
    private final KnowledgeService knowledgeService;
    private final TrainingService trainingService;
    private final DepartmentService departmentService;

    public DemoDataSeeder(UserRepository userRepository,
                          UserService userService,
                          ClientService clientService,
                          ConsultantService consultantService,
                          ConsultancyRequestService requestService,
                          QuotationService quotationService,
                          BillingService billingService,
                          ProjectService projectService,
                          KnowledgeService knowledgeService,
                          TrainingService trainingService,
                          DepartmentService departmentService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.clientService = clientService;
        this.consultantService = consultantService;
        this.requestService = requestService;
        this.quotationService = quotationService;
        this.billingService = billingService;
        this.projectService = projectService;
        this.knowledgeService = knowledgeService;
        this.trainingService = trainingService;
        this.departmentService = departmentService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByEmail(MARKER)) {
            return;
        }
        try {
            seed();
            log.info("Demonstration workspace loaded for TIRDO consultancy, billing, and training.");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void seed() {
        User manager = staff(MARKER, "Neema Juma", RoleName.MANAGER);
        User finance = staff("finance.officer@tirdo.go.tz", "Joseph Mkenda", RoleName.FINANCE);
        Consultant ndt = expert("expert.ndt@tirdo.go.tz", "Eng. Baraka Mushi", RoleName.EXPERT,
                ExpertCategory.ENGINEER, "Ultrasonic testing, radiography, visual inspection of welds", 14, "180000");
        Consultant energy = expert("expert.energy@tirdo.go.tz", "Eng. Hassan Nwaka", RoleName.EXPERT,
                ExpertCategory.INDUSTRIAL_TECHNOLOGIST, "Energy audits, boiler and dryer performance, coal quality", 16, "160000");
        Consultant food = expert("expert.foodlab@tirdo.go.tz", "Dr. Rehema Lyimo", RoleName.EXPERT,
                ExpertCategory.SCIENTIST, "Edible oils, dairy microbiology, proximate analysis", 11, "150000");
        Consultant environment = expert("expert.environment@tirdo.go.tz", "Lilian Komba", RoleName.EXPERT,
                ExpertCategory.ENVIRONMENTAL_EXPERT, "Wastewater, air quality, environmental audit", 9, "140000");
        expert("expert.ict@tirdo.go.tz", "Daniel Shao", RoleName.EXPERT,
                ExpertCategory.ICT_SPECIALIST, "T-Hub software advisory and industrial ICT", 8, "130000");
        departmentService.enrollConsultants();

        Client sunflower = client("Mwanza Sunflower Processors Ltd", "900100001", ClientType.SME,
                IndustryCategory.FOOD_AND_BEVERAGE, "Grace Mtey", "+255282501140",
                "quality@mwanzasunflower.co.tz", "Mwanza", "Nyamagana industrial area, Mwanza");
        Client leather = client("Moshi Leather Works Ltd", "900100002", ClientType.INDUSTRY,
                IndustryCategory.TEXTILE, "Peter Shirima", "+255272754410",
                "plant@moshileather.co.tz", "Kilimanjaro", "Rau industrial area, Moshi");
        Client sisal = client("Tanga Sisal Products Ltd", "900100003", ClientType.INDUSTRY,
                IndustryCategory.AGRO_PROCESSING, "Amina Jongo", "+255272644118",
                "production@tangasisal.co.tz", "Tanga", "Pongwe sisal estate road, Tanga");
        Client steel = client("Kariakoo Steel Fabricators Ltd", "900100004", ClientType.SME,
                IndustryCategory.MANUFACTURING, "Said Mgaya", "+255222860441",
                "works@kariakoosteel.co.tz", "Dar es Salaam", "Chang'ombe, Dar es Salaam");
        Client palm = client("Kigoma Palm Oil Cooperative Society", "900100005", ClientType.SME,
                IndustryCategory.AGRO_PROCESSING, "Esther Ndayisaba", "+255282802215",
                "secretary@kigomapalm.co.tz", "Kigoma", "Mwanga, Kigoma");
        Client coal = client("Mbeya Highlands Coal Traders Ltd", "900100006", ClientType.INDUSTRY,
                IndustryCategory.ENERGY, "Frank Mwakyusa", "+255252500773",
                "lab@mbeyacoal.co.tz", "Mbeya", "Iyunga, Mbeya");
        Client dairy = client("Arusha Dairy Foods Ltd", "900100007", ClientType.INDUSTRY,
                IndustryCategory.FOOD_AND_BEVERAGE, "Naomi Mollel", "+255272545090",
                "qc@arushadairy.co.tz", "Arusha", "Themi industrial area, Arusha");
        Client water = client("Dodoma Urban Water Supply Authority", "900100008", ClientType.GOVERNMENT_INSTITUTION,
                IndustryCategory.ENVIRONMENTAL, "Eng. Paul Ngowi", "+255262324410",
                "dodoma.water@demo.tirdo.go.tz", "Dodoma", "Njedengwa oxidation ponds, Dodoma");

        as(sunflower.getEmail(), "CLIENT", () -> requestService.submit(new CreateRequestDto(
                sunflower.getId(), ConsultancyType.INDUSTRIAL,
                "Quality testing of sunflower oil and seed cake",
                "The Nyamagana mill needs chemical and microbiological testing of crude and refined sunflower oil, plus protein and moisture checks on seed cake sold as animal feed. Sampling will follow the edible-fats methods used by the TIRDO food laboratory.",
                LocalDate.now().plusWeeks(3))));

        as(leather.getEmail(), "CLIENT", () -> {
            RequestResponse submitted = requestService.submit(new CreateRequestDto(
                    leather.getId(), ConsultancyType.ENVIRONMENTAL,
                    "Effluent assessment for the Moshi tannery line",
                    "The tannery discharges to a municipal drain. TIRDO is asked to sample wastewater, advise on on-site treatment, and outline an environmental audit before the next production season.",
                    LocalDate.now().plusMonths(1)));
            asHod(ConsultancyType.ENVIRONMENTAL, () -> requestService.startReview(submitted.id()));
        });

        as(sisal.getEmail(), "CLIENT", () -> {
            RequestResponse submitted = requestService.submit(new CreateRequestDto(
                    sisal.getId(), ConsultancyType.INDUSTRIAL,
                    "Sisal fibre yield and dryer performance at Pongwe",
                    "The estate wants a process audit of decortication and the fibre dryer, including moisture and energy use, before buying a second dryer.",
                    LocalDate.now().plusMonths(2)));
            asHod(ConsultancyType.INDUSTRIAL, () -> {
                requestService.startReview(submitted.id());
                requestService.review(submitted.id(), new ReviewDto(true,
                        "Within the industrial-process mandate. A site visit at Pongwe can be scheduled in the next quarter."));
            });
            QuotationResponse quote = quotationService.create(new CreateQuotationRequest(
                    submitted.id(), VAT, LocalDate.now().plusDays(45),
                    "Site work at Pongwe, Tanga. Fees are in Tanzanian shillings. VAT is 18%.",
                    List.of(
                            item("Process audit of the decortication line", "5", "350000"),
                            item("Dryer performance test", "1", "1200000"),
                            item("Technical report and recommendations", "1", "800000"))));
            quotationService.send(quote.id());
        });

        as(steel.getEmail(), "CLIENT", () -> {
            RequestResponse submitted = requestService.submit(new CreateRequestDto(
                    steel.getId(), ConsultancyType.ENGINEERING,
                    "Non-destructive testing of welded storage tanks",
                    "Three welded mild-steel tanks at Chang'ombe need ultrasonic and visual inspection before they are put into fuel service. This matches TIRDO's published NDT service for tanks, pressure vessels, and pipelines.",
                    LocalDate.now().plusWeeks(2)));
            asHod(ConsultancyType.ENGINEERING, () -> {
                requestService.startReview(submitted.id());
                requestService.review(submitted.id(), new ReviewDto(true,
                        "NDT scope is clear: ultrasonic and visual inspection of three tanks, plus a written report."));
            });
            QuotationResponse first = quotationService.create(new CreateQuotationRequest(
                    submitted.id(), VAT, LocalDate.now().plusDays(20),
                    "First issue. Superseded after the client added a third tank.",
                    List.of(item("Ultrasonic inspection of two tanks", "2", "900000"))));
            quotationService.send(first.id());
            QuotationResponse second = quotationService.create(new CreateQuotationRequest(
                    submitted.id(), VAT, LocalDate.now().plusDays(30),
                    "Revised for three tanks at Chang'ombe. Includes visual inspection and the NDT report.",
                    List.of(
                            item("Ultrasonic testing of welded tanks", "3", "950000"),
                            item("Visual inspection and photographic record", "1", "450000"),
                            item("NDT report", "1", "600000"))));
            quotationService.send(second.id());
            billingService.issueControlNumber(submitted.id());
        });

        paidEngagement(palm, manager, finance, ConsultancyType.INDUSTRIAL,
                "Edible palm-oil quality and shelf-life advisory",
                "The cooperative sells crude palm oil in Kigoma. They need moisture, free-fatty-acid, and microbiology results, plus advice on storage before the oil moves to Dar es Salaam.",
                "Laboratory work at the TIRDO Complex, Kimweri Avenue, Msasani.",
                List.of(
                        item("Physicochemical analysis of palm oil", "12", "85000"),
                        item("Microbiological examination", "12", "70000"),
                        item("Storage and shelf-life advisory note", "1", "500000")),
                "GEPG-DEMO-KIGOMA-OIL",
                "Control number settled through the demonstration finance desk. No live GePG call was made.");

        Long coalRequest = delivered(coal, manager, finance, energy, ConsultancyType.INDUSTRIAL,
                "Proximate analysis of Mbeya coal samples",
                "Six samples from Mbinga and Kiwira need proximate analysis (moisture, ash, volatile matter, and fixed carbon) for the coal-quality record TIRDO maintains, plus a short note on boiler use.",
                "Coal laboratory, TIRDO Complex, Msasani.",
                List.of(
                        item("Proximate analysis, six samples", "6", "180000"),
                        item("Utilisation note for small boilers", "1", "700000")),
                "GEPG-DEMO-MBEYA-COAL",
                "Paid against the revised coal-analysis quotation.",
                false);

        ProjectResponse coalProject = projectService.createForRequest(coalRequest, new CreateProjectRequest(
                "Mbeya coal sample analysis",
                "Receive, prepare, and analyse six coal samples, then issue the utilisation note.",
                LocalDate.now().minusWeeks(2), LocalDate.now().plusWeeks(4), energy.getId()));
        projectService.start(coalProject.id());
        TaskResponse received = projectService.addTask(coalProject.id(), new CreateTaskRequest(
                "Log and prepare the six samples", "Crush and split the Kiwira and Mbinga bags.", energy.getId(), LocalDate.now().minusDays(3)));
        projectService.addTask(coalProject.id(), new CreateTaskRequest(
                "Complete proximate analysis and the boiler note", null, energy.getId(), LocalDate.now().plusWeeks(2)));
        projectService.updateTaskStatus(coalProject.id(), received.id(), TaskStatus.DONE);

        Long dairyRequest = delivered(dairy, manager, finance, food, ConsultancyType.INDUSTRIAL,
                "Microbiological quality of fresh milk and yoghurt",
                "Arusha Dairy Foods asks for total aerobic count, coliforms, E. coli, and Salmonella on fresh milk and plain yoghurt, using the parameters published for the TIRDO food laboratory.",
                "Food laboratory, TIRDO Complex, Msasani.",
                List.of(
                        item("Milk and yoghurt microbiology panel", "20", "95000"),
                        item("Interpretation and factory hygiene note", "1", "650000")),
                "GEPG-DEMO-ARUSHA-DAIRY",
                "Laboratory invoice settled before sample receipt.",
                true);
        requestService.submitReport(dairyRequest);

        Long waterRequest = delivered(water, manager, finance, environment, ConsultancyType.ENVIRONMENTAL,
                "Wastewater monitoring at the Njedengwa oxidation ponds",
                "Quarterly monitoring of effluent from the Njedengwa ponds: pH, BOD, COD, suspended solids, and a short environmental note for the utility's regulator file.",
                "Field sampling in Dodoma and analysis at the TIRDO environmental laboratory.",
                List.of(
                        item("Field sampling visit, Dodoma", "2", "750000"),
                        item("Wastewater analysis panel", "8", "120000"),
                        item("Environmental monitoring note", "1", "900000")),
                "GEPG-DEMO-DODOMA-WATER",
                "Government utility settled the control number before the field visit.",
                true);
        ProjectResponse waterProject = projectService.getForRequest(waterRequest);
        projectService.complete(waterProject.id());
        requestService.submitReport(waterRequest);
        requestService.close(waterRequest);

        as(steel.getEmail(), "CLIENT", () -> {
            RequestResponse patent = requestService.submit(new CreateRequestDto(
                    steel.getId(), ConsultancyType.ICT,
                    "Filing a trademark for the Kariakoo tank brand",
                    "The fabricator asked TIRDO to file a trademark. Trademark registration is handled by BRELA, not by TIRDO's industrial-research mandate.",
                    null));
            asHod(ConsultancyType.ICT, () -> {
                requestService.startReview(patent.id());
                requestService.review(patent.id(), new ReviewDto(false,
                        "Outside TIRDO's mandate. The client should use BRELA for trademark filing. TIRDO can still support the NDT work already quoted."));
            });
        });

        as(sisal.getEmail(), "CLIENT", () -> {
            RequestResponse postponed = requestService.submit(new CreateRequestDto(
                    sisal.getId(), ConsultancyType.ENGINEERING,
                    "Design of a new sisal decorticator",
                    "The estate asked for a full machine design. They have since postponed the capital project to the next financial year.",
                    null));
            asHod(ConsultancyType.ENGINEERING, () -> {
                requestService.startReview(postponed.id());
                requestService.cancel(postponed.id(),
                        "The estate postponed the capital project to the next financial year.");
            });
        });

        knowledge();
        training();
    }

    private Long delivered(Client client, User manager, User finance, Consultant consultant,
                           ConsultancyType type, String title, String description, String quoteNotes,
                           List<QuotationItemDto> items, String gepgRef, String reason, boolean startProject) {
        RequestResponse submitted = asResult(client.getEmail(), "CLIENT", () -> requestService.submit(new CreateRequestDto(
                client.getId(), type, title, description, LocalDate.now().minusWeeks(6))));
        asHod(type, () -> {
            requestService.startReview(submitted.id());
            requestService.review(submitted.id(), new ReviewDto(true, "Scope accepted. Quotation to follow."));
        });
        QuotationResponse quote = quotationService.create(new CreateQuotationRequest(
                submitted.id(), VAT, LocalDate.now().plusDays(21), quoteNotes, items));
        quotationService.send(quote.id());
        quotationService.accept(quote.id());
        PaymentResponse payment = billingService.issueControlNumber(submitted.id());
        as(finance.getEmail(), "FINANCE", () ->
                billingService.handleCallback(payment.controlNumber(), gepgRef, reason));
        asHod(type, () -> requestService.assign(submitted.id(), new AssignDto(consultant.getId())));
        as(manager.getEmail(), "MANAGER", () -> requestService.startExecution(submitted.id()));
        if (startProject) {
            ProjectResponse project = projectService.createForRequest(submitted.id(), new CreateProjectRequest(
                    title, description, LocalDate.now().minusWeeks(4), LocalDate.now().plusWeeks(2), consultant.getId()));
            projectService.start(project.id());
            TaskResponse task = projectService.addTask(project.id(), new CreateTaskRequest(
                    "Field and laboratory work", description, consultant.getId(), LocalDate.now().minusDays(2)));
            projectService.updateTaskStatus(project.id(), task.id(), TaskStatus.DONE);
        }
        return submitted.id();
    }

    private void paidEngagement(Client client, User manager, User finance, ConsultancyType type,
                                String title, String description, String quoteNotes,
                                List<QuotationItemDto> items, String gepgRef, String reason) {
        RequestResponse submitted = asResult(client.getEmail(), "CLIENT", () -> requestService.submit(new CreateRequestDto(
                client.getId(), type, title, description, LocalDate.now().plusWeeks(5))));
        asHod(type, () -> {
            requestService.startReview(submitted.id());
            requestService.review(submitted.id(), new ReviewDto(true, "Food-laboratory scope accepted."));
        });
        QuotationResponse quote = quotationService.create(new CreateQuotationRequest(
                submitted.id(), VAT, LocalDate.now().plusDays(30), quoteNotes, items));
        quotationService.send(quote.id());
        quotationService.accept(quote.id());
        PaymentResponse payment = billingService.issueControlNumber(submitted.id());
        as(finance.getEmail(), "FINANCE", () ->
                billingService.handleCallback(payment.controlNumber(), gepgRef, reason));
    }

    private void knowledge() {
        knowledgeService.create(new CreateArticleRequest(
                "Tanzania Industrial Research and Development Organisation",
                "About TIRDO",
                "TIRDO is the national industrial research organisation under the Ministry of Industry and Trade.",
                """
                Tanzania Industrial Research and Development Organisation (TIRDO) was established by Act of Parliament No. 5 of 1979 and became operational on 1 April 1979. It is a government institution under the Ministry of Industry and Trade.

                Its mandate is to assist Tanzanian industry with technical expertise, applied research, and technology services, including the use of local materials and the reduction of industrial pollution.

                Visit or write to the TIRDO Complex, Kimweri Avenue, Msasani, P.O. Box 23235, Dar es Salaam. Switchboard: +255 22 2666034 or +255 22 2668822. Email: info@tirdo.or.tz.
                """,
                true));
        knowledgeService.create(new CreateArticleRequest(
                "Non-destructive testing for tanks, vessels, and pipelines",
                "Engineering",
                "TIRDO uses NDT to check welds and castings without cutting the component.",
                """
                The engineering workshops offer radiographic, ultrasonic, magnetic-particle, liquid-penetrant, and visual inspection. Typical jobs are welded oil tanks, pressure and marine vessels, gas and oil pipelines, refinery piping, and cast industrial parts.

                A consultancy request should name the equipment, the site, and the inspection method required. The quotation is in Tanzanian shillings and carries VAT at 18%.
                """,
                true));
        knowledgeService.create(new CreateArticleRequest(
                "Energy efficiency and coal quality",
                "Energy",
                "Audits cover ovens, cookstoves, tobacco barns, solar dryers, and coal samples.",
                """
                TIRDO audits energy use in factories, commercial buildings, and households, and tests the performance of ovens, cookstoves, tobacco barns, and solar dryers.

                The coal laboratory carries out proximate and ultimate analysis, records coking behaviour, and keeps a coal-quality record for Tanzanian deposits. It also looks at coal-bed methane where a deposit may support it.
                """,
                true));
        knowledgeService.create(new CreateArticleRequest(
                "Food, environment, and chemical laboratories",
                "Laboratories",
                "Accredited food tests and environmental monitoring are offered from Msasani.",
                """
                The food laboratory tests products of plant and animal origin, including fish, cereals, meat, edible oils, dairy, honey, and fruit. Accredited work includes total aerobic count, Staphylococcus aureus, coliforms, E. coli, and Salmonella.

                The environmental laboratory covers wastewater, air quality, noise, light, and environmental audits. The chemistry laboratory tests raw materials, industrial products, salt, fertiliser, and heavy metals.
                """,
                true));
        knowledgeService.create(new CreateArticleRequest(
                "How a consultancy moves from request to closure",
                "Clients",
                "A request is reviewed, quoted, paid, assigned, delivered, and closed.",
                """
                Submit the request with the industrial problem, the site, and the date you hope to start. The Head of Department for that service line reviews the scope. If it is within the mandate, you receive a quotation in Tanzanian shillings.

                After the control number is settled, the Head of Department assigns an expert and the work is carried out. The engagement closes when the report is accepted. Requests that belong to another authority, such as trademark filing at BRELA, are declined with a reason.
                """,
                true));
    }

    private void training() {
        trainingService.create(new CreateEventRequest(
                "Non-destructive testing of welded tanks",
                "Two-day workshop on ultrasonic and visual inspection, held in the engineering bays at the TIRDO Complex, Kimweri Avenue, Msasani.",
                TrainingMode.IN_PERSON,
                "TIRDO Complex, Kimweri Avenue, Msasani, Dar es Salaam",
                Instant.parse("2026-11-18T06:00:00Z"),
                Instant.parse("2026-11-19T14:00:00Z"),
                24,
                true));
        trainingService.create(new CreateEventRequest(
                "Energy audit methods for factory engineers",
                "How to measure dryer, boiler, and lighting loads and turn the readings into a costed recommendation. Hybrid: the morning is at Msasani and the afternoon is online.",
                TrainingMode.HYBRID,
                "TIRDO Complex, Kimweri Avenue, Msasani, and online",
                Instant.parse("2026-12-02T06:00:00Z"),
                Instant.parse("2026-12-02T14:00:00Z"),
                40,
                true));
        trainingService.create(new CreateEventRequest(
                "Sampling milk, oils, and cereals for laboratory testing",
                "Practical sampling for the food-laboratory panels: aerobic count, coliforms, E. coli, and Salmonella, plus moisture and free fatty acids in edible oils.",
                TrainingMode.IN_PERSON,
                "Food laboratory, TIRDO Complex, Kimweri Avenue, Msasani",
                Instant.parse("2027-01-21T06:00:00Z"),
                Instant.parse("2027-01-21T13:00:00Z"),
                18,
                true));
    }

    private User staff(String email, String name, RoleName role) {
        return userService.createUser(email, DEMO_PASSWORD, name, SWITCHBOARD, Set.of(role));
    }

    private Consultant expert(String email, String name, RoleName role, ExpertCategory category,
                              String skills, int years, String hourlyRate) {
        User user = staff(email, name, role);
        return consultantService.create(new CreateConsultantRequest(
                name, category, skills, years, new BigDecimal(hourlyRate), user.getId()));
    }

    private Client client(String company, String tin, ClientType type, IndustryCategory industry,
                          String contact, String phone, String email, String region, String address) {
        return clientService.register(new ClientRegistrationRequest(
                company, tin, type, industry, contact, phone, email, DEMO_PASSWORD,
                region, address, "Tanzania"));
    }

    private static QuotationItemDto item(String description, String quantity, String unitPrice) {
        return new QuotationItemDto(description, new BigDecimal(quantity), new BigDecimal(unitPrice));
    }

    private void asHod(ConsultancyType type, Runnable action) {
        as(departmentService.requireHodEmail(type), "HOD", action);
    }

    private void as(String email, String role, Runnable action) {
        asResult(email, role, () -> {
            action.run();
            return null;
        });
    }

    private <T> T asResult(String email, String role, java.util.function.Supplier<T> action) {
        var previous = SecurityContextHolder.getContext().getAuthentication();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                email.toLowerCase(), null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
        try {
            return action.get();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previous);
        }
    }
}
