'use client';

import { Event } from '../types';
import { X, Calendar, Clock, MapPin, Users, CheckCircle } from 'lucide-react';

interface EventDetailModalProps {
  event: Event | null;
  isOpen: boolean;
  onClose: () => void;
  onRegister?: (eventId: string) => void;
}

export function EventDetailModal({ event, isOpen, onClose, onRegister }: EventDetailModalProps) {
  if (!isOpen || !event) return null;

  const availableSeats = event.maxParticipants - event.currentParticipants;
  const percentageFull = Math.round((event.currentParticipants / event.maxParticipants) * 100);

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('ko-KR', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
      <div className="bg-neutral-800 rounded-2xl w-full max-w-3xl max-h-[90vh] overflow-y-auto border border-neutral-700">
        {/* Header */}
        <div className="relative">
          <img
            src={event.imageUrl}
            alt={event.title}
            className="w-full h-64 object-cover"
          />
          <button
            onClick={onClose}
            className="absolute top-4 right-4 bg-black/50 hover:bg-black/70 text-white p-2 rounded-full transition-colors"
          >
            <X className="w-6 h-6" />
          </button>
          {event.status === 'active' && (
            <div className="absolute top-4 left-4">
              <span className="bg-status-available text-white text-sm px-4 py-2 rounded-full font-medium">
                Active
              </span>
            </div>
          )}
        </div>

        {/* Content */}
        <div className="p-8">
          <h2 className="text-3xl font-bold text-white mb-4">{event.title}</h2>

          <p className="text-gray-300 text-lg mb-6 leading-relaxed">
            {event.description}
          </p>

          {/* Event Details Grid */}
          <div className="bg-neutral-700/50 rounded-xl p-6 mb-6 space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="flex items-start">
                <Calendar className="w-5 h-5 text-status-available mr-3 mt-0.5" />
                <div>
                  <div className="text-gray-400 text-sm">Date</div>
                  <div className="text-white font-medium">{formatDate(event.startDate)}</div>
                </div>
              </div>

              <div className="flex items-start">
                <Clock className="w-5 h-5 text-status-available mr-3 mt-0.5" />
                <div>
                  <div className="text-gray-400 text-sm">Time</div>
                  <div className="text-white font-medium">
                    {event.startTime} - {event.endTime}
                  </div>
                </div>
              </div>

              <div className="flex items-start">
                <MapPin className="w-5 h-5 text-status-available mr-3 mt-0.5" />
                <div>
                  <div className="text-gray-400 text-sm">Location</div>
                  <div className="text-white font-medium">{event.location}</div>
                </div>
              </div>

              <div className="flex items-start">
                <Users className="w-5 h-5 text-status-available mr-3 mt-0.5" />
                <div>
                  <div className="text-gray-400 text-sm">Event Type</div>
                  <div className="text-white font-medium">{event.eventType}</div>
                </div>
              </div>
            </div>
          </div>

          {/* Seat Availability */}
          <div className="mb-6">
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-lg font-semibold text-white">Seat Availability</h3>
              <div className="text-right">
                <div className="text-2xl font-bold text-white">
                  {event.currentParticipants}
                  <span className="text-gray-400 text-lg">/{event.maxParticipants}</span>
                </div>
                <div className="text-sm text-gray-400">
                  {availableSeats} seats available
                </div>
              </div>
            </div>

            {/* Progress Bar */}
            <div className="bg-neutral-700 rounded-full h-4 overflow-hidden">
              <div
                className={`h-full transition-all duration-500 ${
                  percentageFull >= 90
                    ? 'bg-red-500'
                    : percentageFull >= 70
                    ? 'bg-yellow-500'
                    : 'bg-status-available'
                }`}
                style={{ width: `${percentageFull}%` }}
              >
                <div className="h-full flex items-center justify-end pr-2">
                  <span className="text-white text-xs font-medium">{percentageFull}%</span>
                </div>
              </div>
            </div>
          </div>

          {/* Requirements */}
          {event.requirements && event.requirements.length > 0 && (
            <div className="mb-6">
              <h3 className="text-lg font-semibold text-white mb-3">Requirements</h3>
              <div className="space-y-2">
                {event.requirements.map((req, index) => (
                  <div key={index} className="flex items-center text-gray-300">
                    <CheckCircle className="w-5 h-5 text-status-available mr-2 flex-shrink-0" />
                    <span>{req}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Action Button */}
          <div className="flex gap-3">
            {availableSeats > 0 && onRegister ? (
              <button
                onClick={() => onRegister(event.id)}
                className="flex-1 bg-status-available hover:bg-status-available/80 text-white font-semibold py-4 px-6 rounded-xl transition-all duration-200 shadow-lg hover:shadow-status-available/50"
              >
                Confirm Registration
              </button>
            ) : (
              <button
                disabled
                className="flex-1 bg-neutral-700 text-gray-400 font-semibold py-4 px-6 rounded-xl cursor-not-allowed"
              >
                Event Full
              </button>
            )}

            <button
              onClick={onClose}
              className="px-6 py-4 bg-neutral-700 hover:bg-neutral-600 text-white rounded-xl transition-colors"
            >
              Close
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
