import { motion } from "framer-motion";
import { Eye, EyeOff, Wallet } from "lucide-react";
import { useState } from "react";

export function WalletCard({ balance, currency = "₹", userName = "User" }) {
  const [showBalance, setShowBalance] = useState(true);
  const formatBalance = (amount) => new Intl.NumberFormat("en-IN").format(amount);

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, ease: "easeOut" }} className="gradient-primary rounded-2xl p-6 text-primary-foreground relative overflow-hidden">
      <div className="absolute inset-0 opacity-10">
        <div className="absolute top-0 right-0 w-40 h-40 rounded-full bg-primary-foreground/20 -translate-y-1/2 translate-x-1/2" />
        <div className="absolute bottom-0 left-0 w-32 h-32 rounded-full bg-primary-foreground/10 translate-y-1/2 -translate-x-1/2" />
      </div>
      <div className="relative z-10">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-2"><Wallet className="w-5 h-5" /><span className="text-sm font-medium opacity-90">Wallet Balance</span></div>
          <button onClick={() => setShowBalance(!showBalance)} className="p-2 rounded-full bg-primary-foreground/10 hover:bg-primary-foreground/20 transition-colors">{showBalance ? <Eye className="w-4 h-4" /> : <EyeOff className="w-4 h-4" />}</button>
        </div>
        <motion.div key={showBalance ? "visible" : "hidden"} initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="mb-4">
          <span className="text-4xl font-display font-bold">{showBalance ? `${currency}${formatBalance(balance)}` : "••••••"}</span>
        </motion.div>
        <div className="flex items-center justify-between">
          <span className="text-sm opacity-75">Hello, {userName}</span>
          <div className="flex gap-1">{[1, 2, 3, 4].map((i) => <div key={i} className="w-2 h-2 rounded-full bg-primary-foreground/30" />)}</div>
        </div>
      </div>
    </motion.div>
  );
}
