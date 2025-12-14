'use client';

import { useState, useEffect, useRef } from 'react';

interface Notification {
  id: string;
  title: string;
  description: string;
  date: string;
  icon: string;
  link?: string;
}

// Mock notifications data
const mockNotifications: Notification[] = [
  {
    id: '1',
    title: '리더보드 오픈!',
    description: '다른 학습자들과 경쟁하며 학습 동기를 높여보세요. 퀴즈를 풀면 EXP를 얻고 순위가 올라갑니다.',
    date: '2025-11-20',
    icon: '🏆',
    link: '#'
  },
  {
    id: '2',
    title: '메뉴 구조 개선',
    description: '"오답노트"가 "복습하기"로 이름이 변경되었고, 더 쉽게 접근할 수 있도록 메뉴 순서를 조정했습니다.',
    date: '2025-11-20',
    icon: '🔄'
  },
  {
    id: '3',
    title: '망각곡선 기반 복습 시스템',
    description: '문제를 풀면 자동으로 복습 스케줄이 생성됩니다. 맞춘 문제는 간격이 늘어나고, 틀린 문제는 더 자주 나타납니다.',
    date: '2025-11-19',
    icon: '📚',
    link: '#'
  }
];

export function NotificationDropdown() {
  const [isOpen, setIsOpen] = useState(false);
  const [isFading, setIsFading] = useState(false);
  const [notifications, setNotifications] = useState<Notification[]>(mockNotifications);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Close dropdown when clicking outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsFading(true);
        setTimeout(() => {
          setIsOpen(false);
          setIsFading(false);
        }, 300);
      }
    }

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
      return () => document.removeEventListener('mousedown', handleClickOutside);
    }
  }, [isOpen]);

  // Close dropdown on scroll with fade out
  useEffect(() => {
    function handleScroll() {
      if (isOpen && !isFading) {
        setIsFading(true);
        setTimeout(() => {
          setIsOpen(false);
          setIsFading(false);
        }, 300);
      }
    }

    if (isOpen) {
      window.addEventListener('scroll', handleScroll, true);
      return () => window.removeEventListener('scroll', handleScroll, true);
    }
  }, [isOpen, isFading]);

  const unreadCount = notifications.length;

  return (
    <div className="relative" ref={dropdownRef}>
      {/* Notification Bell Button */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="relative p-2 text-white/70 hover:text-white transition-colors"
        aria-label="알림"
      >
        {/* Bell Icon */}
        <svg
          xmlns="http://www.w3.org/2000/svg"
          width="24"
          height="24"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
          <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
        </svg>

        {/* Badge - Hidden for now */}
        {false && unreadCount > 0 && (
          <span className="absolute -top-1 -right-1 flex items-center justify-center min-w-[20px] h-5 px-1 text-xs font-medium text-white bg-red-500 rounded-full">
            {unreadCount}
          </span>
        )}
      </button>

      {/* Dropdown */}
      {isOpen && (
        <div className={`absolute right-0 top-full mt-2 w-[400px] bg-gray-900 rounded-2xl border border-white/10 shadow-2xl overflow-hidden transition-opacity duration-300 ${isFading ? 'opacity-0' : 'opacity-100'}`}>
          {/* Header */}
          <div className="px-6 py-4 border-b border-white/10">
            <div className="flex items-center gap-2 text-white">
              <span className="text-lg">📢</span>
              <h3 className="text-lg font-medium">업데이트 소식</h3>
            </div>
          </div>

          {/* Notifications List */}
          <div className="max-h-[500px] overflow-y-auto">
            {notifications.map((notification) => (
              <div
                key={notification.id}
                className="px-6 py-4 border-b border-white/5 hover:bg-white/5 transition-colors"
              >
                <div className="flex gap-4">
                  {/* Icon */}
                  <div className="flex-shrink-0 w-10 h-10 flex items-center justify-center bg-white/5 rounded-lg">
                    <span className="text-xl">{notification.icon}</span>
                  </div>

                  {/* Content */}
                  <div className="flex-1 min-w-0">
                    <h4 className="text-white font-medium mb-1">
                      {notification.title}
                    </h4>
                    <p className="text-white/60 text-sm mb-3 line-clamp-3">
                      {notification.description}
                    </p>

                    <div className="flex items-center justify-between">
                      <span className="text-white/40 text-xs">
                        {notification.date}
                      </span>
                      {notification.link && (
                        <a
                          href={notification.link}
                          className="text-green-400 text-sm hover:text-green-300 transition-colors inline-flex items-center gap-1"
                        >
                          자세히 보기
                          <svg
                            xmlns="http://www.w3.org/2000/svg"
                            width="14"
                            height="14"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth="2"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                          >
                            <path d="M5 12h14" />
                            <path d="m12 5 7 7-7 7" />
                          </svg>
                        </a>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* Footer */}
          <div className="px-6 py-4 bg-white/5">
            <button
              onClick={() => {
                setIsFading(true);
                setTimeout(() => {
                  setIsOpen(false);
                  setIsFading(false);
                }, 300);
              }}
              className="w-full py-2 text-white/60 hover:text-white text-sm transition-colors"
            >
              닫기
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
