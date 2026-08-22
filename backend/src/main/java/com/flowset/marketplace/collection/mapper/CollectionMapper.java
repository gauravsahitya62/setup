package com.flowset.marketplace.collection.mapper;

import com.flowset.marketplace.collection.dto.CollectionItemResponse;
import com.flowset.marketplace.collection.dto.CollectionResponse;
import com.flowset.marketplace.collection.entity.ProductCollectionEntity;
import com.flowset.marketplace.collection.entity.ProductCollectionItemEntity;
import org.springframework.stereotype.Component;

@Component
public class CollectionMapper {

    public CollectionResponse toResponse(ProductCollectionEntity collection) {
        return new CollectionResponse(
                collection.getId(),
                collection.getVendor().getId(),
                collection.getName(),
                collection.getSlug(),
                collection.getDescription(),
                collection.getFeaturedImage(),
                collection.getStatus(),
                collection.getCreatedAt(),
                collection.getUpdatedAt());
    }

    public CollectionItemResponse toItemResponse(ProductCollectionItemEntity item) {
        return new CollectionItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getSlug(),
                item.getDisplayOrder());
    }
}
