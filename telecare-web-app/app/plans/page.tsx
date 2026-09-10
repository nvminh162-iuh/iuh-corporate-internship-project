import Header from "@/components/layout/Header";
import Footer from "@/components/layout/Footer";
import PlansCatalogWrapper from "@/components/plans/PlansCatalogContent";

export default function PlansPage() {
  return (
    <div className="min-h-screen flex flex-col bg-background text-foreground">
      {/* Top Sticky Header */}
      <Header />

      {/* Main Content Area */}
      <main className="flex-grow pt-20">
        <PlansCatalogWrapper />
      </main>

      {/* Footer */}
      <Footer />
    </div>
  );
}
