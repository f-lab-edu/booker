import { apiClient } from './client';
import { Event, EventType, Page } from './types';

export const eventApi = {
  /**
   * Get all events with optional type filter
   */
  getEvents: async (type?: EventType, page = 0, size = 20): Promise<Page<Event>> => {
    const params = new URLSearchParams();
    if (type) params.append('type', type);
    params.append('page', page.toString());
    params.append('size', size.toString());

    return apiClient<Page<Event>>(`/api/events?${params.toString()}`);
  },

  /**
   * Get event by ID
   */
  getEvent: async (id: number): Promise<Event> => {
    return apiClient<Event>(`/api/events/${id}`);
  },

  /**
   * Create a new event
   */
  createEvent: async (event: Omit<Event, 'id' | 'presenter' | 'participants' | 'currentParticipants'>): Promise<Event> => {
    return apiClient<Event>('/api/events', {
      method: 'POST',
      body: JSON.stringify(event),
    });
  },

  /**
   * Update an event
   */
  updateEvent: async (id: number, event: Partial<Event>): Promise<void> => {
    return apiClient<void>(`/api/events/${id}`, {
      method: 'PUT',
      body: JSON.stringify(event),
    });
  },

  /**
   * Delete an event
   */
  deleteEvent: async (id: number): Promise<void> => {
    return apiClient<void>(`/api/events/${id}`, {
      method: 'DELETE',
    });
  },

  /**
   * Add a participant to an event
   */
  addParticipant: async (eventId: number, memberId: string): Promise<void> => {
    return apiClient<void>(`/api/events/${eventId}/participants?memberId=${memberId}`, {
      method: 'POST',
    });
  },

  /**
   * Remove a participant from an event
   */
  removeParticipant: async (eventId: number, memberId: string): Promise<void> => {
    return apiClient<void>(`/api/events/${eventId}/participants/${memberId}`, {
      method: 'DELETE',
    });
  },
};
