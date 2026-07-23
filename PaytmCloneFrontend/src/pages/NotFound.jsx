import { useLocation, Link } from "react-router-dom";
import { useEffect } from "react";
import { motion } from "framer-motion";
import { AlertTriangle, ArrowLeft } from "lucide-react";
import { LABELS } from "@/config/labels.config";

const NotFound = () => {
  const labels = LABELS.pages.notFoundPage;
  const MotionDiv = motion.div;
  const location = useLocation();

  useEffect(() => {
    console.error(labels.consolePrefix, location.pathname);
  }, [labels.consolePrefix, location.pathname]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-muted px-4">
      <MotionDiv
        initial={{ opacity: 0, y: 40 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6, ease: "easeOut" }}
        className="text-center"
      >
        <MotionDiv
          animate={{ rotate: [0, -5, 5, 0] }}
          transition={{ duration: 1.5, repeat: Infinity, repeatDelay: 3 }}
          className="mx-auto mb-6 flex h-20 w-20 items-center justify-center rounded-full bg-primary/10"
        >
          <AlertTriangle className="h-10 w-10 text-primary" />
        </MotionDiv>

        <h1 className="mb-2 text-5xl font-extrabold tracking-tight">{labels.notFoundCode}</h1>

        <p className="mb-6 text-lg text-muted-foreground">{labels.description}</p>

        <MotionDiv whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}>
          <Link
            to="/"
            className="inline-flex items-center gap-2 rounded-xl bg-primary px-6 py-3 text-sm font-medium text-primary-foreground shadow hover:bg-primary/90"
          >
            <ArrowLeft className="h-4 w-4" />
            {labels.backToHome}
          </Link>
        </MotionDiv>
      </MotionDiv>
    </div>
  );
};

export default NotFound;
