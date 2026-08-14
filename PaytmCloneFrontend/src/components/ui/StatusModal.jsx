import { motion as Motion, AnimatePresence } from "framer-motion";
import { CheckCircle2, XCircle, X } from "lucide-react";
import { Button } from "./button";

export function StatusModal({ isOpen, onClose, status, title, message, amount, buttonText = "Done" }) {
  const isSuccess = status === "success";

  return (
    <AnimatePresence>
      {isOpen && (
        <>
          <Motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="fixed inset-0 bg-foreground/20 backdrop-blur-sm z-50" />
          <Motion.div initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.9 }} transition={{ type: "spring", damping: 25 }} className="fixed inset-0 flex items-center justify-center z-50 p-6">
            <div className="bg-card rounded-3xl p-8 w-full max-w-sm text-center relative shadow-elevated">
              <button onClick={onClose} className="absolute top-4 right-4 p-2 rounded-full hover:bg-muted transition-colors"><X className="w-5 h-5 text-muted-foreground" /></button>
              <Motion.div initial={{ scale: 0 }} animate={{ scale: 1 }} transition={{ delay: 0.2, type: "spring", damping: 15 }} className={`w-20 h-20 mx-auto mb-6 rounded-full flex items-center justify-center ${isSuccess ? "bg-success/10" : "bg-destructive/10"}`}>
                {isSuccess ? <CheckCircle2 className="w-10 h-10 text-success" /> : <XCircle className="w-10 h-10 text-destructive" />}
              </Motion.div>
              <Motion.h2 initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.3 }} className={`text-2xl font-display font-bold mb-2 ${isSuccess ? "text-success" : "text-destructive"}`}>{title}</Motion.h2>
              {amount !== undefined && <Motion.p initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.4 }} className="text-3xl font-display font-bold text-foreground mb-2">₹{amount.toLocaleString("en-IN")}</Motion.p>}
              <Motion.p initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.5 }} className="text-muted-foreground mb-8">{message}</Motion.p>
              <Motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.6 }}><Button onClick={onClose} className="w-full" size="lg" variant={isSuccess ? "default" : "destructive"}>{buttonText}</Button></Motion.div>
            </div>
          </Motion.div>
        </>
      )}
    </AnimatePresence>
  );
}
