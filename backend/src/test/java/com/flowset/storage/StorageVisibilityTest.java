package com.flowset.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StorageVisibilityTest {

    @Test
    void classifiesPublicProductImages() {
        assertThat(StorageVisibility.isPublic("products/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/images/x.jpg"))
                .isTrue();
        assertThat(StorageVisibility.isPublic("vendors/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/documents/gst.pdf"))
                .isFalse();
    }

    @Test
    void extractsOwnerIdsFromPrivatePaths() {
        String vendorId = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
        assertThat(StorageVisibility.vendorIdFromDocumentPath("vendors/" + vendorId + "/documents/pan.pdf"))
                .isEqualTo(vendorId);
        assertThat(StorageVisibility.projectIdFromPath("projects/cccccccc-cccc-cccc-cccc-cccccccccccc/files/a.txt"))
                .isEqualTo("cccccccc-cccc-cccc-cccc-cccccccccccc");
        assertThat(StorageVisibility.orderIdFromPath("orders/dddddddd-dddd-dddd-dddd-dddddddddddd/delivery-proof/x.jpg"))
                .isEqualTo("dddddddd-dddd-dddd-dddd-dddddddddddd");
    }
}
