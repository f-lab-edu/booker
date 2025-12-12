'use client';

import { useEffect, useState } from 'react';
import { motion, useMotionValue, useSpring, useTransform } from 'framer-motion';
import { ShaderCanvas } from './ShaderCanvas';
import { Sparkles } from 'lucide-react';

interface HeroBannerProps {
  totalBooks?: number;
  activeLoanCount?: number;
}

export function HeroBanner({
  totalBooks = 0,
  activeLoanCount = 0
}: HeroBannerProps) {
  const [mounted, setMounted] = useState(false);

  const mouseX = useMotionValue(0);
  const mouseY = useMotionValue(0);

  const springConfig = { damping: 25, stiffness: 150 };
  const mouseXSpring = useSpring(mouseX, springConfig);
  const mouseYSpring = useSpring(mouseY, springConfig);

  const background1 = useTransform(
    [mouseXSpring, mouseYSpring],
    (latest) => {
      const [x, y] = latest;
      return `radial-gradient(600px circle at ${x}px ${y}px, rgba(139, 92, 246, 0.15), transparent 80%)`;
    }
  );

  const background2 = useTransform(
    [mouseXSpring, mouseYSpring],
    (latest) => {
      const [x, y] = latest;
      return `radial-gradient(500px circle at ${x}px ${y}px, rgba(99, 102, 241, 0.1), transparent 70%)`;
    }
  );

  const background3 = useTransform(
    [mouseXSpring, mouseYSpring],
    (latest) => {
      const [x, y] = latest;
      return `radial-gradient(400px circle at ${x}px ${y}px, rgba(167, 139, 250, 0.08), transparent 60%)`;
    }
  );

  useEffect(() => {
    setMounted(true);

    const handleMouseMove = (e: MouseEvent) => {
      mouseX.set(e.clientX);
      mouseY.set(e.clientY);
    };

    window.addEventListener('mousemove', handleMouseMove);
    return () => window.removeEventListener('mousemove', handleMouseMove);
  }, [mouseX, mouseY]);

  if (!mounted) {
    return (
      <section className="relative min-h-screen w-full bg-black flex items-end overflow-hidden">
        <div className="absolute bottom-8 left-8 z-10 max-w-2xl">
          <h1 className="text-5xl md:text-6xl text-white font-light tracking-tight">
            BOOKER
          </h1>
        </div>
      </section>
    );
  }

  return (
    <section className="relative min-h-screen w-full flex items-center justify-center overflow-hidden" style={{
      background: 'radial-gradient(ellipse at top left, #1e3a5f 0%, #2d1b4e 30%, #1a1625 60%, #0a0a0f 100%)'
    }}>
      {/* Cursor Light Effect - Ripple 1 */}
      <motion.div
        className="pointer-events-none fixed inset-0 z-10"
        style={{
          background: background1
        }}
      />

      {/* Cursor Light Effect - Ripple 2 (Delayed) */}
      <motion.div
        className="pointer-events-none fixed inset-0 z-10"
        style={{
          background: background2
        }}
      />

      {/* Cursor Light Effect - Ripple 3 (Most Delayed) */}
      <motion.div
        className="pointer-events-none fixed inset-0 z-10"
        style={{
          background: background3
        }}
      />

      {/* Main Content - Center */}
      <main className="relative z-20 max-w-5xl px-6">
        <div className="text-center">
          {/* Badge */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.1 }}
            className="inline-flex items-center px-3 py-1.5 rounded-full bg-white/5 backdrop-blur-sm border border-white/10 mb-6"
          >
            <Sparkles size={12} className="text-white/70 mr-2" />
            <span className="text-white/80 text-xs font-light">
              사내 도서 대출 및 이벤트 관리 시스템
            </span>
          </motion.div>

          {/* Main Heading */}
          <motion.h1
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.2 }}
            className="text-6xl md:text-7xl lg:text-8xl tracking-tight font-bold text-white mb-8 leading-tight"
          >
            모든 지식을 한 곳에서
          </motion.h1>

          {/* Description */}
          <motion.p
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.4 }}
            className="text-lg md:text-xl font-light text-white/70 mb-16 leading-relaxed max-w-2xl mx-auto"
          >
            간편한 도서 대출과 반납으로 팀의 생산성을 높이고,
            테크톡과 이벤트로 지식을 공유하는 스마트한 플랫폼.
          </motion.p>

          {/* Stats */}
          {(totalBooks > 0 || activeLoanCount > 0) && (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.8, delay: 0.5 }}
              className="flex items-center justify-center gap-6 mb-8"
            >
              {totalBooks > 0 && (
                <div>
                  <div className="text-5xl md:text-6xl font-light text-white mb-2">
                    {totalBooks.toLocaleString()}
                  </div>
                  <div className="text-sm text-white/60">전체 도서</div>
                </div>
              )}

              {activeLoanCount > 0 && (
                <div>
                  <div className="text-5xl md:text-6xl font-light text-violet-400 mb-2">
                    {activeLoanCount.toLocaleString()}
                  </div>
                  <div className="text-sm text-white/60">대출 중</div>
                </div>
              )}
            </motion.div>
          )}

          {/* Buttons */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.6 }}
            className="flex items-center justify-center gap-4 flex-wrap"
          >
            <a
              href="/books"
              className="px-10 py-4 rounded-full bg-white text-black font-medium text-base transition-all duration-200 hover:bg-white/90"
            >
              도서 검색하기
            </a>
            <a
              href="/events"
              className="px-10 py-4 rounded-full bg-transparent border border-white/30 text-white font-medium text-base transition-all duration-200 hover:bg-white/10 hover:border-white/50"
            >
              이벤트 보기
            </a>
          </motion.div>
        </div>
      </main>

      {/* Scroll Indicator - Bottom Center */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 1, delay: 1 }}
        className="absolute bottom-8 left-1/2 -translate-x-1/2 z-10 flex flex-col items-center gap-2"
      >
        <p className="text-white/60 text-xs font-light">Scroll to explore</p>
        <motion.div
          animate={{ y: [0, 8, 0] }}
          transition={{ duration: 1.5, repeat: Infinity }}
          className="w-5 h-8 border border-white/30 rounded-full flex items-start justify-center p-1"
        >
          <div className="w-1 h-1 bg-white/60 rounded-full" />
        </motion.div>
      </motion.div>
    </section>
  );
}
