package com.flowset.marketplace.product.mapper;

import com.flowset.marketplace.product.dto.AttributeResponse;
import com.flowset.marketplace.product.dto.ImageResponse;
import com.flowset.marketplace.product.dto.ProductResponse;
import com.flowset.marketplace.product.dto.ProductSummaryResponse;
import com.flowset.marketplace.product.dto.VariantResponse;
import com.flowset.marketplace.product.entity.ProductAttributeEntity;
import com.flowset.marketplace.product.entity.ProductEntity;
import com.flowset.marketplace.product.entity.ProductImageEntity;
import com.flowset.marketplace.product.entity.ProductTagEntity;
import com.flowset.marketplace.product.entity.ProductVariantEntity;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toResponse(
            ProductEntity product,
            List<ProductVariantEntity> variants,
            List<ProductImageEntity> images,
            List<ProductAttributeEntity> attributes
    ) {
        return new ProductResponse(
                product.getId(),
                product.getVendor().getId(),
                product.getVendor().getCompanyName(),
                product.getVendor().getSlug(),
                product.getVendor().isVerified(),
                product.getVendor().getRating(),
                product.getVendor().getReviewCount(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getCategory().getSlug(),
                product.getSubcategory() == null ? null : product.getSubcategory().getId(),
                product.getSubcategory() == null ? null : product.getSubcategory().getName(),
                product.getName(),
                product.getSlug(),
                product.getShortDescription(),
                product.getDescription(),
                product.getFeaturedImage(),
                product.getPricePerDay(),
                product.getSecurityDeposit(),
                product.getMinimumRentalDays(),
                product.isDeliveryAvailable(),
                product.isSetupAvailable(),
                product.isPickupAvailable(),
                product.getStatus(),
                product.isFeatured(),
                variants.stream().map(this::toVariant).toList(),
                images.stream().map(this::toImage).toList(),
                attributes.stream().map(this::toAttribute).toList(),
                product.getTags().stream().map(ProductTagEntity::getName).sorted().toList(),
                product.getViewCount(),
                product.getModerationReason(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }

    public ProductSummaryResponse toSummary(ProductEntity product) {
        var vendor = product.getVendor();
        var category = product.getCategory();
        String city = null;
        if (org.hibernate.Hibernate.isInitialized(vendor.getServiceAreas())
                && vendor.getServiceAreas() != null
                && !vendor.getServiceAreas().isEmpty()) {
            city = vendor.getServiceAreas().get(0).getCity();
        }
        boolean available = true;
        if (org.hibernate.Hibernate.isInitialized(product.getVariants())
                && product.getVariants() != null
                && !product.getVariants().isEmpty()) {
            available = product.getVariants().stream().anyMatch(v ->
                    v.getStatus() == com.flowset.common.VariantStatus.ACTIVE && v.getAvailableQuantity() > 0);
        }

        return new ProductSummaryResponse(
                product.getId(),
                vendor.getId(),
                vendor.getCompanyName(),
                vendor.getSlug(),
                category.getId(),
                category.getName(),
                category.getSlug(),
                product.getName(),
                product.getSlug(),
                product.getShortDescription(),
                product.getFeaturedImage(),
                product.getPricePerDay(),
                product.getStatus(),
                product.isFeatured(),
                vendor.isVerified(),
                vendor.getRating(),
                vendor.getReviewCount(),
                city,
                available,
                product.getModerationReason(),
                product.getCreatedAt());
    }

    public VariantResponse toVariant(ProductVariantEntity variant) {
        return new VariantResponse(
                variant.getId(),
                variant.getProduct().getId(),
                variant.getVariantName(),
                variant.getSku(),
                variant.getColor(),
                variant.getMaterial(),
                variant.getQuantity(),
                variant.getAvailableQuantity(),
                variant.getPricePerDay(),
                variant.getStatus());
    }

    public ImageResponse toImage(ProductImageEntity image) {
        return new ImageResponse(
                image.getId(), image.getImageUrl(), image.getAltText(), image.getDisplayOrder(), image.isFeatured());
    }

    public AttributeResponse toAttribute(ProductAttributeEntity attribute) {
        return new AttributeResponse(
                attribute.getId(), attribute.getAttributeName(), attribute.getAttributeValue(), attribute.getDisplayOrder());
    }
}
