import { ExternalLink, Globe, Share2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { VendorSocialLinks } from "@/types";

interface VendorSocialLinksCardProps {
  social: VendorSocialLinks;
}

const socialItems: {
  key: keyof VendorSocialLinks;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
}[] = [
  { key: "website", label: "Website", icon: Globe },
  { key: "instagram", label: "Instagram", icon: Share2 },
  { key: "linkedin", label: "LinkedIn", icon: ExternalLink },
  { key: "facebook", label: "Facebook", icon: Share2 },
  { key: "youtube", label: "YouTube", icon: ExternalLink },
  { key: "pinterest", label: "Pinterest", icon: Share2 },
];

export function VendorSocialLinksCard({ social }: VendorSocialLinksCardProps) {
  const links = socialItems.filter((item) => Boolean(social[item.key]));

  if (links.length === 0) return null;

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">Social & web</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-wrap gap-2">
        {links.map((item) => {
          const Icon = item.icon;
          const href = social[item.key];
          if (!href) return null;

          return (
            <Button key={item.key} variant="outline" className="rounded-xl" asChild>
              <a href={href} target="_blank" rel="noopener noreferrer">
                <Icon className="size-4" />
                {item.label}
              </a>
            </Button>
          );
        })}
      </CardContent>
    </Card>
  );
}
