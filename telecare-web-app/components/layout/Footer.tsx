import Image from "next/image";
import Link from "next/link";
import { Mail, Phone, ShieldCheck, Headphones } from "lucide-react";

export default function Footer() {
  return (
    <footer className="bg-[#163b70] dark:bg-[#162032] text-slate-100 border-t border-primary-dark/80 dark:border-[#223147] transition-colors shadow-2xl">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-12 pb-8">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 lg:gap-12 mb-10">
          {/* Column 1: Brand & Topic Summary */}
          <div className="flex flex-col gap-4">
            <Link href="/" className="flex items-center self-start group">
              <div className="bg-white/10 dark:bg-[#1f2d47] border border-white/15 dark:border-[#2a3c5a] rounded-2xl px-5 py-3 hover:bg-white/15 dark:hover:bg-[#253655] transition-all inline-flex items-center justify-center shadow-sm">
                <Image
                  src="/logo/telecare-remove-bg.png"
                  alt="TeleCare Logo"
                  width={160}
                  height={55}
                  unoptimized
                  style={{ width: "auto" }}
                  className="h-12 sm:h-14 w-auto object-contain brightness-0 invert"
                />
              </div>
            </Link>

            <p className="text-sm text-slate-200 dark:text-slate-300 leading-relaxed max-w-sm">
              TeleCare - Cổng tra cứu gói cước và tiếp nhận yêu cầu hỗ trợ khách hàng viễn thông.
            </p>

            <div className="flex items-center gap-3 text-xs text-emerald-400 font-medium">
              <ShieldCheck className="w-4 h-4" />
              <span>Hỗ trợ nhanh chóng & Bảo mật thông tin</span>
            </div>
          </div>

          {/* Column 2: Dịch vụ & Hướng dẫn */}
          <div className="flex flex-col gap-3.5">
            <h4 className="text-xs font-bold text-white uppercase tracking-widest font-heading">
              Dịch vụ viễn thông
            </h4>
            <nav className="flex flex-col space-y-2 text-sm text-slate-200 dark:text-slate-300">
              <Link href="#" className="hover:text-white transition-colors">
                Tra cứu gói cước
              </Link>
              <Link href="#" className="hover:text-white transition-colors">
                Gửi yêu cầu hỗ trợ
              </Link>
              <Link href="#" className="hover:text-white transition-colors">
                Chính sách bảo mật
              </Link>
              <Link href="#" className="hover:text-white transition-colors">
                Điều khoản sử dụng
              </Link>
            </nav>
          </div>

          {/* Column 3: Liên hệ hỗ trợ */}
          <div className="flex flex-col gap-3.5">
            <h4 className="text-xs font-bold text-white uppercase tracking-widest font-heading">
              Kênh hỗ trợ
            </h4>
            <div className="flex flex-col space-y-2.5 text-sm text-slate-200 dark:text-slate-300">
              <div className="flex items-center gap-2 text-slate-200 dark:text-slate-300">
                <Headphones className="w-4 h-4 text-cyan-400 shrink-0" />
                <span>Tổng đài CSKH 24/7</span>
              </div>
              <a
                href="mailto:telecare.platform.cskh@gmail.com"
                className="hover:text-cyan-300 transition-colors flex items-center gap-2 text-slate-200 dark:text-slate-300"
              >
                <Mail className="w-4 h-4 text-cyan-400 shrink-0" />
                <span className="truncate">telecare.platform.cskh@gmail.com</span>
              </a>
            </div>
          </div>
        </div>

        {/* Bottom Bar */}
        <div className="pt-6 border-t border-white/15 dark:border-[#223147] flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-slate-300/80">
          <p>© 2026 TeleCare. Nền tảng hỗ trợ khách hàng viễn thông.</p>
          <div className="flex items-center gap-4 text-slate-300/80">
            <span>Bảo mật</span>
            <span>·</span>
            <span>Điều khoản</span>
            <span>·</span>
            <span>Trợ giúp</span>
          </div>
        </div>
      </div>
    </footer>
  );
}
