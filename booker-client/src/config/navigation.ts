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
    label: '비즈니스',
    href: '/business',
    description: '비즈니스 도서 및 자료',
  },
  {
    label: '기술',
    href: '/tech',
    description: '기술 도서 및 자료',
  },
  {
    label: '내 프로필',
    href: '/profile',
    description: '내 정보 관리',
  },
];
