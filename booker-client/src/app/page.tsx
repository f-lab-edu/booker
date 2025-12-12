import { HeroBanner } from '@/components/hero/HeroBanner';
import { Header } from '@/components/layout/Header';
import { Footer } from '@/components/layout/Footer';
import { BookList } from '@/components/sections/BookList';
import { EventList } from '@/components/sections/EventList';
import { ValueProposition } from '@/components/sections/ValueProposition';

export default function HomePage() {
  return (
    <main className="min-h-screen bg-black">
      <Header />
      <HeroBanner totalBooks={247} activeLoanCount={38} />
      <BookList />
      <EventList />
      <ValueProposition />
      <Footer />
    </main>
  );
}
