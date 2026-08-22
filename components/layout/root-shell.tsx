import { Navbar } from "@/components/layout/navbar";
import { Footer } from "@/components/layout/footer";
import { cn } from "@/lib/utils";

interface RootShellProps {
  children: React.ReactNode;
  className?: string;
  showFooter?: boolean;
}

export function RootShell({ children, className, showFooter = true }: RootShellProps) {
  return (
    <div className="flex min-h-screen flex-col">
      <Navbar />
      <main className={cn("flex-1", className)}>{children}</main>
      {showFooter && <Footer />}
    </div>
  );
}
