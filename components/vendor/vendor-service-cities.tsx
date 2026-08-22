import { MapPin } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

interface VendorServiceCitiesProps {
  cities: string[];
  hq?: string;
}

export function VendorServiceCities({ cities, hq }: VendorServiceCitiesProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">Service cities</CardTitle>
        {hq ? <p className="text-muted-foreground text-sm">Headquarters in {hq}</p> : null}
      </CardHeader>
      <CardContent className="flex flex-wrap gap-2">
        {cities.map((city) => (
          <Badge key={city} variant="secondary" className="rounded-xl px-3 py-1.5 text-sm">
            <MapPin className="size-3.5" />
            {city}
          </Badge>
        ))}
      </CardContent>
    </Card>
  );
}
