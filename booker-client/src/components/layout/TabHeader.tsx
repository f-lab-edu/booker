interface Tab {
  id: string;
  label: string;
}

interface TabHeaderProps {
  title: string;
  tabs: Tab[];
  activeTab: string;
  onTabChange: (tabId: string) => void;
}

export function TabHeader({ title, tabs, activeTab, onTabChange }: TabHeaderProps) {
  return (
    <div className="fixed top-0 left-0 right-0 bg-white border-b border-gray-200 z-50">
      <div className="pl-8 pr-6">
        <div className="flex items-center gap-8 py-4">
          {/* Left: Logo/Title */}
          <h1 className="text-xl font-bold text-gray-900">{title}</h1>

          {/* Main Tab Menu */}
          <div className="flex gap-3">
            {tabs.map((tab) => (
              <button
                key={tab.id}
                onClick={() => onTabChange(tab.id)}
                className={`px-6 py-3 text-xl font-normal rounded-lg transition-all ${
                  activeTab === tab.id
                    ? 'text-green-600 hover:bg-green-100'
                    : 'text-gray-700 hover:bg-gray-100 hover:text-green-600'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
