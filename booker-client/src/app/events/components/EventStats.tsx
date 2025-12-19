'use client';

import { Calendar, Users } from 'lucide-react';
import { EventStats as EventStatsType } from '../types';

interface EventStatsProps {
  stats: EventStatsType;
}

export function EventStats({ stats }: EventStatsProps) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-8">
      {/* Active Events */}
      <div className="bg-gradient-to-br from-primary/20 to-primary/5 border border-primary/30 rounded-2xl p-6 backdrop-blur-sm">
        <div className="flex items-start justify-between">
          <div>
            <div className="flex items-center gap-2 mb-2">
              <Calendar className="w-5 h-5 text-primary" />
              <h3 className="text-gray-300 text-sm font-medium">Active Events</h3>
            </div>
            <div className="text-4xl font-bold text-white mb-1">{stats.activeEvents}</div>
            <p className="text-gray-400 text-sm">{stats.activeEventsThisWeek} new this week</p>
          </div>
          <div className="bg-primary/20 p-3 rounded-xl">
            <Calendar className="w-6 h-6 text-primary" />
          </div>
        </div>
      </div>

      {/* Total Registrations */}
      <div className="bg-gradient-to-br from-status-available/20 to-status-available/5 border border-status-available/30 rounded-2xl p-6 backdrop-blur-sm">
        <div className="flex items-start justify-between">
          <div>
            <div className="flex items-center gap-2 mb-2">
              <Users className="w-5 h-5 text-status-available" />
              <h3 className="text-gray-300 text-sm font-medium">Total Registrations</h3>
            </div>
            <div className="text-4xl font-bold text-white mb-1">{stats.totalRegistrations}</div>
            <p className="text-gray-400 text-sm">Across all events</p>
          </div>
          <div className="bg-status-available/20 p-3 rounded-xl">
            <Users className="w-6 h-6 text-status-available" />
          </div>
        </div>
      </div>
    </div>
  );
}
