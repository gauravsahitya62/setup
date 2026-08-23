import { VendorAbout } from "@/components/vendor/vendor-about";
import { VendorBookingCta } from "@/components/vendor/vendor-booking-cta";
import { VendorPortfolio } from "@/components/vendor/vendor-portfolio";
import { VendorProductsGrid } from "@/components/vendor/vendor-products-grid";
import { VendorProfileHeader } from "@/components/vendor/vendor-profile-header";
import { VendorReviews } from "@/components/vendor/vendor-reviews";
import { VendorServiceCities } from "@/components/vendor/vendor-service-cities";
import { VendorSocialLinksCard } from "@/components/vendor/vendor-social-links";
import { AnalyticsPageView } from "@/components/shared/analytics-page-view";
import type { MarketplaceListing, VendorProfile } from "@/types";

interface VendorProfilePageProps {
  vendor: VendorProfile;
  listings: MarketplaceListing[];
}

export function VendorProfileView({ vendor, listings }: VendorProfilePageProps) {
  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 sm:py-10 lg:px-8">
      <AnalyticsPageView
        event="vendor_viewed"
        properties={{ verified: vendor.verificationStatus === "verified" }}
      />
      <div className="animate-in fade-in slide-in-from-bottom-3 duration-500">
        <VendorProfileHeader vendor={vendor} />
      </div>

      <div className="mt-8 grid gap-8 lg:grid-cols-[minmax(0,1fr)_320px] lg:items-start xl:grid-cols-[minmax(0,1fr)_340px]">
        <div className="space-y-8 pb-28 md:pb-8">
          <VendorAbout about={vendor.about} highlights={vendor.highlights} />
          <VendorServiceCities cities={vendor.serviceCities} hq={vendor.location} />
          <VendorPortfolio portfolio={vendor.portfolio} />
          <VendorProductsGrid listings={listings} vendorName={vendor.name} vendorId={vendor.id} />
          <VendorReviews
            vendorId={vendor.id}
            rating={vendor.rating}
            reviewCount={vendor.reviewCount}
          />
          <VendorSocialLinksCard social={vendor.social} />
        </div>

        <aside className="hidden md:block">
          <div className="sticky top-24 space-y-4">
            <VendorBookingCta vendor={vendor} />
          </div>
        </aside>
      </div>

      {/* Mobile sticky booking bar */}
      <div className="md:hidden">
        <VendorBookingCta vendor={vendor} sticky />
      </div>
    </div>
  );
}
