import { useState, useEffect, useRef } from "react";

export const AnimatedCounter = ({ end, duration = 2, suffix = '', decimals = 0 }) => {
  const [count, setCount] = useState(0);
  const [hasAnimated, setHasAnimated] = useState(false);
  
  const ref = useRef(null);
  const observerRef = useRef(null);


  useEffect(() => {
    observerRef.current = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting && !hasAnimated) {
          setHasAnimated(true);
          
          const numericValue = parseFloat(end.replace(/[^0-9.]/g, ''));
          const startTime = Date.now();

          const updateCount = () => {
            const now = Date.now();
            const progress = Math.min((now - startTime) / (duration * 1000), 1);
            
            const easeOutQuart = 1 - Math.pow(1 - progress, 4);
            const currentCount = easeOutQuart * numericValue;
            
            setCount(currentCount);

            if (progress < 1) {
              requestAnimationFrame(updateCount);
            } else {
              setCount(numericValue);
              // Disconnect observer after animation completes
              if (observerRef.current && ref.current) {
                observerRef.current.disconnect();
              }
            }
          };

          requestAnimationFrame(updateCount);
        }
      },
      { threshold: 0.5 }
    );

    if (ref.current) {
      observerRef.current.observe(ref.current);
    }

    return () => {
      if (observerRef.current) {
        observerRef.current.disconnect();
      }
    };
  }, [end, duration, hasAnimated]);

  const formatNumber = (num) => {
    if (decimals > 0) {
      return num.toFixed(decimals);
    }
    return Math.floor(num).toString();
  };

  return (
    <span ref={ref}>
      {formatNumber(count)}{suffix}
    </span>
  );
};