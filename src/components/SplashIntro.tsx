import React, {useEffect} from 'react';
import {motion, AnimatePresence} from 'motion/react';
import {ByaheroMotorcycleVector, ByaheroWordmarkVector} from './VectorGraphics';

interface SplashIntroProps {
  visible: boolean;
  onFinish: () => void;
}

export const SplashIntro: React.FC<SplashIntroProps> = ({visible, onFinish}) => {
  useEffect(() => {
    if (!visible) return;
    const timer = window.setTimeout(() => {
      onFinish();
    }, 2300);
    return () => window.clearTimeout(timer);
  }, [visible, onFinish]);

  return (
    <AnimatePresence>
      {visible && (
        <motion.div
          key="byahero-splash"
          initial={{opacity: 1}}
          exit={{opacity: 0, scale: 1.03}}
          transition={{duration: 0.45, ease: [0.16, 1, 0.3, 1]}}
          className="fixed inset-0 z-[100] flex flex-col items-center justify-center bg-[#060908] text-white px-6 overflow-hidden select-none"
        >
          {/* Subtle ambient dark-green radial glow */}
          <div
            className="pointer-events-none absolute inset-0"
            style={{
              background:
                'radial-gradient(circle at 50% 48%, rgba(6, 78, 59, 0.58) 0%, rgba(9, 11, 10, 0.96) 62%, #050706 100%)',
            }}
          />

          {/* Center Vector Motorcycle + BYAHERO + by HDDEV */}
          <div className="relative z-10 flex flex-col items-center text-center">
            <motion.div
              initial={{x: -90, opacity: 0, scale: 0.9}}
              animate={{x: 0, opacity: 1, scale: 1}}
              transition={{duration: 0.85, ease: [0.16, 1, 0.3, 1]}}
              className="mb-5"
            >
              <ByaheroMotorcycleVector className="w-52 h-28 drop-shadow-[0_14px_28px_rgba(16,185,129,0.25)]" />
            </motion.div>

            {/* Vector Road Divider Line */}
            <motion.div
              initial={{scaleX: 0, opacity: 0}}
              animate={{scaleX: 1, opacity: 1}}
              transition={{duration: 0.7, delay: 0.25, ease: [0.16, 1, 0.3, 1]}}
              className="w-48 h-[2px] bg-gradient-to-r from-transparent via-emerald-400 to-transparent mb-6 origin-center"
            />

            <motion.div
              initial={{y: 18, opacity: 0, scale: 0.96}}
              animate={{y: 0, opacity: 1, scale: 1}}
              transition={{duration: 0.75, delay: 0.35, ease: [0.16, 1, 0.3, 1]}}
              className="flex items-center justify-center"
            >
              <ByaheroWordmarkVector
                lightSurface={false}
                className="h-11 sm:h-14 w-auto drop-shadow-[0_10px_24px_rgba(16,185,129,0.28)]"
              />
            </motion.div>

            <motion.p
              initial={{y: 10, opacity: 0}}
              animate={{y: 0, opacity: 1}}
              transition={{duration: 0.6, delay: 0.65, ease: [0.16, 1, 0.3, 1]}}
              className="mt-2.5 text-sm font-medium tracking-[0.22em] text-emerald-300"
            >
              by HDDEV
            </motion.p>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
};
