import Header from "@/components/layout/Header";
import Footer from "@/components/layout/Footer";

export default function HomePage() {
  return (
    <div className="min-h-screen flex flex-col bg-background text-foreground">
      {/* Top Sticky Header */}
      <Header />

      {/* Main Content Area */}
      <main className="flex-grow pt-20">

      </main>

      {/* Footer */}
      <Footer />
    </div>
  );
}
