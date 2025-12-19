# Events Page Implementation Plan

## Overview
Implement a comprehensive event management system on the `/events` route with the following screens:
1. Event List View (이벤트 취소 수정)
2. Event Detail/Edit View (이벤트 참여 상세)
3. Create New Event View (이벤트 등록)
4. Event Statistics Dashboard (이벤트 취소 수정)

## Design Requirements
- Maintain **dark theme** with **green accents** (matching the existing color scheme)
- Use card-based layout for event listings
- Implement modals/dialogs for create and edit operations
- Show event statistics (active events, total registrations)
- Display event images, dates, times, locations, and participant counts

## Component Structure

### 1. Events Page (Main Container)
- `/app/events/page.tsx`
- Container for all event-related views
- Manage state for active view (list, detail, create)
- Handle modal open/close states

### 2. Event List Component
**Features:**
- Display upcoming events in card format
- Show event banner images
- Display event title, date, time, location
- Show participant count and availability
- "Register" button with green accent
- Filter/search functionality
- "Add Event" button (admin only)

**Data displayed per event:**
- Event image/banner
- Event title
- Description (preview)
- Date and time
- Location
- Capacity (e.g., "15/30 registered")
- Active status badge
- Register/Edit/Cancel actions

### 3. Event Detail/Edit Modal
**Features:**
- View/edit event details
- Event banner image
- Title and description
- Date and time (start/end)
- Location information
- Seat availability chart (50% full visual)
- Requirements checklist (laptop required, materials provided)
- Confirm registration button
- Responsive to screen sizes

### 4. Create New Event Modal
**Features:**
- Image upload for event banner (1200x600px recommended)
- Event title input
- Start/end date and time pickers
- Location input with icon
- Event type dropdown
- Maximum participants input
- Description textarea
- Requirements checklist (laptop required, materials provided)
- Create button with purple/green accent

### 5. Event Statistics Dashboard
**Features:**
- Show key metrics:
  - Total active events (12 active this week)
  - Total registrations (248 across all events)
- Display in card format with icons
- Use purple accent for cards

## Technical Implementation

### State Management
- Use React hooks (useState, useEffect)
- Consider context for global event state if needed

### API Integration
- Create API service for events CRUD operations
- Endpoints needed:
  - GET `/api/events` - List all events
  - GET `/api/events/:id` - Get event details
  - POST `/api/events` - Create new event
  - PUT `/api/events/:id` - Update event
  - DELETE `/api/events/:id` - Delete event
  - POST `/api/events/:id/register` - Register for event
  - DELETE `/api/events/:id/register` - Cancel registration

### Styling
- Use Tailwind CSS with custom dark theme
- Color palette:
  - Background: `bg-neutral-900` (dark)
  - Cards: `bg-neutral-800` with subtle borders
  - Primary accent: Green (`green-500`, `green-600`)
  - Secondary accent: Purple (`purple-600`)
  - Text: Light gray on dark background
  - Active badges: Green with opacity

### Components to Create
1. `EventCard.tsx` - Individual event card
2. `EventListView.tsx` - List of all events
3. `EventDetailModal.tsx` - View/edit event details
4. `CreateEventModal.tsx` - Create new event form
5. `EventStats.tsx` - Statistics dashboard
6. `EventRegistrationButton.tsx` - Register/cancel button with state

### Form Handling
- Use controlled components for form inputs
- Implement form validation
- Handle image upload (file input or drag-drop)
- Date/time picker components

### Responsive Design
- Mobile-first approach
- Stack cards vertically on mobile
- Grid layout on desktop (2-3 columns)
- Full-screen modals on mobile, centered on desktop

## MVP Scope
For initial implementation:
1. ✅ Event list view with dummy data
2. ✅ Event card component with proper styling
3. ✅ Basic statistics display
4. ✅ Create event modal (UI only, no backend)
5. ✅ Detail/edit modal (UI only)
6. ✅ Dark theme with green accents applied throughout
7. ⏳ Backend integration (Phase 2)

## Implementation Complete (2025-12-18)

### Files Created:
- `/app/events/page.tsx` - Main events page with state management
- `/app/events/types.ts` - TypeScript interfaces for Event and EventStats
- `/app/events/mockData.ts` - Mock data for testing (4 sample events)
- `/app/events/components/EventCard.tsx` - Individual event card component
- `/app/events/components/EventStats.tsx` - Statistics dashboard component
- `/app/events/components/CreateEventModal.tsx` - Create new event modal
- `/app/events/components/EventDetailModal.tsx` - View/edit event detail modal

### Features Implemented:

#### Event Card Component
- Event banner image with hover effects
- Active status badge (green)
- Event title, description preview
- Date, time, location, and participant count with icons
- Visual indicators for seat availability (almost full warning, full status)
- Register button with green accent
- Edit and Cancel buttons for admin actions

#### Event Statistics Dashboard
- Purple gradient card for Active Events count
- Green gradient card for Total Registrations
- Icons from lucide-react
- Responsive grid layout

#### Create Event Modal
- Full form with all required fields
- Image upload placeholder
- Date and time inputs
- Event type dropdown (Workshop, Tech Talk, Team Building, etc.)
- Maximum participants input
- Description textarea
- Requirements checkboxes (Laptop Required, Materials Provided)
- Primary purple button for submission

#### Event Detail Modal
- Full-width event banner
- Comprehensive event information display
- Seat availability progress bar with percentage
- Color-coded progress (green < 70%, yellow 70-90%, red > 90%)
- Requirements list with checkmarks
- Register button or "Event Full" disabled state

#### Main Events Page
- Dark theme (`bg-neutral-900`) background
- Header with title and "Add Event" button
- Statistics dashboard at the top
- Responsive grid layout (1 col mobile, 2 col tablet, 3 col desktop)
- Click event card to open detail modal
- Modal state management

### Styling Details:
- Background: `bg-neutral-900`
- Cards: `bg-neutral-800` with `border-neutral-700`
- Green accent: `status.available` (#10B981) for active badges, register buttons
- Purple accent: `primary` (#8b5cf6) for CTA buttons
- Text: White headings, gray-300/400 for body text
- Hover effects on cards and buttons
- Smooth transitions throughout

### Mock Data:
- 4 sample events with realistic data
- Event types: Workshop, Team Building, Tech Talk
- Unsplash placeholder images
- Various capacity levels to test UI states
- Stats: 12 active events, 248 total registrations

### Next Steps (Phase 2):
- Backend API integration
- Real image upload functionality
- User authentication for register/edit actions
- Event search and filtering
- Calendar integration
- Email notifications
- Admin role checks

## Phase 2 (Future)
- Connect to actual backend API
- Image upload functionality
- Real-time event updates
- Email notifications for registrations
- Calendar integration
- Event search and filters
- Admin role checks

## Dependencies
- React 18+
- Next.js 14+
- Tailwind CSS
- Date picker library (e.g., react-datepicker)
- Icon library (already using lucide-react or similar)

## Reasoning
- **Dark theme with green accents**: Matches existing design system and user preference
- **Modal-based flows**: Better UX for create/edit operations without full page navigation
- **Card-based layout**: Modern, scannable design for event listings
- **Statistics dashboard**: Quick overview of event engagement
- **MVP approach**: Focus on UI first, then backend integration
