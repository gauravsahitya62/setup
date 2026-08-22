package com.flowset.marketplace.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.flowset.common.ProductStatus;
import com.flowset.common.RoleName;
import com.flowset.common.UserStatus;
import com.flowset.common.VendorStatus;
import com.flowset.marketplace.category.entity.CategoryEntity;
import com.flowset.marketplace.category.repository.CategoryRepository;
import com.flowset.marketplace.product.entity.ProductEntity;
import com.flowset.marketplace.product.repository.ProductRepository;
import com.flowset.users.entity.UserEntity;
import com.flowset.users.repository.UserRepository;
import com.flowset.vendor.entity.VendorEntity;
import com.flowset.vendor.repository.VendorRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductSpecificationsTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void keywordAndPriceAndCategorySpecificationsFilterCorrectly() {
        VendorEntity vendor = createVendor("spec.vendor@" + UUID.randomUUID() + ".example.com", "Spec Vendor");
        CategoryEntity category = createCategory("Spec Category " + UUID.randomUUID());
        CategoryEntity otherCategory = createCategory("Other Category " + UUID.randomUUID());

        ProductEntity cheapChair = createProduct(vendor, category, "Folding Chair", new BigDecimal("5.00"));
        ProductEntity expensiveChair = createProduct(vendor, category, "Throne Chair", new BigDecimal("95.00"));
        ProductEntity otherCategoryProduct = createProduct(vendor, otherCategory, "Chair Lamp", new BigDecimal("40.00"));
        ProductEntity draftProduct = createProduct(vendor, category, "Draft Chair", new BigDecimal("10.00"));
        draftProduct.setStatus(ProductStatus.DRAFT);
        productRepository.save(draftProduct);

        var spec = ProductSpecifications.allOf(
                ProductSpecifications.notDeleted(),
                ProductSpecifications.isPublished(),
                ProductSpecifications.keyword("chair"),
                ProductSpecifications.categoryId(category.getId()),
                ProductSpecifications.priceBetween(new BigDecimal("1.00"), new BigDecimal("50.00")));

        Page<ProductEntity> result = productRepository.findAll(spec, PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(ProductEntity::getId).containsExactly(cheapChair.getId());
        assertThat(result.getContent()).extracting(ProductEntity::getId)
                .doesNotContain(expensiveChair.getId(), otherCategoryProduct.getId(), draftProduct.getId());
    }

    @Test
    void verifiedVendorAndMinRatingSpecificationsFilterCorrectly() {
        VendorEntity verifiedVendor = createVendor("verified@" + UUID.randomUUID() + ".example.com", "Verified Vendor");
        verifiedVendor.setVerificationStatus(com.flowset.common.VendorVerificationStatus.VERIFIED);
        verifiedVendor.setRating(new BigDecimal("4.80"));
        vendorRepository.save(verifiedVendor);

        VendorEntity unverifiedVendor = createVendor("unverified@" + UUID.randomUUID() + ".example.com", "Unverified Vendor");
        unverifiedVendor.setVerified(false);
        unverifiedVendor.setRating(new BigDecimal("3.00"));
        vendorRepository.save(unverifiedVendor);

        CategoryEntity category = createCategory("Rating Category " + UUID.randomUUID());
        ProductEntity fromVerified = createProduct(verifiedVendor, category, "Verified Tent", new BigDecimal("30.00"));
        ProductEntity fromUnverified = createProduct(unverifiedVendor, category, "Unverified Tent", new BigDecimal("30.00"));

        var spec = ProductSpecifications.allOf(
                ProductSpecifications.notDeleted(),
                ProductSpecifications.isPublished(),
                ProductSpecifications.verifiedVendor(true),
                ProductSpecifications.minRating(new BigDecimal("4.00")));

        Page<ProductEntity> result = productRepository.findAll(spec, PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(ProductEntity::getId).containsExactly(fromVerified.getId());
        assertThat(result.getContent()).extracting(ProductEntity::getId).doesNotContain(fromUnverified.getId());
    }

    private VendorEntity createVendor(String email, String companyName) {
        UserEntity owner = new UserEntity();
        owner.setFirstName("Vendor");
        owner.setLastName("Owner");
        owner.setEmail(email);
        owner.setPasswordHash("hash");
        owner.setRole(RoleName.VENDOR);
        owner.setStatus(UserStatus.ACTIVE);
        owner.setEmailVerified(true);
        UserEntity savedOwner = userRepository.save(owner);

        VendorEntity vendor = new VendorEntity();
        vendor.setOwnerUserId(savedOwner.getId());
        vendor.setCompanyName(companyName);
        vendor.setSlug(companyName.toLowerCase().replaceAll("[^a-z0-9]+", "-") + "-" + UUID.randomUUID());
        vendor.setStatus(VendorStatus.PUBLISHED);
        return vendorRepository.save(vendor);
    }

    private CategoryEntity createCategory(String name) {
        CategoryEntity category = new CategoryEntity();
        category.setName(name);
        category.setSlug(name.toLowerCase().replaceAll("[^a-z0-9]+", "-"));
        return categoryRepository.save(category);
    }

    private ProductEntity createProduct(VendorEntity vendor, CategoryEntity category, String name, BigDecimal price) {
        ProductEntity product = new ProductEntity();
        product.setVendor(vendor);
        product.setCategory(category);
        product.setName(name);
        product.setSlug(name.toLowerCase().replaceAll("[^a-z0-9]+", "-") + "-" + UUID.randomUUID());
        product.setPricePerDay(price);
        product.setStatus(ProductStatus.PUBLISHED);
        return productRepository.save(product);
    }
}
