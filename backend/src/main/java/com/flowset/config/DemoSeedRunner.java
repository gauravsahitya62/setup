package com.flowset.config;

import com.flowset.common.EventType;
import com.flowset.common.ProductStatus;
import com.flowset.common.ProjectStatus;
import com.flowset.common.RoleName;
import com.flowset.common.UserStatus;
import com.flowset.common.VendorStatus;
import com.flowset.common.VendorVerificationStatus;
import com.flowset.marketplace.category.entity.CategoryEntity;
import com.flowset.marketplace.category.repository.CategoryRepository;
import com.flowset.marketplace.product.dto.ProductRequest;
import com.flowset.marketplace.product.dto.ProductResponse;
import com.flowset.marketplace.product.dto.VariantRequest;
import com.flowset.marketplace.product.service.ProductService;
import com.flowset.marketplace.product.service.ProductVariantService;
import com.flowset.planner.booking.dto.CreateBookingRequest;
import com.flowset.planner.booking.service.BookingRequestService;
import com.flowset.planner.projects.dto.AddProjectProductRequest;
import com.flowset.planner.projects.dto.CreateProjectRequest;
import com.flowset.planner.projects.dto.ProjectResponse;
import com.flowset.planner.projects.service.ProjectProductService;
import com.flowset.planner.projects.service.ProjectService;
import com.flowset.security.UserPrincipal;
import com.flowset.users.entity.PlannerProfileEntity;
import com.flowset.users.entity.UserEntity;
import com.flowset.users.entity.VendorProfileEntity;
import com.flowset.users.repository.PlannerProfileRepository;
import com.flowset.users.repository.UserRepository;
import com.flowset.users.repository.VendorProfileRepository;
import com.flowset.vendor.entity.VendorEntity;
import com.flowset.vendor.repository.VendorRepository;
import com.flowset.vendor.service.VendorService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Development/staging catalog only. Refused in production by {@link ProductionSafetyStartup}.
 * Enable with DEMO_SEED_ENABLED=true. Idempotent on {@code demo.planner.1@flowset.local}.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "flowset.demo-seed", name = "enabled", havingValue = "true")
