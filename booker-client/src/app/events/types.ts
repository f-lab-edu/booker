export interface Event {
  id: string;
  title: string;
  description: string;
  imageUrl: string;
  startDate: string;
  endDate: string;
  startTime: string;
  endTime: string;
  location: string;
  eventType: string;
  maxParticipants: number;
  currentParticipants: number;
  requirements: string[];
  status: 'active' | 'cancelled' | 'completed';
}

export interface EventStats {
  activeEvents: number;
  activeEventsThisWeek: number;
  totalRegistrations: number;
  totalEventsAllTime: number;
}
