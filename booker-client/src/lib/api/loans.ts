import { apiClient } from './client';
import { BookLoan, BookLoanSearchParams, Page } from './types';

export const loanApi = {
  /**
   * Get my loans with optional filters
   */
  getMyLoans: async (params?: BookLoanSearchParams): Promise<Page<BookLoan>> => {
    const searchParams = new URLSearchParams();
    if (params?.status) searchParams.append('status', params.status);
    if (params?.page !== undefined) searchParams.append('page', params.page.toString());
    if (params?.size !== undefined) searchParams.append('size', params.size.toString());

    const query = searchParams.toString();
    return apiClient<Page<BookLoan>>(`/api/v1/loans${query ? `?${query}` : ''}`);
  },

  /**
   * Get loan details by ID
   */
  getLoan: async (loanId: number): Promise<BookLoan> => {
    return apiClient<BookLoan>(`/api/v1/loans/${loanId}`);
  },

  /**
   * Create a new loan (borrow a book)
   */
  createLoan: async (bookId: number): Promise<BookLoan> => {
    return apiClient<BookLoan>('/api/v1/loans', {
      method: 'POST',
      body: JSON.stringify({ bookId }),
    });
  },

  /**
   * Return a book
   */
  returnBook: async (loanId: number): Promise<BookLoan> => {
    return apiClient<BookLoan>(`/api/v1/loans/${loanId}/return`, {
      method: 'POST',
    });
  },

  /**
   * Extend loan period
   */
  extendLoan: async (loanId: number): Promise<BookLoan> => {
    return apiClient<BookLoan>(`/api/v1/loans/${loanId}/extend`, {
      method: 'POST',
    });
  },
};
