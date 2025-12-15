'use client';

import { motion } from 'framer-motion';
import { BookOpen } from 'lucide-react';

interface Book {
  id: number;
  title: string;
  author: string;
  publisher?: string;
  availableCopies: number;
}

const mockBooks: Book[] = [
  { id: 1, title: '클린 코드', author: '로버트 C. 마틴', publisher: '인사이트', availableCopies: 3 },
  { id: 2, title: '이펙티브 자바', author: '조슈아 블로크', publisher: '인사이트', availableCopies: 5 },
  { id: 3, title: '리팩토링 2판', author: '마틴 파울러', publisher: '한빛미디어', availableCopies: 2 },
  { id: 4, title: 'HTTP 완벽 가이드', author: '데이빗 고울리', publisher: '인사이트', availableCopies: 4 },
  { id: 5, title: '도메인 주도 설계', author: '에릭 에반스', publisher: '위키북스', availableCopies: 1 },
  { id: 6, title: '실용주의 프로그래머', author: '데이비드 토머스', publisher: '인사이트', availableCopies: 3 },
];

export function BookList() {
  // 무한 스크롤을 위해 배열을 복제
  const duplicatedBooks = [...mockBooks, ...mockBooks];

  return (
    <section className="py-20 bg-black overflow-hidden">
      <div className="container mx-auto px-6 max-w-7xl">
        {/* Section Header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
          className="text-center mb-12"
        >
          <div className="inline-flex items-center px-4 py-2 rounded-full bg-green-500/10 border border-green-500/20 mb-4">
            <BookOpen size={20} className="text-green-400 mr-2" />
            <span className="text-green-400 text-sm font-medium">도서 목록</span>
          </div>
          <p className="text-white/60 text-sm">최근 등록된 도서를 확인하세요</p>
        </motion.div>

        {/* Stats Grid */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.8, delay: 0.3 }}
          className="grid grid-cols-1 md:grid-cols-3 gap-8 mb-16"
        >
          <div className="text-center">
            <div className="text-4xl font-bold text-green-400 mb-2">24/7</div>
            <div className="text-white/60 text-sm">언제든지 대출 가능</div>
          </div>
          <div className="text-center">
            <div className="text-4xl font-bold text-yellow-200 mb-2">2주</div>
            <div className="text-white/60 text-sm">대출 기간 (연장 가능)</div>
          </div>
          <div className="text-center">
            <div className="text-4xl font-bold text-green-300 mb-2">무료</div>
            <div className="text-white/60 text-sm">모든 서비스 이용료</div>
          </div>
        </motion.div>

        {/* Scrolling Book Cards */}
        <div className="relative">
          <motion.div
            className="flex gap-6"
            animate={{
              x: ['0%', '-50%'],
            }}
            transition={{
              x: {
                repeat: Infinity,
                repeatType: 'loop',
                duration: 15,
                ease: 'linear',
              },
            }}
          >
            {duplicatedBooks.map((book, index) => (
              <div
                key={`${book.id}-${index}`}
                className="group relative p-6 rounded-2xl bg-white/5 backdrop-blur-sm border border-white/10 hover:bg-white/10 hover:border-green-500/30 transition-all duration-300 cursor-pointer flex-shrink-0 w-80"
              >
                {/* Book Title */}
                <h3 className="text-lg font-semibold text-white mb-2 group-hover:text-green-400 transition-colors">
                  {book.title}
                </h3>

                {/* Author & Publisher */}
                <p className="text-sm text-white/60 mb-4">
                  {book.author}
                  {book.publisher && ` · ${book.publisher}`}
                </p>

                {/* Available Copies Badge */}
                <div className="flex items-center justify-between">
                  <span className="text-xs text-white/40">재고</span>
                  <span className={`px-3 py-1 rounded-full text-xs font-medium ${
                    book.availableCopies > 3
                      ? 'bg-green-500/10 text-green-400 border border-green-500/20'
                      : book.availableCopies > 0
                      ? 'bg-yellow-500/10 text-yellow-400 border border-yellow-500/20'
                      : 'bg-red-500/10 text-red-400 border border-red-500/20'
                  }`}>
                    {book.availableCopies}권
                  </span>
                </div>
              </div>
            ))}
          </motion.div>
        </div>

        {/* View More Button */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6, delay: 0.6 }}
          className="text-center mt-12"
        >
          <a
            href="/books"
            className="inline-block px-8 py-3 rounded-full bg-white/5 border border-white/10 text-white font-medium text-sm hover:bg-white/10 hover:border-green-500/30 transition-all duration-200"
          >
            전체 도서 보기
          </a>
        </motion.div>
      </div>
    </section>
  );
}
