# Book Detail, Loan Management & Admin Dashboard Implementation Plan

## Overview
Create a comprehensive book library management system with:
1. **Book Detail Page** - Individual book view with loan/waitlist functionality
2. **My Loans Page** - User's current loans, waitlist, and history
3. **Books Listing Page** - Search and browse all books
4. **Admin Dashboard** - Book order management and inventory control

## API Endpoints to Use

### Books API (http://localhost:8084/api/v1/books)
- `GET /api/v1/books` - Search and list books (with pagination)
- `GET /api/v1/books/{id}` - Get book details
- `POST /api/v1/books` - Create book (admin)
- `PUT /api/v1/books/{id}` - Update book (admin)
- `DELETE /api/v1/books/{id}` - Delete book (admin)

### Book Loans API (http://localhost:8084/api/v1/loans)
- `GET /api/v1/loans` - Get my loan list (with pagination)
- `GET /api/v1/loans/{loanId}` - Get loan details
- `POST /api/v1/loans` - Create loan (borrow book)
- `POST /api/v1/loans/{loanId}/return` - Return book
- `POST /api/v1/loans/{loanId}/extend` - Extend loan period

### Book Orders API (http://localhost:8084/api/v1/book-orders)
- `GET /api/v1/book-orders` - All orders (admin)
- `GET /api/v1/book-orders/{id}` - Order details
- `GET /api/v1/book-orders/my` - My order requests
- `POST /api/v1/book-orders` - Create order request
- `POST /api/v1/book-orders/{id}/approve` - Approve order (admin)
- `POST /api/v1/book-orders/{id}/reject` - Reject order (admin)
- `POST /api/v1/book-orders/{id}/receive` - Mark as received (admin)

## Design Requirements

### Color Scheme (Maintain existing dark theme)
- **Background**: `#0a1a13` (dark green-black) or `bg-[#0a1a13]`
- **Cards**: `bg-white/5` with `border-white/10`
- **Primary Accent**: Green (`text-green-400`, `bg-green-500/10`)
- **Secondary Accent**: Yellow (`text-yellow-400` for warnings)
- **Success**: Green borders and backgrounds
- **Warning**: Yellow/Orange for due dates
- **Error**: Red for overdue items
- **Text**: White with opacity variants (`text-white/60`, `text-white/40`)

### Typography
- **Headers**: Bold, white text
- **Body**: `text-white/60` or `text-white/70`
- **Labels**: `text-white/40` or `text-sm`

## Implementation Tasks

### Phase 1: Setup & Infrastructure

#### 1.1 TypeScript Interfaces
Create `/booker-client/src/types/api.ts`:
```typescript
// Book types
interface Book {
  id: number;
  title: string;
  author: string;
  isbn?: string;
  publisher?: string;
  category?: string;
  description?: string;
  coverImageUrl?: string;
  totalCopies: number;
  availableCopies: number;
  publishedDate?: string;
  createdAt: string;
  updatedAt: string;
}

// Loan types
interface BookLoan {
  id: number;
  bookId: number;
  userId: number;
  book?: Book;
  user?: User;
  loanDate: string;
  dueDate: string;
  returnDate?: string;
  status: 'ACTIVE' | 'RETURNED' | 'OVERDUE';
  extensionCount: number;
  overdueFeeDays?: number;
  overdueFeeAmount?: number;
}

// Book Order types
interface BookOrder {
  id: number;
  userId: number;
  bookTitle: string;
  author: string;
  isbn?: string;
  publisher?: string;
  reason?: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'RECEIVED';
  requestedAt: string;
  processedAt?: string;
  receivedAt?: string;
}

// User types
interface User {
  id: number;
  email: string;
  name: string;
  avatarUrl?: string;
  role: 'USER' | 'ADMIN';
}

// Pagination
interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
```

