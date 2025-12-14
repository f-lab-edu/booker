import { apiClient } from './client';
import { Book, BookSearchParams, Page } from './types';

export const bookApi = {
  /**
   * Search books with optional filters
   */
  searchBooks: async (params?: BookSearchParams): Promise<Page<Book>> => {
    const searchParams = new URLSearchParams();
    if (params?.keyword) searchParams.append('keyword', params.keyword);
    if (params?.author) searchParams.append('author', params.author);
    if (params?.publisher) searchParams.append('publisher', params.publisher);
    if (params?.page !== undefined) searchParams.append('page', params.page.toString());
    if (params?.size !== undefined) searchParams.append('size', params.size.toString());

    const query = searchParams.toString();
    return apiClient<Page<Book>>(`/books${query ? `?${query}` : ''}`);
  },

  /**
   * Get a book by ID
   */
  getBook: async (id: number): Promise<Book> => {
    return apiClient<Book>(`/books/${id}`);
  },

  /**
   * Create a new book (admin only)
   */
  createBook: async (book: Omit<Book, 'id'>): Promise<Book> => {
    return apiClient<Book>('/books', {
      method: 'POST',
      body: JSON.stringify(book),
    });
  },

  /**
   * Update a book (admin only)
   */
  updateBook: async (id: number, book: Partial<Book>): Promise<Book> => {
    return apiClient<Book>(`/books/${id}`, {
      method: 'PUT',
      body: JSON.stringify(book),
    });
  },

  /**
   * Delete a book (admin only)
   */
  deleteBook: async (id: number): Promise<void> => {
    return apiClient<void>(`/books/${id}`, {
      method: 'DELETE',
    });
  },
};
