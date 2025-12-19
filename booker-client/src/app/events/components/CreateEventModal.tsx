'use client';

import { useState } from 'react';
import { X, Upload, Calendar, Clock, MapPin, Users, FileText } from 'lucide-react';

interface CreateEventModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (eventData: any) => void;
}

export function CreateEventModal({ isOpen, onClose, onSubmit }: CreateEventModalProps) {
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    imageUrl: '',
    startDate: '',
    endDate: '',
    startTime: '',
    endTime: '',
    location: '',
    eventType: '',
    maxParticipants: '',
    requirements: {
      laptopRequired: false,
      materialsProvided: false,
    },
  });

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit(formData);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
      <div className="bg-neutral-800 rounded-2xl w-full max-w-2xl max-h-[90vh] overflow-y-auto border border-neutral-700">
        {/* Header */}
        <div className="sticky top-0 bg-neutral-800 border-b border-neutral-700 p-6 flex items-center justify-between">
          <h2 className="text-2xl font-bold text-white">Create New Event</h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-white transition-colors"
          >
            <X className="w-6 h-6" />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-6">
          {/* Event Image */}
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">
              Event Image
            </label>
            <div className="border-2 border-dashed border-neutral-600 rounded-xl p-8 text-center hover:border-status-available transition-colors cursor-pointer">
              <Upload className="w-12 h-12 text-gray-400 mx-auto mb-2" />
              <p className="text-gray-400 text-sm">Click to upload event banner</p>
              <p className="text-gray-500 text-xs mt-1">Recommended size: 1200x600px</p>
            </div>
          </div>

          {/* Event Title */}
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">
              Event Title
            </label>
            <input
              type="text"
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              placeholder="Enter event title"
              className="w-full bg-neutral-700 border border-neutral-600 rounded-lg px-4 py-3 text-white placeholder-gray-400 focus:outline-none focus:border-status-available transition-colors"
              required
            />
          </div>

          {/* Date and Time */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">
                <Calendar className="w-4 h-4 inline mr-1" />
                Start Date
              </label>
              <input
                type="date"
                value={formData.startDate}
                onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
                className="w-full bg-neutral-700 border border-neutral-600 rounded-lg px-4 py-3 text-white focus:outline-none focus:border-status-available transition-colors"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">
                <Clock className="w-4 h-4 inline mr-1" />
                Start Time
              </label>
              <input
                type="time"
                value={formData.startTime}
                onChange={(e) => setFormData({ ...formData, startTime: e.target.value })}
                className="w-full bg-neutral-700 border border-neutral-600 rounded-lg px-4 py-3 text-white focus:outline-none focus:border-status-available transition-colors"
                required
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">
                <Calendar className="w-4 h-4 inline mr-1" />
                End Date
              </label>
              <input
                type="date"
                value={formData.endDate}
                onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
                className="w-full bg-neutral-700 border border-neutral-600 rounded-lg px-4 py-3 text-white focus:outline-none focus:border-status-available transition-colors"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">
                <Clock className="w-4 h-4 inline mr-1" />
                End Time
              </label>
              <input
                type="time"
                value={formData.endTime}
                onChange={(e) => setFormData({ ...formData, endTime: e.target.value })}
                className="w-full bg-neutral-700 border border-neutral-600 rounded-lg px-4 py-3 text-white focus:outline-none focus:border-status-available transition-colors"
                required
              />
            </div>
          </div>

          {/* Location */}
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">
              <MapPin className="w-4 h-4 inline mr-1" />
              Location
            </label>
            <input
              type="text"
              value={formData.location}
              onChange={(e) => setFormData({ ...formData, location: e.target.value })}
              placeholder="Enter event location"
              className="w-full bg-neutral-700 border border-neutral-600 rounded-lg px-4 py-3 text-white placeholder-gray-400 focus:outline-none focus:border-status-available transition-colors"
              required
            />
          </div>

          {/* Event Type */}
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">
              Event Type
            </label>
            <select
              value={formData.eventType}
              onChange={(e) => setFormData({ ...formData, eventType: e.target.value })}
              className="w-full bg-neutral-700 border border-neutral-600 rounded-lg px-4 py-3 text-white focus:outline-none focus:border-status-available transition-colors"
              required
            >
              <option value="">Select event type</option>
              <option value="Workshop">Workshop</option>
              <option value="Tech Talk">Tech Talk</option>
              <option value="Team Building">Team Building</option>
              <option value="Conference">Conference</option>
              <option value="Training">Training</option>
            </select>
          </div>

          {/* Maximum Participants */}
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">
              <Users className="w-4 h-4 inline mr-1" />
              Maximum Participants
            </label>
            <input
              type="number"
              value={formData.maxParticipants}
              onChange={(e) => setFormData({ ...formData, maxParticipants: e.target.value })}
              placeholder="Enter maximum number of participants"
              className="w-full bg-neutral-700 border border-neutral-600 rounded-lg px-4 py-3 text-white placeholder-gray-400 focus:outline-none focus:border-status-available transition-colors"
              required
              min="1"
            />
          </div>

          {/* Description */}
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">
              <FileText className="w-4 h-4 inline mr-1" />
              Description
            </label>
            <textarea
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              placeholder="Enter event description"
              rows={4}
              className="w-full bg-neutral-700 border border-neutral-600 rounded-lg px-4 py-3 text-white placeholder-gray-400 focus:outline-none focus:border-status-available transition-colors resize-none"
              required
            />
          </div>

          {/* Requirements */}
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-3">
              Requirements
            </label>
            <div className="space-y-2">
              <label className="flex items-center">
                <input
                  type="checkbox"
                  checked={formData.requirements.laptopRequired}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      requirements: {
                        ...formData.requirements,
                        laptopRequired: e.target.checked,
                      },
                    })
                  }
                  className="w-4 h-4 rounded border-neutral-600 bg-neutral-700 text-status-available focus:ring-status-available focus:ring-offset-neutral-800"
                />
                <span className="ml-2 text-gray-300">Laptop Required</span>
              </label>

              <label className="flex items-center">
                <input
                  type="checkbox"
                  checked={formData.requirements.materialsProvided}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      requirements: {
                        ...formData.requirements,
                        materialsProvided: e.target.checked,
                      },
                    })
                  }
                  className="w-4 h-4 rounded border-neutral-600 bg-neutral-700 text-status-available focus:ring-status-available focus:ring-offset-neutral-800"
                />
                <span className="ml-2 text-gray-300">Materials Provided</span>
              </label>
            </div>
          </div>

          {/* Submit Button */}
          <button
            type="submit"
            className="w-full bg-primary hover:bg-primary-dark text-white font-semibold py-4 rounded-xl transition-all duration-200 shadow-lg hover:shadow-primary/50"
          >
            Create Event
          </button>
        </form>
      </div>
    </div>
  );
}