#### 1.2 API Client Setup
Create `/booker-client/src/lib/api/client.ts`:
```typescript
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8084';

class ApiClient {
  private baseUrl: string;

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl;
  }

  async fetch<T>(endpoint: string, options?: RequestInit): Promise<T> {
    const response = await fetch(`${this.baseUrl}${endpoint}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...options?.headers,
      },
    });

    if (!response.ok) {
      throw new Error(`API Error: ${response.statusText}`);
    }

    return response.json();
  }

  // Books
  async getBooks(params?: { page?: number; size?: number; search?: string }) {
    const query = new URLSearchParams(params as any).toString();
    return this.fetch<PageResponse<Book>>(`/api/v1/books?${query}`);
  }

  async getBook(id: number) {
    return this.fetch<Book>(`/api/v1/books/${id}`);
  }

  // Loans
  async getMyLoans(params?: { page?: number; size?: number }) {
    const query = new URLSearchParams(params as any).toString();
    return this.fetch<PageResponse<BookLoan>>(`/api/v1/loans?${query}`);
  }

  async borrowBook(bookId: number) {
    return this.fetch<BookLoan>(`/api/v1/loans`, {
      method: 'POST',
      body: JSON.stringify({ bookId }),
    });
  }

  async returnBook(loanId: number) {
    return this.fetch<void>(`/api/v1/loans/${loanId}/return`, {
      method: 'POST',
    });
  }

  async extendLoan(loanId: number) {
    return this.fetch<BookLoan>(`/api/v1/loans/${loanId}/extend`, {
      method: 'POST',
    });
  }

  // Book Orders
  async getMyBookOrders() {
    return this.fetch<BookOrder[]>(`/api/v1/book-orders/my`);
  }

  async createBookOrder(order: Partial<BookOrder>) {
    return this.fetch<BookOrder>(`/api/v1/book-orders`, {
      method: 'POST',
      body: JSON.stringify(order),
    });
  }
}

