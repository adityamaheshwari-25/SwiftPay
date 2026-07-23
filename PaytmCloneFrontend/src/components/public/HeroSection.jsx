import { ArrowRight, CheckCircle, CreditCard, Send, Shield, Smartphone, TrendingUp, Wallet } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { Button } from "@/components/ui/button";
import { LABELS } from "@/config/labels.config";
import { useAuth } from "@/context/AuthContext";

export const HeroSection = () => {
  const labels = LABELS.publicComponents.heroSection;
  const navigate = useNavigate();
  const { isAuthenticated, user, getHomeRouteForRole } = useAuth();
  const MotionDiv = motion.div;
  const MotionH1 = motion.h1;
  const MotionP = motion.p;

  const goToDashboard = () => {
    navigate(getHomeRouteForRole(user?.role));
  };

  const actionCards = [
    { icon: Send, label: labels.actions.sendMoney, color: "bg-white/20" },
    { icon: Wallet, label: labels.actions.addMoney, color: "bg-white/15" },
    { icon: CreditCard, label: labels.actions.payBills, color: "bg-white/15" },
    { icon: Smartphone, label: labels.actions.recharge, color: "bg-white/20" },
  ];

  return (
    <section className="pt-24 pb-12 px-4 sm:pt-32 sm:pb-16">
      <div className="container mx-auto max-w-7xl">
        <div className="flex flex-col lg:flex-row items-center gap-8 lg:gap-12">
          <MotionDiv initial={{ opacity: 0, y: 30 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.7, delay: 0.2 }} className="flex-1 text-center lg:text-left">
            <MotionDiv initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }} transition={{ delay: 0.3 }} className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-teal-50 border border-teal-200 mb-6">
              <span className="w-2 h-2 rounded-full bg-teal-500 animate-pulse" />
              <span className="text-sm font-medium text-teal-700">{labels.trustedBy}</span>
            </MotionDiv>

            <MotionH1 initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.4 }} className="text-4xl sm:text-5xl lg:text-6xl font-bold text-gray-900 leading-tight mb-6">
              {labels.headingPrefix} <span className="text-blue-600">{labels.headingAccent}</span>
            </MotionH1>

            <MotionP initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.5 }} className="text-base sm:text-lg text-gray-600 leading-relaxed mb-8 max-w-xl mx-auto lg:mx-0">
              {labels.description}
            </MotionP>

            <MotionDiv initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.6 }} className="flex flex-col sm:flex-row gap-3 justify-center lg:justify-start mb-6">
              {!isAuthenticated ? (
                <>
                  <Button size="lg" className="bg-blue-600 text-white hover:bg-blue-700 text-base px-8 h-12 group shadow-lg shadow-blue-600/20" onClick={() => navigate("/register-user")}>
                    {labels.registerUser}
                    <ArrowRight className="ml-2 h-5 w-5 group-hover:translate-x-1 transition-transform" />
                  </Button>
                  <Button size="lg" className="bg-cyan-600 text-white hover:bg-cyan-700 text-base px-8 h-12 shadow-lg shadow-cyan-600/20" onClick={() => navigate("/register-merchant")}>
                    {labels.registerMerchant}
                  </Button>
                </>
              ) : (
                <Button size="lg" className="bg-emerald-600 text-white hover:bg-emerald-700 text-base px-8 h-12 shadow-lg shadow-emerald-600/20" onClick={goToDashboard}>
                  {labels.goToDashboard}
                  <ArrowRight className="ml-2 h-5 w-5" />
                </Button>
              )}
            </MotionDiv>

            {!isAuthenticated && (
              <MotionDiv initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.7 }} className="text-center lg:text-left">
                <div className="text-sm text-gray-600 hover:text-blue-600 transition-colors underline underline-offset-4 cursor-pointer" onClick={() => navigate("/login")}>
                  {labels.alreadyHaveAccount}
                </div>
              </MotionDiv>
            )}
          </MotionDiv>

          <MotionDiv initial={{ opacity: 0, x: 50 }} animate={{ opacity: 1, x: 0 }} transition={{ duration: 0.7, delay: 0.4 }} className="flex-1 w-full max-w-md relative">
            <div className="relative">
              <MotionDiv initial={{ y: 50, opacity: 0 }} animate={{ y: 0, opacity: 1 }} transition={{ delay: 0.8, duration: 0.6 }} className="bg-gradient-to-br from-blue-600 to-cyan-600 rounded-3xl p-6 shadow-2xl">
                <div className="bg-white/10 backdrop-blur-sm rounded-2xl p-5 mb-4">
                  <p className="text-xs text-white/70 mb-1">{labels.availableBalance}</p>
                  <p className="text-3xl font-bold text-white mb-4">$12,458.50</p>
                  <div className="flex items-center gap-2">
                    <div className="px-3 py-1 rounded-full bg-white/20 backdrop-blur-sm">
                      <p className="text-xs text-white font-medium">USD</p>
                    </div>
                    <div className="flex items-center gap-1 text-white/90">
                      <TrendingUp className="w-3 h-3" />
                      <span className="text-xs font-medium">+12.5%</span>
                    </div>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  {actionCards.map((item, i) => (
                    <MotionDiv key={i} initial={{ scale: 0, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} transition={{ delay: 1 + i * 0.1, duration: 0.4 }} className={`${item.color} backdrop-blur-sm rounded-xl p-4 flex flex-col items-center justify-center gap-2 hover:bg-white/30 transition-colors cursor-pointer`}>
                      <item.icon className="w-5 h-5 text-white" />
                      <span className="text-xs text-white font-medium text-center">{item.label}</span>
                    </MotionDiv>
                  ))}
                </div>
              </MotionDiv>

              <MotionDiv initial={{ x: -50, opacity: 0 }} animate={{ x: 0, opacity: 1 }} transition={{ delay: 1.4, duration: 0.6 }} className="absolute -left-4 top-8 bg-white border border-gray-200 rounded-xl p-3 shadow-xl max-w-[180px]">
                <div className="flex items-center gap-2 mb-2">
                  <div className="w-8 h-8 rounded-full bg-teal-100 flex items-center justify-center">
                    <CheckCircle className="w-4 h-4 text-teal-600" />
                  </div>
                  <div>
                    <p className="text-xs font-semibold text-gray-900">{labels.paymentSent}</p>
                    <p className="text-xs text-gray-600">{labels.minsAgo}</p>
                  </div>
                </div>
                <p className="text-lg font-bold text-gray-900">-$124.00</p>
              </MotionDiv>

              <MotionDiv initial={{ x: 50, opacity: 0 }} animate={{ x: 0, opacity: 1 }} transition={{ delay: 1.6, duration: 0.6 }} className="absolute -right-4 bottom-12 bg-white border border-gray-200 rounded-xl p-3 shadow-xl">
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center">
                    <Shield className="w-4 h-4 text-blue-600" />
                  </div>
                  <div>
                    <p className="text-xs text-gray-600">{labels.security}</p>
                    <p className="text-sm font-semibold text-gray-900">{labels.bankGrade}</p>
                  </div>
                </div>
              </MotionDiv>
            </div>
          </MotionDiv>
        </div>
      </div>
    </section>
  );
};