public class DemoSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoSeedRunner.class);
    private static final String MARKER_EMAIL = "demo.planner.1@flowset.local";
    private static final LocalDate EVENT = LocalDate.of(2026, 12, 12);
    private static final LocalDate START = LocalDate.of(2026, 12, 12);
    private static final LocalDate END = LocalDate.of(2026, 12, 14);

    private static final String[][] PLANNERS = {
        {"Maya", "Sharma", "Northline Events"},
        {"Arjun", "Patel", "Lotus Weddings"},
        {"Priya", "Kapoor", "Saffron Occasions"},
        {"Rohan", "Mehta", "Harbor Planning"},
        {"Ananya", "Iyer", "Canvas Events"}
    };

    private static final String[] VENDORS = {
        "Grand Palace Rentals",
        "Lumen Event Hire",
        "Harbor Seating Co",
        "Marigold Decor",
        "Skyline Tents",
        "Peak Audio Visual",
        "Silk Table Studio",
        "Royal Mandap Hire",
        "Oasis Lounge Rentals",
        "Northline Lighting"
    };

    private static final CatalogItem[] CATALOG = {
        new CatalogItem("Gold Chiavari Chair", "furniture", "Gold", new BigDecimal("500.00"), 100),
        new CatalogItem("Banquet Round Table", "furniture", "72 inch", new BigDecimal("350.00"), 40),
        new CatalogItem("Warm String Lights", "lighting", "20m", new BigDecimal("180.00"), 60),
        new CatalogItem("Floral Backdrop", "decor", "Ivory", new BigDecimal("2200.00"), 8),
        new CatalogItem("Clear Marquee Tent", "tents-structures", "12x12", new BigDecimal("8500.00"), 4)
    };

    private final DemoSeedProperties properties;
    private final AppEnvironmentProperties app;
    private final UserRepository userRepository;
    private final PlannerProfileRepository plannerProfileRepository;
    private final VendorProfileRepository vendorProfileRepository;
    private final VendorRepository vendorRepository;
    private final CategoryRepository categoryRepository;
    private final VendorService vendorService;
    private final ProductService productService;
    private final ProductVariantService variantService;
    private final ProjectService projectService;
    private final ProjectProductService projectProductService;
    private final BookingRequestService bookingRequestService;
    private final PasswordEncoder passwordEncoder;

    public DemoSeedRunner(
            DemoSeedProperties properties,
            AppEnvironmentProperties app,
            UserRepository userRepository,
            PlannerProfileRepository plannerProfileRepository,
            VendorProfileRepository vendorProfileRepository,
            VendorRepository vendorRepository,
            CategoryRepository categoryRepository,
            VendorService vendorService,
            ProductService productService,
            ProductVariantService variantService,
            ProjectService projectService,
            ProjectProductService projectProductService,
            BookingRequestService bookingRequestService,
            PasswordEncoder passwordEncoder
    ) {
        this.properties = properties;
        this.app = app;
        this.userRepository = userRepository;
        this.plannerProfileRepository = plannerProfileRepository;
        this.vendorProfileRepository = vendorProfileRepository;
        this.vendorRepository = vendorRepository;
        this.categoryRepository = categoryRepository;
        this.vendorService = vendorService;
        this.productService = productService;
        this.variantService = variantService;
        this.projectService = projectService;
        this.projectProductService = projectProductService;
        this.bookingRequestService = bookingRequestService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        String env = app.env() == null ? "development" : app.env().toLowerCase();
        if ("production".equals(env)) {
            throw new IllegalStateException("Demo seed cannot run in production");
        }
        if (userRepository.existsActiveByEmail(MARKER_EMAIL)) {
            log.info("Demo seed skipped; {} already exists", MARKER_EMAIL);
            return;
        }

        log.info("Seeding FlowSet demo catalog env={}", env);
        UserEntity[] planners = new UserEntity[PLANNERS.length];
        for (int i = 0; i < PLANNERS.length; i++) {
            planners[i] = createPlanner(i);
        }
        UserEntity[] vendorUsers = new UserEntity[VENDORS.length];
        UUID firstVariantId = null;
        UUID firstVendorId = null;
        for (int i = 0; i < VENDORS.length; i++) {
            vendorUsers[i] = createVendor(i);
            VendorEntity vendor = vendorRepository
                    .findActiveByOwnerUserId(vendorUsers[i].getId())
                    .orElseThrow();
            vendor.setStatus(VendorStatus.PUBLISHED);
            vendor.setVerificationStatus(VendorVerificationStatus.VERIFIED);
            vendor.setShortDescription("Demo rental catalog for FlowSet staging.");
            vendorRepository.save(vendor);
            vendorProfileRepository.findByUserId(vendorUsers[i].getId()).ifPresent(profile -> {
                profile.setVerificationStatus(VendorVerificationStatus.VERIFIED);
                vendorProfileRepository.save(profile);
            });
            UUID firstForVendor = seedCatalog(vendorUsers[i], i);
            if (i == 0) {
                firstVariantId = firstForVendor;
                firstVendorId = vendor.getId();
            }
        }

        ProjectResponse project = asUser(planners[0], () -> projectService.create(new CreateProjectRequest(
                "Sharma Wedding",
                EventType.WEDDING,
                "Pilot workspace for the Sharma wedding.",
                EVENT,
                null,
                null,
                "Example Venue",
                "Example Venue Road",
                "Jaipur",
                "Rajasthan",
                "India",
                300,
                ProjectStatus.PLANNING)));
        if (firstVariantId != null) {
            UUID variantId = firstVariantId;
            asUser(planners[0], () -> projectProductService.add(
                    project.id(), new AddProjectProductRequest(variantId, 10, "Gold chairs near the mandap")));
        }
        for (int i = 1; i < planners.length; i++) {
            int index = i;
            asUser(planners[i], () -> projectService.create(new CreateProjectRequest(
                    PLANNERS[index][2] + " event",
                    EventType.WEDDING,
                    "Demo project",
                    EVENT.plusDays(index),
                    null,
                    null,
                    "Example Venue",
                    null,
                    "Jaipur",
                    "Rajasthan",
                    "India",
                    150 + index * 20,
                    ProjectStatus.DRAFT)));
        }

        if (firstVariantId != null && firstVendorId != null) {
            UUID variantId = firstVariantId;
            UUID vendorId = firstVendorId;
            var created = asUser(planners[0], () -> bookingRequestService.create(new CreateBookingRequest(
                    project.id(),
                    vendorId,
                    List.of(new CreateBookingRequest.Item(variantId, 10, null)),
                    EVENT,
                    START,
                    END,
                    "Example Venue",
                    "Example Venue Road",
                    "Jaipur",
                    "Rajasthan",
                    "Pilot booking request",
                    new CreateBookingRequest.Delivery(
                            START,
                            null,
                            "Example Venue",
                            "Example Venue Road",
                            "Jaipur",
                            "Rajasthan",
                            "Maya Sharma",
                            "9999999999",
                            null,
                            END,
                            null))));
            asUser(vendorUsers[0], () -> bookingRequestService.accept(created.id()));
        }

        log.info(
                "Demo seed complete. Planners demo.planner.1-5@flowset.local, vendors demo.vendor.1-10@flowset.local, password from DEMO_SEED_PASSWORD");
    }

    private UUID seedCatalog(UserEntity vendorUser, int vendorIndex) {
        UUID firstVariant = null;
        for (int i = 0; i < CATALOG.length; i++) {
            CatalogItem item = CATALOG[i];
            boolean featured = i == 0;
            CategoryEntity category = categoryRepository
                    .findBySlugIgnoreCase(item.categorySlug())
                    .orElseThrow(() -> new IllegalStateException("Missing category " + item.categorySlug()));
            ProductResponse product = asUser(vendorUser, () -> productService.create(new ProductRequest(
                    null,
                    item.name(),
                    null,
                    category.getId(),
                    null,
                    item.name() + " for event hire",
                    "Demo product for the FlowSet pilot catalog.",
                    item.price(),
                    BigDecimal.ZERO,
                    1,
                    true,
                    true,
                    true,
                    featured,
                    ProductStatus.PUBLISHED,
                    null)));
            String sku = "DEMO-" + (vendorIndex + 1) + "-" + (i + 1);
            var variant = asUser(vendorUser, () -> variantService.create(
                    product.id(),
                    new VariantRequest(item.variant(), sku, item.variant(), null, item.quantity(), null, null)));
            if (firstVariant == null) {
                firstVariant = variant.id();
            }
        }
        return firstVariant;
    }

    private UserEntity createPlanner(int index) {
        String[] row = PLANNERS[index];
        UserEntity user = newUser(row[0], row[1], "demo.planner." + (index + 1) + "@flowset.local", RoleName.PLANNER);
        PlannerProfileEntity profile = new PlannerProfileEntity();
        profile.setUser(user);
        profile.setCompanyName(row[2]);
        plannerProfileRepository.save(profile);
        return user;
    }

    private UserEntity createVendor(int index) {
        String business = VENDORS[index];
        UserEntity user = newUser("Vendor", String.valueOf(index + 1), "demo.vendor." + (index + 1) + "@flowset.local", RoleName.VENDOR);
        VendorProfileEntity profile = new VendorProfileEntity();
        profile.setUser(user);
        profile.setBusinessName(business);
        profile.setVerificationStatus(VendorVerificationStatus.PENDING);
        vendorProfileRepository.save(profile);
        vendorService.createDraftForOwner(user.getId(), business, user.getEmail());
        return user;
    }

    private UserEntity newUser(String first, String last, String email, RoleName role) {
        UserEntity user = new UserEntity();
        user.setFirstName(first);
        user.setLastName(last);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(properties.password()));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    private <T> T asUser(UserEntity user, SeedAction<T> action) {
        UserPrincipal principal = new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole(),
                user.getStatus(),
                user.isEmailVerified());
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            return action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @FunctionalInterface
    private interface SeedAction<T> {
        T run();
    }

    private record CatalogItem(String name, String categorySlug, String variant, BigDecimal price, int quantity) {
    }
}