export const apiClient = new ApiClient(API_BASE_URL);
```

### Phase 2: Book Detail Page

#### 2.1 Book Detail Page Component
**File**: `/booker-client/src/app/books/[id]/page.tsx`

**Features**:
- Dynamic route parameter `[id]`
- Fetch book details from `GET /api/v1/books/{id}`
- Display book cover, title, author, description
- Show availability status (available copies)
- Display current borrower info (if borrowed)
- Show loan dates and due date
- Action buttons:
  - "Borrow Book" (if available)
  - "Join Waiting List" (if all copies borrowed)
  - "Return Book" (if user has borrowed it)
- Waiting list section with avatars

**Layout**:
```
┌─────────────────────────────────┐
│         Book Cover Image        │
│         (Centered, Large)       │
├─────────────────────────────────┤
│         Atomic Habits           │
│         by James Clear          │
│                                 │
│   [Available] Due: June 15, 2025│
├─────────────────────────────────┤
│         Description             │
│   No matter your goals...       │
├─────────────────────────────────┤
│       Current Status            │
│   Current Borrower: Emily J     │
│   Loan Date: May 15, 2025       │
│   Due Date: June 15, 2025       │
├─────────────────────────────────┤
│       Waiting List (3)          │
│   👤 Michael Chen - May 20      │
│   👤 Sarah Williams - May 25    │
│   👤 David Rodriguez - Jun 1    │
├─────────────────────────────────┤
│   [     Borrow Book    ]        │
│                                 │
│   Book ID: LIB-2025-0042        │
│   Category: Self-Help           │
│   Location: 12th Floor          │
└─────────────────────────────────┘
```

#### 2.2 Components to Create

##### BookCoverImage Component
**File**: `/booker-client/src/components/books/BookCoverImage.tsx`
- Display book cover with fallback
- Responsive image sizing
- Loading skeleton

##### BookMetadata Component
**File**: `/booker-client/src/components/books/BookMetadata.tsx`
- Display ISBN, category, publisher
- Book ID display
- Location information

##### BorrowStatusBadge Component
**File**: `/booker-client/src/components/books/BorrowStatusBadge.tsx`
- Show "Available", "Borrowed", "Overdue"
- Color-coded badges (green/yellow/red)

##### BorrowButton Component
**File**: `/booker-client/src/components/books/BorrowButton.tsx`
- Multiple states: "Borrow", "Return", "Join Waitlist", "Extend"
- Handle API calls on click
- Loading states
- Success/error toasts

##### WaitingListSection Component
**File**: `/booker-client/src/components/books/WaitingListSection.tsx`
- List of users waiting
- Avatar + name + request date
- "Join Waiting List" button if not in list
- "Cancel Request" if user is in list

### Phase 3: My Loans Page

#### 3.1 My Loans Page Component
**File**: `/booker-client/src/app/my-loans/page.tsx`

**Features**:
- Tab navigation: "All Books", "Current", "Waitlist", "History"
- Display loan statistics:
  - Current loans count
  - Waitlist count
  - History count
- **Current Loans Section**:
  - Book card with cover thumbnail
  - Due date with days remaining
  - "Renew" button
  - "Return" button
- **Waitlist Section**:
  - Books user is waiting for
  - Position in queue
  - "Cancel" button
- **History Section**:
  - Previously borrowed books
  - Return date
  - "Borrow Again" button

**Layout Reference**: Screenshot 1 (My book loans)

```
┌─────────────────────────────────┐
│       My book loans             │
│                                 │
│  Current   Waitlist   History   │
│    2          3         12      │
├─────────────────────────────────┤
│ [All Books] [Current] [Wait...] │
├─────────────────────────────────┤
│       Current Loans             │
│                                 │
│  ┌──────────────────────────┐  │
│  │ 📕 Atomic Habits         │  │
│  │    James Clear           │  │
│  │    Due in 14 days   [Renew]│
│  └──────────────────────────┘  │
│                                 │
│  ┌──────────────────────────┐  │
│  │ 📗 Zero to One           │  │
│  │    Peter Thiel           │  │
│  │    Due in 2 days   [Return]│
│  └──────────────────────────┘  │
├─────────────────────────────────┤
│       On Waitlist               │
│                                 │
│  ┌──────────────────────────┐  │
│  │ 📘 Deep Work             │  │
│  │    Cal Newport           │  │
│  │    2nd in line   [Cancel]  │
│  └──────────────────────────┘  │
└─────────────────────────────────┘
```

#### 3.2 Components to Create

##### LoanCard Component
**File**: `/booker-client/src/components/loans/LoanCard.tsx`
- Book thumbnail + title + author
- Due date badge (color-coded by urgency)
- Action buttons (Renew/Return)
- Overdue warning if applicable

##### WaitlistCard Component
**File**: `/booker-client/src/components/loans/WaitlistCard.tsx`
- Book info
- Queue position
- Cancel button

##### LoanHistoryCard Component
**File**: `/booker-client/src/components/loans/LoanHistoryCard.tsx`
- Book info
- Return date
- "Borrow Again" button

### Phase 4: Books Listing Page

#### 4.1 Update Books Page
**File**: `/booker-client/src/app/books/page.tsx`

**Features**:
- Search bar with real-time search
- Filter by category
- Pagination
- Grid view of book cards
- Click card to navigate to `/books/[id]`

**API Integration**:
- `GET /api/v1/books?page=0&size=20&search=clean`

**Layout**:
```
┌─────────────────────────────────┐
│         도서 검색                │
│                                 │
│  🔍 [Search books, authors...]  │
│                                 │
│  [All] [Business] [Tech] [...]  │
├─────────────────────────────────┤
│  ┌─────────┐ ┌─────────┐       │
│  │ Clean   │ │ Atomic  │       │
│  │ Code    │ │ Habits  │       │
│  │         │ │         │       │
│  │ 3 avail │ │ 5 avail │       │
│  └─────────┘ └─────────┘       │
│                                 │
│  ┌─────────┐ ┌─────────┐       │
│  │ Deep    │ │ HTTP    │       │
│  │ Work    │ │ Guide   │       │
│  │         │ │         │       │
│  │ 1 avail │ │ 4 avail │       │
│  └─────────┘ └─────────┘       │
├─────────────────────────────────┤
│         [← 1 2 3 ... 10 →]      │
└─────────────────────────────────┘
```

#### 4.2 Update BookList Component
**File**: `/booker-client/src/components/sections/BookList.tsx`
- Add navigation to detail page on click
- Update to use real API data

### Phase 5: Admin Dashboard

#### 5.1 Admin Dashboard Page
**File**: `/booker-client/src/app/admin/page.tsx`

**Features** (from Screenshot 2):
- Total books count with trend
- New requests count (pending orders)
- Search bar: "Search books, users, or orders..."
- **Recent Orders Section**:
  - Order cards with status badges
  - "Delivered", "Processing", "Ordered" status
- **Book Management Section**:
  - List of books with available copies
  - "Edit" and "Delete" buttons
  - "Add New Book" button

**API Integration**:
- `GET /api/v1/book-orders` - Get all orders
- `GET /api/v1/books` - Get all books

**Layout**:
```
┌─────────────────────────────────┐
│       Admin Dashboard           │
│                                 │
│  Total Books        New Requests│
│     2,547               18      │
│  +17% this month   Pending...   │
├─────────────────────────────────┤
│  🔍 [Search books, users...]    │
├─────────────────────────────────┤
│       Recent Orders             │
│                                 │
│  ┌──────────────────────────┐  │
│  │ 📕 Clean Code           │  │
│  │    Robert C. Martin      │  │
│  │    Order #LIB-2025-0147  │  │
│  │    Jun 7, 2025           │  │
│  │    [Delivered]           │  │
│  └──────────────────────────┘  │
├─────────────────────────────────┤
│       Book Management           │
│                                 │
│  ┌──────────────────────────┐  │
│  │ 📘 The Pragmatic...      │  │
│  │    20 copies available   │  │
│  │    [Edit]  [Delete]      │  │
│  └──────────────────────────┘  │
│                                 │
│  [+ Add New Book]               │
└─────────────────────────────────┘
```

#### 5.2 Admin Order Detail/Edit Page
**File**: `/booker-client/src/app/admin/orders/[id]/page.tsx`

**Features** (from Screenshot 2 middle):
- Confirm Book Order form
- Book Details section:
  - Title, Author, ISBN, Publisher, Category
- Classification section:
  - Category/Genre dropdown
  - Tags/Keywords input
  - Reading Level dropdown
- Inventory Details:
  - Number of Copies input
  - Location/Shelf Information
  - Condition dropdown
- Book Cover upload
- Description textarea
- "Confirm Order" button
- "Save" button

**API Integration**:
- `POST /api/v1/book-orders/{id}/approve` - Approve order
- `POST /api/v1/books` - Create book after approval

#### 5.3 Order Status Update Page
**File**: `/booker-client/src/app/admin/orders/[id]/status/page.tsx`

**Features** (from Screenshot 2 right):
- Edit Order Status
- Status options with icons:
  - 🟡 Processing
  - 📦 Shipped
  - ✅ Delivered
  - ❌ Cancelled
- "Update Status" button

**API Integration**:
- `POST /api/v1/book-orders/{id}/receive` - Mark as delivered

#### 5.4 Components to Create

##### OrderCard Component
**File**: `/booker-client/src/components/admin/OrderCard.tsx`
- Display order summary
- Status badge
- Book title + author
- Order date

##### BookManagementCard Component
**File**: `/booker-client/src/components/admin/BookManagementCard.tsx`
- Book thumbnail
- Title + copies available
- Edit/Delete buttons

##### StatCard Component (already exists)
**File**: `/booker-client/src/components/ui/StatCard.tsx`
- Display metrics with trends
- Reuse for Total Books, New Requests

## File Structure
```
booker-client/src/
├── app/
│   ├── books/
│   │   ├── page.tsx (Updated: listing with search)
│   │   └── [id]/
│   │       └── page.tsx (New: book detail)
│   ├── my-loans/
│   │   └── page.tsx (Updated: full functionality)
│   └── admin/
│       ├── page.tsx (New: dashboard)
│       └── orders/
│           ├── [id]/
│           │   ├── page.tsx (New: order detail)
│           │   └── status/
│           │       └── page.tsx (New: status update)
│           └── new/
│               └── page.tsx (New: create order)
├── components/
│   ├── books/
│   │   ├── BookCoverImage.tsx
│   │   ├── BookMetadata.tsx
│   │   ├── BorrowStatusBadge.tsx
│   │   ├── BorrowButton.tsx
│   │   └── WaitingListSection.tsx
│   ├── loans/
│   │   ├── LoanCard.tsx
│   │   ├── WaitlistCard.tsx
│   │   └── LoanHistoryCard.tsx
│   └── admin/
│       ├── OrderCard.tsx
│       ├── BookManagementCard.tsx
│       └── OrderForm.tsx
├── lib/
│   └── api/
│       └── client.ts (New: API client)
└── types/
    └── api.ts (New: TypeScript interfaces)
