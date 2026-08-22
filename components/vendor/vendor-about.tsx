import { Check } from "lucide-react";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

interface VendorAboutProps {
  about: string;
  highlights?: string[];
}

export function VendorAbout({ about, highlights }: VendorAboutProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">About the company</CardTitle>
      </CardHeader>
      <CardContent className="space-y-5">
        <p className="text-muted-foreground text-base leading-relaxed text-pretty">{about}</p>
        {highlights && highlights.length > 0 ? (
          <ul className="space-y-3">
            {highlights.map((item) => (
              <li key={item} className="flex items-start gap-3 text-sm">
                <span className="bg-muted mt-0.5 flex size-5 shrink-0 items-center justify-center rounded-md">
                  <Check className="size-3" />
                </span>
                <span>{item}</span>
              </li>
            ))}
          </ul>
        ) : null}
      </CardContent>
    </Card>
  );
}
