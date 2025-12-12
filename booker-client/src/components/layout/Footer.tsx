'use client';

import { Github, Mail } from 'lucide-react';

export function Footer() {
  return (
    <footer className="bg-black border-t border-white/10 py-12">
      <div className="container mx-auto px-6 max-w-7xl">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 mb-8">
          {/* Brand Section */}
          <div>
            <h3 className="text-2xl font-bold text-violet-400 mb-3">BOOKER</h3>
            <p className="text-white/60 text-sm">
              사내 도서 대출 및 이벤트 관리 시스템
            </p>
          </div>

          {/* Services Links */}
          <div>
            <h4 className="text-white font-medium mb-4">서비스</h4>
            <ul className="space-y-2">
              <li>
                <a href="/books" className="text-white/60 hover:text-white text-sm transition-colors">
                  도서 검색
                </a>
              </li>
              <li>
                <a href="/events" className="text-white/60 hover:text-white text-sm transition-colors">
                  이벤트
                </a>
              </li>
              <li>
                <a href="/my-loans" className="text-white/60 hover:text-white text-sm transition-colors">
                  내 대출
                </a>
              </li>
              <li>
                <a href="/profile" className="text-white/60 hover:text-white text-sm transition-colors">
                  프로필
                </a>
              </li>
            </ul>
          </div>

          {/* Info Links */}
          <div>
            <h4 className="text-white font-medium mb-4">정보</h4>
            <ul className="space-y-2">
              <li>
                <a href="#" className="text-white/60 hover:text-white text-sm transition-colors">
                  이용약관
                </a>
              </li>
              <li>
                <a href="#" className="text-white/60 hover:text-white text-sm transition-colors">
                  개인정보처리방침
                </a>
              </li>
              <li>
                <a href="#" className="text-white/60 hover:text-white text-sm transition-colors">
                  공지사항
                </a>
              </li>
            </ul>
          </div>
        </div>

        {/* Bottom Section */}
        <div className="flex flex-col md:flex-row justify-between items-center pt-8 border-t border-white/5">
          <p className="text-white/40 text-sm mb-4 md:mb-0">
            © 2025 BOOKER. All rights reserved.
          </p>

          {/* Social Links */}
          <div className="flex items-center gap-4">
            <a
              href="https://github.com"
              target="_blank"
              rel="noopener noreferrer"
              className="text-white/60 hover:text-white transition-colors"
            >
              <Github size={20} />
            </a>
            <a
              href="mailto:contact@booker.com"
              className="text-white/60 hover:text-white transition-colors"
            >
              <Mail size={20} />
            </a>
          </div>
        </div>
      </div>
    </footer>
  );
}
