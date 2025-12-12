interface StatCardProps {
  value: number;
  label: string;
  valueColor?: string;
  className?: string;
}

export function StatCard({
  value,
  label,
  valueColor = 'text-white',
  className = '',
}: StatCardProps) {
  return (
    <div className={`bg-white/10 backdrop-blur-md rounded-2xl px-8 py-4 border border-white/20 ${className}`}>
      <div className={`text-4xl md:text-5xl font-bold mb-1 ${valueColor}`}>
        {value.toLocaleString()}
      </div>
      <div className="text-sm md:text-base text-gray-300">{label}</div>
    </div>
  );
}
