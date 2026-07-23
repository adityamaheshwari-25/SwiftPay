import { motion, useReducedMotion } from "framer-motion";
import { ArrowLeft, Wallet } from "lucide-react";

export const AuthShell = ({
  title,
  subtitle,
  panelTitle,
  panelDescription,
  panelActionLabel,
  onPanelAction,
  panelOnLeft = false,
  children,
  footer,
  backLabel,
  onBack,
  isExiting = false,
}) => {
  const reduceMotion = useReducedMotion();
  const baseTransition = { duration: reduceMotion ? 0 : 0.36, ease: [0.22, 1, 0.36, 1] };

  return (
    <div className="auth-shell min-h-screen p-4 sm:p-6 lg:p-10">
      <div className="auth-bg-shape auth-bg-shape-one" aria-hidden />
      <div className="auth-bg-shape auth-bg-shape-two" aria-hidden />
      <div className="auth-bg-shape auth-bg-shape-three" aria-hidden />

      <motion.div
        initial={reduceMotion ? false : { opacity: 0, y: 20, scale: 0.98 }}
        animate={
          reduceMotion
            ? {}
            : isExiting
              ? { opacity: 0, y: -18, scale: 0.985 }
              : { opacity: 1, y: 0, scale: 1 }
        }
        transition={baseTransition}
        className="auth-wrapper mx-auto w-full max-w-5xl"
      >
        <div className="auth-mobile-brand lg:hidden">
          <div className="auth-brand-badge">
            <Wallet className="w-5 h-5 text-white" />
          </div>
          <div>
            <p className="auth-brand-title">SwiftPay</p>
            <p className="auth-brand-subtitle">Secure payments for everyone</p>
          </div>
        </div>

        <div className="auth-card grid overflow-hidden rounded-[2rem] border border-white/60 bg-white/90 shadow-[0_24px_80px_-36px_rgba(14,35,84,0.45)] backdrop-blur-xl lg:grid-cols-2">
          <aside className={`auth-panel hidden lg:flex ${panelOnLeft ? "lg:order-1" : "lg:order-2"}`}>
            <div className="auth-panel-overlay" />
            <div className="auth-panel-content">
              <div className="auth-brand">
                <div className="auth-brand-badge">
                  <Wallet className="w-5 h-5 text-white" />
                </div>
                <div>
                  <p className="auth-brand-title">SwiftPay</p>
                  <p className="auth-brand-subtitle">Digital Wallet</p>
                </div>
              </div>

              <motion.div
                initial={reduceMotion ? false : { opacity: 0, x: panelOnLeft ? -12 : 12 }}
                animate={reduceMotion ? {} : { opacity: 1, x: 0 }}
                transition={{ ...baseTransition, delay: reduceMotion ? 0 : 0.1 }}
              >
                <h2 className="auth-panel-title">{panelTitle}</h2>
                <p className="auth-panel-description">{panelDescription}</p>
                <button type="button" onClick={onPanelAction} className="auth-panel-cta">
                  {panelActionLabel}
                </button>
              </motion.div>
            </div>
          </aside>

          <section className={`auth-content ${panelOnLeft ? "lg:order-2" : "lg:order-1"}`}>
            <div className="auth-content-inner">
              <motion.div
                initial={reduceMotion ? false : { opacity: 0, y: 8 }}
                animate={reduceMotion ? {} : { opacity: 1, y: 0 }}
                transition={{ ...baseTransition, delay: reduceMotion ? 0 : 0.07 }}
              >
                <h1 className="auth-heading">{title}</h1>
                <p className="auth-subheading">{subtitle}</p>
              </motion.div>

              {children}
              {footer}
            </div>
          </section>
        </div>

        <button type="button" onClick={onBack} className="auth-back-btn">
          <ArrowLeft className="h-4 w-4" />
          {backLabel}
        </button>
      </motion.div>
    </div>
  );
};
