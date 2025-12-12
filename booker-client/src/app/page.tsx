import { HeroBanner } from '@/components/hero/HeroBanner';
import { Header } from '@/components/layout/Header';

export default function HomePage() {
  return (
    <main className="min-h-screen bg-black">
      <Header />
      <HeroBanner totalBooks={247} activeLoanCount={38} />
    </main>
  );
}
