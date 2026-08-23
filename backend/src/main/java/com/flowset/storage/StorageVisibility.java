package com.flowset.storage;

import java.util.Locale;
import java.util.regex.Pattern;

public final class StorageVisibility {

    private static final Pattern PUBLIC = Pattern.compile(
            "^(vendors/[0-9a-f-]{36}/(logo|banner|portfolio)/|products/[0-9a-f-]{36}/images/|categories/[0-9a-f-]{36}/).+");

    private static final Pattern VENDOR_DOCUMENT = Pattern.compile("^vendors/([0-9a-f-]{36})/documents/.+");
    private static final Pattern PROJECT_FILE = Pattern.compile("^projects/([0-9a-f-]{36})/.+");
    private static final Pattern ORDER_FILE = Pattern.compile("^orders/([0-9a-f-]{36})/.+");

    private StorageVisibility() {
    }

    public static boolean isPublic(String relativePath) {
        return relativePath != null && PUBLIC.matcher(normalize(relativePath)).matches();
    }

    public static boolean isVendorDocument(String relativePath) {
        return relativePath != null && VENDOR_DOCUMENT.matcher(normalize(relativePath)).matches();
    }

    public static String vendorIdFromDocumentPath(String relativePath) {
        var matcher = VENDOR_DOCUMENT.matcher(normalize(relativePath));
        if (!matcher.matches()) {
            return null;
        }
        return matcher.group(1);
    }

    public static String projectIdFromPath(String relativePath) {
        var matcher = PROJECT_FILE.matcher(normalize(relativePath));
        return matcher.matches() ? matcher.group(1) : null;
    }

    public static String orderIdFromPath(String relativePath) {
        var matcher = ORDER_FILE.matcher(normalize(relativePath));
        return matcher.matches() ? matcher.group(1) : null;
    }

    public static boolean isPrivatePrefix(String relativePath) {
        String path = normalize(relativePath);
        return path.contains("/documents/")
                || path.startsWith("projects/")
                || path.startsWith("orders/");
    }

    static String normalize(String relativePath) {
        return relativePath.replace("\\", "/").replaceAll("^/+", "").toLowerCase(Locale.ROOT);
    }
}
