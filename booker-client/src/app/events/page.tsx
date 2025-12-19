'use client';

import { useState } from 'react';
import { Plus } from 'lucide-react';
import { EventCard } from './components/EventCard';
import { EventStats } from './components/EventStats';
import { CreateEventModal } from './components/CreateEventModal';
import { EventDetailModal } from './components/EventDetailModal';
import { mockEvents, mockStats } from './mockData';
import { Event } from './types';

export default function EventsPage() {
  const [events, setEvents] = useState(mockEvents);
  const [stats, setStats] = useState(mockStats);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [selectedEvent, setSelectedEvent] = useState<Event | null>(null);

  const handleCreateEvent = (eventData: any) => {
    console.log('Creating event:', eventData);
    // TODO: Implement API call to create event
  };

  const handleRegister = (eventId: string) => {
    console.log('Registering for event:', eventId);
    // TODO: Implement API call to register for event
    alert('Registration successful! (Mock)');
  };

  const handleEdit = (eventId: string) => {
    const event = events.find((e) => e.id === eventId);
    if (event) {
      setSelectedEvent(event);
      setIsDetailModalOpen(true);
    }
  };

  const handleCancel = (eventId: string) => {
    if (confirm('Are you sure you want to cancel this event?')) {
      console.log('Cancelling event:', eventId);
      // TODO: Implement API call to cancel event
      alert('Event cancelled! (Mock)');
    }
  };

  const handleEventClick = (event: Event) => {
    setSelectedEvent(event);
    setIsDetailModalOpen(true);
  };

  return (
    <main className="min-h-screen bg-neutral-900 pb-24 pt-8 px-4 md:px-6">
      <div className="container mx-auto max-w-7xl">
        {/* Header */}
        <div className="flex flex-col md:flex-row md:items-center md:justify-between mb-8">
          <div>
            <h1 className="text-4xl md:text-5xl font-bold text-white mb-2">
              Events & Tech Talks
            </h1>
            <p className="text-gray-400 text-lg">
              Join us for workshops, tech talks, and team building events
            </p>
          </div>

          <button
            onClick={() => setIsCreateModalOpen(true)}
            className="mt-4 md:mt-0 inline-flex items-center gap-2 bg-primary hover:bg-primary-dark text-white font-semibold px-6 py-3 rounded-xl transition-all duration-200 shadow-lg hover:shadow-primary/50"
          >
            <Plus className="w-5 h-5" />
            Add Event
          </button>
        </div>

        {/* Statistics */}
        <EventStats stats={stats} />

        {/* Events Section */}
        <div className="mb-6">
          <h2 className="text-2xl font-bold text-white mb-6">Upcoming Events</h2>

          {events.length === 0 ? (
            <div className="text-center py-16 bg-neutral-800 rounded-2xl border border-neutral-700">
              <p className="text-gray-400 text-lg mb-4">No upcoming events</p>
              <button
                onClick={() => setIsCreateModalOpen(true)}
                className="inline-flex items-center gap-2 bg-primary hover:bg-primary-dark text-white font-semibold px-6 py-3 rounded-xl transition-all duration-200"
              >
                <Plus className="w-5 h-5" />
                Create First Event
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {events.map((event) => (
                <div key={event.id} onClick={() => handleEventClick(event)} className="cursor-pointer">
                  <EventCard
                    event={event}
                    onEdit={handleEdit}
                    onCancel={handleCancel}
                    onRegister={handleRegister}
                  />
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Modals */}
      <CreateEventModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onSubmit={handleCreateEvent}
      />

      <EventDetailModal
        event={selectedEvent}
        isOpen={isDetailModalOpen}
        onClose={() => {
          setIsDetailModalOpen(false);
          setSelectedEvent(null);
        }}
        onRegister={handleRegister}
      />
    </main>
  );
}
