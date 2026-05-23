import React from "react";
import { 
  ArrowRight, 
  Smartphone, 
  Share2, 
  MessageSquare, 
  ShieldCheck, 
  Download,
  Flame
} from "lucide-react";
import appPreview from "../assets/app_preview.png";

interface LandingProps {
  onEnterLogin: () => void;
}

export const Landing: React.FC<LandingProps> = ({ onEnterLogin }) => {
  return (
    <div className="bg-[#fafafa] min-h-screen font-sans text-slate-900 overflow-x-hidden selection:bg-accent/20 selection:text-accent">
      
      {/* 1. Header/Navigation Bar */}
      <header className="fixed top-0 left-0 w-full z-50 bg-[#fafafa]/80 backdrop-blur-md border-b border-slate-200/50 py-4 px-6 md:px-12 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="p-1.5 bg-gradient-to-tr from-accent to-accent-secondary rounded-lg text-white">
            <Smartphone size={20} />
          </div>
          <span className="font-display text-xl font-bold tracking-tight text-slate-900">
            UTH Social
          </span>
        </div>
        <nav className="hidden md:flex items-center gap-8 text-sm font-medium text-slate-600">
          <a href="#features" className="hover:text-accent transition">Tính năng</a>
          <a href="#statistics" className="hover:text-accent transition">Số liệu</a>
          <a href="#download" className="hover:text-accent transition">Tải ứng dụng</a>
        </nav>
        <button
          onClick={onEnterLogin}
          className="group flex items-center gap-1.5 px-4.5 py-2 text-xs font-semibold border border-slate-300 hover:border-accent hover:text-accent rounded-xl transition-all duration-200 bg-white shadow-sm"
        >
          Đăng nhập ngay
          <ArrowRight size={14} className="group-hover:translate-x-0.5 transition" />
        </button>
      </header>

      {/* 2. Hero Section */}
      <section className="relative pt-32 pb-24 md:pt-40 md:pb-36 px-6 md:px-12 max-w-6xl mx-auto grid grid-cols-1 lg:grid-cols-[1.2fr_0.8fr] gap-12 lg:gap-8 items-center">
        
        {/* Left Side: Typography Headline & CTAs */}
        <div className="space-y-8 max-w-xl text-left">
          
          {/* Section Label Badge */}
          <div className="inline-flex items-center gap-2 rounded-full border border-accent/20 bg-accent/5 px-4 py-1.5">
            <span className="h-2 w-2 rounded-full bg-accent animate-pulse" />
            <span className="font-mono text-[10px] font-bold uppercase tracking-[0.15em] text-accent">
              Ứng Dụng Mới Ra Mắt
            </span>
          </div>

          <h1 className="font-display text-4xl sm:text-5xl md:text-[4.5rem] leading-[1.05] tracking-tight text-slate-900">
            Mạng xã hội học đường dành riêng cho <span className="gradient-text">sinh viên UTH</span>
          </h1>

          <p className="text-base sm:text-lg text-slate-500 leading-relaxed font-normal">
            Không gian kết nối tri thức, cập nhật thông tin nhanh nhất, trao đổi học thuật và gắn kết cộng đồng sinh viên trường Đại học Giao thông Vận tải TP.HCM.
          </p>

          <div className="flex flex-col sm:flex-row gap-4">
            <a
              href="#download"
              className="group h-13 px-6 bg-gradient-to-r from-accent to-accent-secondary hover:from-accent hover:to-accent-secondary text-white font-semibold rounded-xl transition-all duration-300 shadow-md hover:shadow-accent flex items-center justify-center gap-2 hover:-translate-y-0.5"
            >
              Tải App ngay
              <ArrowRight size={16} className="group-hover:translate-x-1 transition duration-200" />
            </a>
            <a
              href="#features"
              className="h-13 px-6 border border-slate-300 hover:border-accent hover:text-accent font-semibold rounded-xl bg-white hover:bg-slate-50 transition flex items-center justify-center"
            >
              Tìm hiểu thêm
            </a>
          </div>
        </div>

        {/* Right Side: Abstract Graphic Mockup */}
        <div className="relative flex justify-center lg:justify-end select-none">
          {/* Ambient Radial Glow */}
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-80 h-80 bg-accent/5 rounded-full blur-[80px]" />
          
          {/* Rotating Decorative Outer Ring */}
          <div className="absolute w-[22rem] h-[22rem] rounded-full border border-dashed border-accent/20 animate-spin-slow flex items-center justify-center" />

          {/* Floating Element 1: Glass Card 1 (Social Post) */}
          <div className="absolute left-[-20px] top-[40px] z-20 w-44 bg-white/70 backdrop-blur-md p-4 rounded-2xl border border-slate-200/50 shadow-lg animate-float flex flex-col gap-2">
            <div className="flex items-center gap-2">
              <div className="w-6 h-6 rounded-full bg-sky-500/20 flex items-center justify-center font-bold text-[10px] text-sky-600">A</div>
              <span className="text-[10px] font-semibold text-slate-800">Anh Tuấn • UTH</span>
            </div>
            <p className="text-[9px] text-slate-500 leading-normal">
              🔥 Lớp mình đã ai hoàn thành bài tập lớn tuần này chưa nhỉ?
            </p>
          </div>

          {/* Floating Element 2: Glass Card 2 (Trending tag) */}
          <div className="absolute right-[-10px] bottom-[50px] z-20 bg-white/80 backdrop-blur-md px-3.5 py-2.5 rounded-xl border border-slate-200/50 shadow-md animate-float-delayed flex items-center gap-2">
            <div className="p-1 bg-amber-500/10 rounded text-amber-500">
              <Flame size={12} />
            </div>
            <div className="text-left">
              <p className="text-[9px] font-bold text-slate-800">Xu hướng học thuật</p>
              <p className="text-[8px] text-slate-400">#AI_Edge_Uth</p>
            </div>
          </div>

          {/* App Smartphone Frame Mockup */}
          <div className="relative z-10 w-64 h-[32rem] bg-slate-900 rounded-[2.5rem] p-2.5 border-4 border-slate-800 shadow-2xl flex flex-col overflow-hidden">
            {/* Camera notch */}
            <div className="absolute top-5 left-1/2 -translate-x-1/2 w-16 h-4 bg-slate-800 rounded-full z-30" />
            
            {/* Real App Screenshot Image */}
            <img 
              src={appPreview} 
              alt="UTH Social App Screen" 
              className="w-full h-full object-cover rounded-[2rem] z-10" 
            />
          </div>
        </div>
      </section>

      {/* 3. Inverted Contrast Section (Statistics) */}
      <section id="statistics" className="relative bg-slate-900 text-white py-24 md:py-32 dot-pattern">
        {/* Glow overlay */}
        <div className="absolute bottom-0 right-0 w-96 h-96 bg-accent/5 rounded-full blur-[100px]" />
        
        <div className="max-w-6xl mx-auto px-6 md:px-12 text-center space-y-16">
          <div className="space-y-4 max-w-2xl mx-auto">
            <h2 className="font-display text-3xl md:text-5xl tracking-wide">
              Cộng đồng gắn kết, số liệu bứt phá
            </h2>
            <p className="text-slate-400 text-sm md:text-base leading-relaxed">
              Môi trường lý tưởng giúp kết nối sinh viên và hỗ trợ quá trình nghiên cứu, học tập hiệu quả.
            </p>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
            {[
              { label: "Sinh viên tham gia", value: "10K+" },
              { label: "Bài viết chia sẻ", value: "50K+" },
              { label: "Lượt tương tác mỗi ngày", value: "5K+" },
              { label: "Kết nối hoạt động", value: "24/7" }
            ].map((stat, idx) => (
              <div key={idx} className="space-y-3 p-4 border border-slate-800/40 rounded-xl bg-slate-950/20 backdrop-blur-sm">
                <h3 className="font-display text-4xl md:text-6xl text-sky-400 tracking-tight">
                  {stat.value}
                </h3>
                <p className="text-xs md:text-sm text-slate-400 font-medium uppercase tracking-wider">
                  {stat.label}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* 4. App Benefits/Features Grid */}
      <section id="features" className="py-24 md:py-36 px-6 md:px-12 max-w-6xl mx-auto space-y-16">
        
        {/* Section Header */}
        <div className="text-center space-y-4 max-w-xl mx-auto">
          <div className="inline-flex items-center gap-2 rounded-full border border-accent/20 bg-accent/5 px-4 py-1.5">
            <span className="h-2 w-2 rounded-full bg-accent" />
            <span className="font-mono text-[10px] font-bold uppercase tracking-[0.15em] text-accent">
              Giá Trị Mang Lại
            </span>
          </div>
          <h2 className="font-display text-3xl md:text-5xl leading-tight text-slate-900">
            Nền tảng của tương tác sinh viên hiện đại
          </h2>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {[
            {
              title: "Đăng tin & Nhắn tin",
              desc: "Chia sẻ trạng thái, đăng tải hình ảnh chất lượng cao và trò chuyện, nhắn tin thời gian thực với bạn bè trong hệ thống mạng xã hội học đường.",
              icon: MessageSquare,
            },
            {
              title: "Đăng bán Sản phẩm",
              desc: "Sàn trao đổi Marketplace tích hợp giúp sinh viên đăng bán giáo trình, tài liệu cũ, thiết bị hay đồ dùng cá nhân cực kỳ tiện lợi.",
              icon: Share2,
            },
            {
              title: "Nhận Thông báo tức thì",
              desc: "Cập nhật nhanh nhất các thông báo quan trọng từ câu lạc bộ, khoa, trường hoặc nhận cảnh báo hoạt động từ những người theo dõi bạn.",
              icon: ShieldCheck,
            }
          ].map((item, idx) => {
            const Icon = item.icon;
            return (
              <div 
                key={idx} 
                className="bg-white border border-slate-200 p-8 rounded-2xl hover:shadow-xl hover:border-slate-300 transition-all duration-300 flex flex-col text-left group"
              >
                <div className="p-3 bg-gradient-to-tr from-accent to-accent-secondary rounded-xl text-white w-fit shadow-md group-hover:scale-105 transition duration-200 mb-6">
                  <Icon size={22} />
                </div>
                <h4 className="text-lg font-bold text-slate-800 mb-3 tracking-tight">
                  {item.title}
                </h4>
                <p className="text-sm text-slate-500 leading-relaxed">
                  {item.desc}
                </p>
              </div>
            );
          })}
        </div>
      </section>

      {/* 5. Call to Action (CTA) Section */}
      <section id="download" className="py-24 md:py-32 bg-slate-50 border-t border-slate-200/50">
        <div className="max-w-4xl mx-auto px-6 text-center space-y-10">
          
          <div className="inline-flex items-center gap-2 rounded-full border border-accent/20 bg-accent/5 px-4 py-1.5">
            <span className="h-2 w-2 rounded-full bg-accent animate-pulse" />
            <span className="font-mono text-[10px] font-bold uppercase tracking-[0.15em] text-accent">
              Bắt Đầu Trải Nghiệm
            </span>
          </div>

          <h2 className="font-display text-4xl md:text-5xl leading-tight text-slate-900">
            Sẵn sàng kết nối cùng UTH Social?
          </h2>
          <p className="text-slate-500 text-sm md:text-base max-w-xl mx-auto leading-relaxed">
            Tải app ngay hôm nay để nhận thông báo, cập nhật tài liệu học thuật và gắn kết với hàng ngàn bạn bè cùng trường.
          </p>

          <div className="flex justify-center items-center pt-4">
            <a
              href="#"
              onClick={(e) => { e.preventDefault(); alert("Đang tải file cài đặt UTH_Social.apk..."); }}
              className="w-full sm:w-auto h-14 px-8 bg-slate-900 hover:bg-slate-850 text-white font-semibold rounded-xl flex items-center justify-center gap-3 transition-all duration-200 hover:-translate-y-0.5 shadow-md"
            >
              <Download size={18} />
              <span>Tải ứng dụng Android (.APK)</span>
            </a>
          </div>
        </div>
      </section>

      {/* 6. Footer */}
      <footer className="border-t border-slate-200 py-8 px-6 md:px-12 flex flex-col md:flex-row items-center justify-between text-xs text-slate-500 gap-4 max-w-6xl mx-auto">
        <div className="flex items-center gap-2">
          <span className="font-display font-bold text-sm text-slate-700">UTH Social</span>
          <span>© 2026 UTH. Bảo lưu mọi quyền.</span>
        </div>
        <div className="flex gap-6">
          <a href="#features" className="hover:text-accent transition">Tính năng</a>
          <a href="#download" className="hover:text-accent transition">Tải App</a>
          <button onClick={onEnterLogin} className="hover:text-accent font-semibold transition">
            Đăng nhập Web
          </button>
        </div>
      </footer>

    </div>
  );
};
