export interface NavigationItem {
  label: string;
  href: string;
  description?: string;
}

export const mainNavigation: NavigationItem[] = [
  {
    label: '도서',
    href: '/books',
    description: '도서 검색 및 대출',
  },
  {
    label: '이벤트',
    href: '/events',
    description: '사내 이벤트 및 테크톡',
  },
  {
    label: '내 대출',
    href: '/my-loans',
    description: '내 대출 현황',
  },
  {
    label: '프로필',
    href: '/profile',
    description: '내 정보 관리',
  },
];