```

## Testing Plan

### Manual Testing Checklist
1. **Book Detail Page**
   - [ ] Navigate from books list to detail page
   - [ ] Verify book information displays correctly
   - [ ] Test "Borrow Book" button (creates loan)
   - [ ] Test "Join Waiting List" button
   - [ ] Verify waiting list displays correctly

2. **My Loans Page**
   - [ ] Verify current loans display
   - [ ] Test "Renew" button (extends loan)
   - [ ] Test "Return" button (returns book)
   - [ ] Verify waitlist section
   - [ ] Verify history section

3. **Books Listing**
   - [ ] Test search functionality
   - [ ] Test category filtering
   - [ ] Test pagination
   - [ ] Click book card navigates to detail

4. **Admin Dashboard**
   - [ ] Verify stats display correctly
   - [ ] Test order management
   - [ ] Test book CRUD operations
   - [ ] Test order approval flow

## Implementation Order

1. ✅ Create plan document (current)
2. ⏳ Set up API client and TypeScript interfaces
3. Create Book Detail page
4. Update My Loans page
5. Update Books listing page
6. Create Admin Dashboard
7. Create Admin Order Management
8. Test all functionality

## API Configuration

Add to `.env`:
```
NEXT_PUBLIC_API_BASE_URL=http://localhost:8084
```

## Notes & Considerations

1. **Authentication**: Assumes Google OAuth is already set up
2. **Error Handling**: Add toast notifications for API errors
3. **Loading States**: Show skeletons while fetching data
4. **Responsive Design**: Mobile-first approach
5. **Accessibility**: ARIA labels, keyboard navigation
6. **Optimistic Updates**: Update UI before API response for better UX
7. **Caching**: Consider using React Query or SWR for data fetching

## Dependencies to Check
- `framer-motion` - Already installed (used in BookList)
- `lucide-react` - Already installed (icons)
- Consider adding:
  - `react-hot-toast` - For notifications
  - `@tanstack/react-query` - For data fetching (optional)
  - `date-fns` - For date formatting

## Reasoning

### Why this structure?
1. **Separation of Concerns**: Each page handles its own data fetching
2. **Reusable Components**: Shared components for cards, badges, buttons
3. **Type Safety**: TypeScript interfaces for all API responses
4. **Centralized API Client**: Single source of truth for API calls
5. **Progressive Enhancement**: Build basic features first, add advanced later

### Why dark theme?
- Matches existing design (screenshot 3)
- Reduces eye strain
- Modern, professional look
- Consistent with brand identity

### Why client-side rendering?
- Real-time updates for loan status
- Interactive features (borrow, return, renew)
- User-specific data (my loans)
- Admin dashboard requires authentication

## Next Steps

After plan approval:
1. Review API documentation in Swagger UI
2. Test API endpoints with sample requests
3. Begin implementation with Phase 1 (API client setup)
4. Iterate through each phase sequentially
5. Test thoroughly after each component

---

**Status**: Awaiting approval to proceed with implementation.
