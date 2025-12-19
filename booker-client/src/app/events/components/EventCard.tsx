'use client';

import { Event } from '../types';
import { CalendarDays, Clock, MapPin, Users } from 'lucide-react';

interface EventCardProps {
  event: Event;
  onEdit?: (eventId: string) => void;
  onCancel?: (eventId: string) => void;
  onRegister?: (eventId: string) => void;
}

export function EventCard({ event, onEdit, onCancel, onRegister }: EventCardProps) {
  const availableSeats = event.maxParticipants - event.currentParticipants;
  const isAlmostFull = availableSeats <= 5 && availableSeats > 0;
  const isFull = availableSeats <= 0;

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('ko-KR', { month: 'long', day: 'numeric', year: 'numeric' });
  };

  return (
    <div className="bg-neutral-800 rounded-2xl overflow-hidden border border-neutral-700 hover:border-neutral-600 transition-all duration-300 group">
      {/* Event Image */}
      <div className="relative h-48 overflow-hidden">
        <img
          src={event.imageUrl}
          alt={event.title}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
        />
        {event.status === 'active' && (
          <div className="absolute top-4 right-4">
            <span className="bg-status-available text-white text-xs px-3 py-1 rounded-full font-medium">
              Active
            </span>
          </div>
        )}
      </div>

      {/* Event Details */}
      <div className="p-6">
        <h3 className="text-xl font-bold text-white mb-2 group-hover:text-status-available transition-colors">
          {event.title}
        </h3>

        <p className="text-gray-400 text-sm mb-4 line-clamp-2">
          {event.description}
        </p>

        {/* Event Info */}
        <div className="space-y-2 mb-4">
          <div className="flex items-center text-gray-300 text-sm">
            <CalendarDays className="w-4 h-4 mr-2 text-status-available" />
            <span>{formatDate(event.startDate)}</span>
          </div>

          <div className="flex items-center text-gray-300 text-sm">
            <Clock className="w-4 h-4 mr-2 text-status-available" />
            <span>{event.startTime} - {event.endTime}</span>
          </div>

          <div className="flex items-center text-gray-300 text-sm">
            <MapPin className="w-4 h-4 mr-2 text-status-available" />
            <span>{event.location}</span>
          </div>

          <div className="flex items-center text-gray-300 text-sm">
            <Users className="w-4 h-4 mr-2 text-status-available" />
            <span className={isAlmostFull ? 'text-yellow-500' : isFull ? 'text-red-500' : ''}>
              {event.currentParticipants}/{event.maxParticipants} registered
            </span>
            {isAlmostFull && (
              <span className="ml-2 text-yellow-500 text-xs">• {availableSeats} seats left</span>
            )}
            {isFull && (
              <span className="ml-2 text-red-500 text-xs">• Full</span>
            )}
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex gap-2">
          {!isFull && onRegister && (
            <button
              onClick={() => onRegister(event.id)}
              className="flex-1 bg-status-available hover:bg-status-available/80 text-white font-semibold py-2.5 px-4 rounded-lg transition-all duration-200"
            >
              Register
            </button>
          )}

          {isFull && (
            <button
              disabled
              className="flex-1 bg-neutral-700 text-gray-400 font-semibold py-2.5 px-4 rounded-lg cursor-not-allowed"
            >
              Full
            </button>
          )}

          {onEdit && (
            <button
              onClick={() => onEdit(event.id)}
              className="px-4 py-2.5 bg-neutral-700 hover:bg-neutral-600 text-white rounded-lg transition-colors"
            >
              Edit
            </button>
          )}

          {onCancel && (
            <button
              onClick={() => onCancel(event.id)}
              className="px-4 py-2.5 bg-neutral-700 hover:bg-red-900/50 text-white rounded-lg transition-colors"
            >
              Cancel
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
