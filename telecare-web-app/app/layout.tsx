import type { Metadata } from "next";
import { Montserrat, Inter } from "next/font/google";
import NextTopLoader from "nextjs-toploader";
import Providers from "@/components/auth/Providers";
import "./globals.css";

const montserrat = Montserrat({
  variable: "--font-montserrat",
  subsets: ["latin", "vietnamese"],
  weight: ["400", "500", "600", "700", "800", "900"],
  display: "swap",
});

const inter = Inter({
  variable: "--font-inter",
  subsets: ["latin", "vietnamese"],
  weight: ["400", "500", "600", "700"],
  display: "swap",
});

export const metadata: Metadata = {
  title: "TeleCare - Cổng tra cứu gói cước & Tiếp nhận yêu cầu hỗ trợ khách hàng viễn thông",
  description:
    "TeleCare - Cổng thông tin tra cứu gói cước và tiếp nhận yêu cầu hỗ trợ khách hàng viễn thông trực tuyến nhanh chóng, tiện lợi và an toàn.",
  keywords: [
    "TeleCare",
    "Viễn thông",
    "Gói cước",
    "Hỗ trợ khách hàng",
    "CSKH",
    "Tra cứu cước",
  ],
  icons: {
    icon: "/favicon.ico",
    shortcut: "/favicon-16x16.png",
    apple: "/apple-touch-icon.png",
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html
      lang="vi"
      suppressHydrationWarning
      className={`${montserrat.variable} ${inter.variable} h-full antialiased scroll-smooth`}
    >
      <body className="min-h-full flex flex-col font-sans bg-background text-foreground">
        <NextTopLoader
          color="#2563eb"
          initialPosition={0.08}
          crawlSpeed={200}
          height={3}
          crawl={true}
          showSpinner={false}
          easing="ease"
          speed={200}
          shadow="0 0 10px #2563eb,0 0 5px #3b82f6"
          zIndex={99999}
        />
        <Providers>
          {children}
        </Providers>
      </body>
    </html>
  );
}
