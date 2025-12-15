'use client';

import { motion } from 'framer-motion';

export function ValueProposition() {
  return (
    <section className="py-32 bg-gradient-to-b from-gray-950 to-black">
      <div className="container mx-auto px-6 max-w-6xl">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.8 }}
          className="text-center space-y-8"
        >
          {/* Main Message */}
          <h2 className="text-5xl md:text-6xl lg:text-7xl font-bold tracking-wide" style={{ lineHeight: '1.35' }}>
            <span className="text-white/90">활발한 </span>
            <span className="text-green-500">공유,</span>
            <br />
            <span className="text-white/90">간편한 </span>
            <span className="text-green-400/75">대출,</span>
            <br />
            <span className="text-white/90">빠른 </span>
            <span className="text-green-300/50">반납.</span>
          </h2>

          {/* Sub Message */}
          <motion.p
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.8, delay: 0.3 }}
            className="text-lg md:text-xl text-white/60 max-w-3xl mx-auto leading-relaxed"
          >
            동료와 인사이트를 공유하고, 효율적이게 성장해요.
          </motion.p>
        </motion.div>
      </div>
    </section>
  );
}
