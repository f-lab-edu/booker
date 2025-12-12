'use client';

import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
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

  useEffect(() => {
    setMounted(true);
  }, []);

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
    <section className="relative min-h-screen w-full bg-black flex items-end overflow-hidden">
      {/* WebGL Shader Background */}
      <div className="absolute inset-0">
        <ShaderCanvas />
      </div>

      {/* Main Content - Bottom Left */}
      <main className="absolute bottom-8 left-8 z-20 max-w-2xl px-6">
        <div className="text-left">
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
            className="text-5xl md:text-6xl lg:text-7xl tracking-tight font-light text-white mb-6 leading-tight"
          >
            모든 지식을 한 곳에서
            <br />
            <span className="font-normal">BOOKER</span>
          </motion.h1>

          {/* Description */}
          <motion.p
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.4 }}
            className="text-sm font-light text-white/70 mb-6 leading-relaxed max-w-xl"
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
              className="flex items-center gap-6 mb-8"
            >
              {totalBooks > 0 && (
                <div>
                  <div className="text-3xl font-light text-white mb-1">
                    {totalBooks.toLocaleString()}
                  </div>
                  <div className="text-xs text-white/60">전체 도서</div>
                </div>
              )}

              {activeLoanCount > 0 && (
                <div>
                  <div className="text-3xl font-light text-violet-400 mb-1">
                    {activeLoanCount.toLocaleString()}
                  </div>
                  <div className="text-xs text-white/60">대출 중</div>
                </div>
              )}
            </motion.div>
          )}

          {/* Buttons */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.6 }}
            className="flex items-center gap-4 flex-wrap"
          >
            <a
              href="/books"
              className="px-8 py-3 rounded-full bg-white text-black font-normal text-xs transition-all duration-200 hover:bg-white/90"
            >
              도서 검색하기
            </a>
            <a
              href="/events"
              className="px-8 py-3 rounded-full bg-transparent border border-white/30 text-white font-normal text-xs transition-all duration-200 hover:bg-white/10 hover:border-white/50"
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
