'use client';

import { motion } from 'framer-motion';
import { Calendar, Users } from 'lucide-react';

interface Event {
  id: number;
  title: string;
  eventType: string;
  startDateTime: string;
  location?: string;
  currentParticipants: number;
  maxParticipants?: number;
}

const mockEvents: Event[] = [
  {
    id: 1,
    title: 'React 19 새로운 기능 소개',
    eventType: 'TECH_TALK',
    startDateTime: '2025-12-15T14:00:00',
    location: '회의실 A',
    currentParticipants: 12,
    maxParticipants: 20,
  },
  {
    id: 2,
    title: 'TypeScript 고급 활용법',
    eventType: 'WORKSHOP',
    startDateTime: '2025-12-18T15:00:00',
    location: '회의실 B',
    currentParticipants: 8,
    maxParticipants: 15,
  },
  {
    id: 3,
    title: '클린 아키텍처 북 스터디',
    eventType: 'MEETUP',
    startDateTime: '2025-12-20T16:00:00',
    location: '온라인',
    currentParticipants: 5,
    maxParticipants: 10,
  },
];

const eventTypeColors: Record<string, string> = {
  TECH_TALK: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
  WORKSHOP: 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20',
  MEETUP: 'bg-purple-500/10 text-purple-400 border-purple-500/20',
  OTHER: 'bg-green-500/10 text-green-400 border-green-500/20',
};

const eventTypeLabels: Record<string, string> = {
  TECH_TALK: '테크톡',
  WORKSHOP: '워크샵',
  MEETUP: '밋업',
  OTHER: '기타',
};

function formatDate(dateString: string) {
  const date = new Date(dateString);
  const month = date.getMonth() + 1;
  const day = date.getDate();
  const hours = date.getHours();
  const minutes = date.getMinutes();
  return `${month}월 ${day}일 ${hours}:${minutes.toString().padStart(2, '0')}`;
}

export function EventList() {
  // 무한 스크롤을 위해 배열을 복제
  const duplicatedEvents = [...mockEvents, ...mockEvents];

  return (
    <section className="py-20 bg-gradient-to-b from-black to-gray-950 overflow-hidden">
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
            <Calendar size={20} className="text-green-400 mr-2" />
            <span className="text-green-400 text-sm font-medium">이벤트 목록</span>
          </div>
          <p className="text-white/60 text-sm">다가오는 이벤트에 참여하세요</p>
        </motion.div>

        {/* Scrolling Event Cards */}
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
                duration: 13,
                ease: 'linear',
              },
            }}
          >
            {duplicatedEvents.map((event, index) => (
              <div
                key={`${event.id}-${index}`}
                className="group relative p-6 rounded-2xl bg-white/5 backdrop-blur-sm border border-white/10 hover:bg-white/10 hover:border-green-500/30 transition-all duration-300 cursor-pointer flex-shrink-0 w-80"
              >
                {/* Event Type Badge */}
                <div className="mb-4">
                  <span className={`inline-block px-3 py-1 rounded-full text-xs font-medium border ${eventTypeColors[event.eventType]}`}>
                    {eventTypeLabels[event.eventType]}
                  </span>
                </div>

                {/* Event Title */}
                <h3 className="text-lg font-semibold text-white mb-3 group-hover:text-green-400 transition-colors">
                  {event.title}
                </h3>

                {/* Event Details */}
                <div className="space-y-2 mb-4">
                  <div className="flex items-center text-sm text-white/60">
                    <Calendar size={14} className="mr-2" />
                    {formatDate(event.startDateTime)}
                  </div>
                  {event.location && (
                    <div className="flex items-center text-sm text-white/60">
                      <span className="mr-2">📍</span>
                      {event.location}
                    </div>
                  )}
                </div>

                {/* Participants */}
                <div className="flex items-center justify-between pt-4 border-t border-white/5">
                  <div className="flex items-center text-sm text-white/60">
                    <Users size={14} className="mr-2" />
                    참여자
                  </div>
                  <span className="text-sm font-medium text-white">
                    {event.currentParticipants}
                    {event.maxParticipants && `/${event.maxParticipants}`}
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
          transition={{ duration: 0.6, delay: 0.3 }}
          className="text-center mt-12"
        >
          <a
            href="/events"
            className="inline-block px-8 py-3 rounded-full bg-white/5 border border-white/10 text-white font-medium text-sm hover:bg-white/10 hover:border-green-500/30 transition-all duration-200"
          >
            전체 이벤트 보기
          </a>
        </motion.div>
      </div>
    </section>
  );
}
