// Book Types
export interface Book {
  id: number;
  title: string;
  author: string;
  publisher?: string;
  isbn?: string;
  publishedDate?: string;
  description?: string;
  availableCopies: number;
  totalCopies: number;
}

export interface BookSearchParams {
  keyword?: string;
  author?: string;
  publisher?: string;
  page?: number;
  size?: number;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// Event Types
export enum EventType {
  TECH_TALK = 'TECH_TALK',
  WORKSHOP = 'WORKSHOP',
  MEETUP = 'MEETUP',
  OTHER = 'OTHER',
}

export interface Event {
  id: number;
  title: string;
  description: string;
  eventType: EventType;
  startDateTime: string;
  endDateTime: string;
  location?: string;
  maxParticipants?: number;
  currentParticipants: number;
  presenter: Member;
  participants: Member[];
}

export interface Member {
  id: string;
  name: string;
  email: string;
}

// Book Loan Types
export enum LoanStatus {
  ACTIVE = 'ACTIVE',
  RETURNED = 'RETURNED',
  OVERDUE = 'OVERDUE',
}

export interface BookLoan {
  id: number;
  bookId: number;
  bookTitle: string;
  userId: string;
  loanDate: string;
  dueDate: string;
  returnDate?: string;
  status: LoanStatus;
  extensionCount: number;
}

export interface BookLoanSearchParams {
  status?: LoanStatus;
  page?: number;
  size?: number;
}
