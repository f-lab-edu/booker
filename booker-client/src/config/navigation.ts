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
    label: '개발자센터',
    href: '/developer',
    description: 'API 문서 및 개발 가이드',
  },
];

