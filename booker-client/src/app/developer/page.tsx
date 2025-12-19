'use client';

import { useState } from 'react';
import { CodeEditor } from '@/components/sandbox/CodeEditor';
import { CodePreview } from '@/components/sandbox/CodePreview';
import { TabHeader } from '@/components/layout/TabHeader';
import { GuideSidebar } from '@/components/guide/GuideSidebar';
import { ArticleCard } from '@/components/guide/ArticleCard';

type MainTab = 'guide' | 'sandbox';
type SandboxTab = 'html' | 'css' | 'javascript';

const defaultHTML = `<div class="card">
  <h1>Welcome to BOOKER Sandbox</h1>
  <p>Edit the code to see changes in real-time!</p>
  <button id="btn">Click Me</button>
</div>`;

const defaultCSS = `.card {
  max-width: 400px;
  padding: 30px;
  background: linear-gradient(135deg, #10b981, #059669);
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.3);
  color: white;
}

h1 {
  font-size: 24px;
  margin-bottom: 10px;
}

p {
  margin-bottom: 20px;
  opacity: 0.9;
}

button {
  padding: 10px 20px;
  background: white;
  color: #059669;
  border: none;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s;
}

button:hover {
  transform: scale(1.05);
}`;

const defaultJS = `document.getElementById('btn').addEventListener('click', () => {
  alert('Hello from BOOKER Sandbox! 🚀');
});`;

const DEVELOPER_TABS = [
  { id: 'guide', label: '팀 블로그' },
  { id: 'sandbox', label: '샌드박스' },
];

const GUIDE_SIDEBAR_ITEMS = [
  { id: 'getting-started', label: '시작하기' },
  { id: 'setup', label: '환경 설정하기' },
  { id: 'llm-integration', label: 'LLMs로 결제 연동하기' },
  { id: 'migration', label: '마이그레이션하기' },
  {
    id: 'payment-understanding',
    label: '결제 이해하기',
    children: [
      { id: 'pg-online', label: 'PG와 온라인 결제' },
      { id: 'toss-products', label: '토스페이먼츠 결제제품' },
      { id: 'payment-policy', label: '결제수단 정책 안내' },
      { id: 'payment-flow', label: '결제 흐름' },
    ],
  },
  { id: 'payment-service', label: '결제 서비스' },
  {
    id: 'payment-widget',
    label: '결제위젯',
    children: [
      { id: 'widget-understand', label: '이해하기' },
      { id: 'admin-setup', label: '어드민 설정하기' },
      { id: 'payment-integration', label: '결제 연동하기', badge: '표준' },
      { id: 'pro-features', label: 'Pro 기능 사용하기' },
      { id: 'brandpay-integration', label: '브랜드페이 연동하기' },
      { id: 'paypal-integration', label: 'PayPal 연동하기' },
    ],
  },
  { id: 'brandpay', label: '브랜드페이' },
  { id: 'subscription', label: '자동결제(빌링)' },
  { id: 'payment-window', label: '결제창' },
];

export default function DeveloperPage() {
  const [mainTab, setMainTab] = useState<MainTab>('guide');
  const [html, setHtml] = useState(defaultHTML);
  const [css, setCss] = useState(defaultCSS);
  const [javascript, setJavascript] = useState(defaultJS);
  const [sandboxTab, setSandboxTab] = useState<SandboxTab>('html');
  const [guideCategory, setGuideCategory] = useState('getting-started');

  return (
    <main className="min-h-screen bg-white">
      {/* Fixed Header with Main Tabs */}
      <TabHeader
        title="개발자센터"
        tabs={DEVELOPER_TABS}
        activeTab={mainTab}
        onTabChange={(tabId) => setMainTab(tabId as MainTab)}
      />

      {/* Content with top padding to account for fixed header */}
      <div className={mainTab === 'guide' ? '' : 'pl-8 pr-6'} style={{ paddingTop: '5.5rem', paddingBottom: '2rem' }}>

        {/* Tab Content */}
        <div className="mt-8">
          {mainTab === 'guide' && (
            <div className="flex">
              {/* Sidebar */}
              <GuideSidebar
                items={GUIDE_SIDEBAR_ITEMS}
                activeItem={guideCategory}
                onItemClick={setGuideCategory}
              />

              {/* Main Content */}
              <div className="ml-64 flex-1 px-16">
                <ArticleCard
                  title="결제 시스템 시작하기"
                  subtitle="BOOKER 결제 시스템을 활용한 안전하고 빠른 결제 연동 가이드"
                  author={{
                    name: 'Booker Dev Team',
                    role: 'Payment Integration Engineer',
                  }}
                  date="2025년 12월 15일"
                  category="시작하기"
                  bannerGradient="from-green-300 via-emerald-200 to-teal-200"
                  bannerIcon="💳"
                  content={`안녕하세요, BOOKER 개발팀입니다.

BOOKER는 안전하고 편리한 결제 시스템을 제공합니다. 이 가이드에서는 결제 시스템을 시작하는 방법과 기본적인 연동 절차를 안내합니다.

결제 시스템 도입을 통해 사용자에게 더 나은 결제 경험을 제공하고, 안전한 거래를 보장할 수 있습니다.`}
                />
              </div>
            </div>
          )}

          {mainTab === 'sandbox' && (
            <div>
              <div className="mb-6">
                <h2 className="text-2xl font-bold text-gray-900 mb-2">샌드박스</h2>
                <p className="text-gray-600">실시간 코드 샌드박스 - HTML, CSS, JavaScript를 테스트해보세요</p>
              </div>

              {/* Sandbox Container */}
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 h-[calc(100vh-360px)]">
                {/* Left: Code Editor */}
                <div className="flex flex-col gap-4">
                  {/* Sandbox Tabs */}
                  <div className="flex gap-2 bg-gray-100 p-1 rounded-lg">
                    <button
                      onClick={() => setSandboxTab('html')}
                      className={`flex-1 px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                        sandboxTab === 'html'
                          ? 'bg-white text-orange-600 shadow-sm'
                          : 'text-gray-600 hover:text-gray-900'
                      }`}
                    >
                      HTML
                    </button>
                    <button
                      onClick={() => setSandboxTab('css')}
                      className={`flex-1 px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                        sandboxTab === 'css'
                          ? 'bg-white text-blue-600 shadow-sm'
                          : 'text-gray-600 hover:text-gray-900'
                      }`}
                    >
                      CSS
                    </button>
                    <button
                      onClick={() => setSandboxTab('javascript')}
                      className={`flex-1 px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                        sandboxTab === 'javascript'
                          ? 'bg-white text-yellow-600 shadow-sm'
                          : 'text-gray-600 hover:text-gray-900'
                      }`}
                    >
                      JavaScript
                    </button>
                  </div>

                  {/* Editor */}
                  <div className="flex-1">
                    {sandboxTab === 'html' && (
                      <CodeEditor code={html} onChange={setHtml} language="html" />
                    )}
                    {sandboxTab === 'css' && (
                      <CodeEditor code={css} onChange={setCss} language="css" />
                    )}
                    {sandboxTab === 'javascript' && (
                      <CodeEditor code={javascript} onChange={setJavascript} language="javascript" />
                    )}
                  </div>
                </div>

                {/* Right: Preview */}
                <div className="flex flex-col">
                  <CodePreview html={html} css={css} javascript={javascript} />
                </div>
              </div>

              {/* Info */}
              <div className="mt-8 p-4 bg-green-50 border border-green-200 rounded-lg">
                <p className="text-green-700 text-sm">
                  💡 <strong>Tip:</strong> 코드를 수정하면 실시간으로 프리뷰가 업데이트됩니다. 샌드박스는 격리된 환경에서 실행됩니다.
                </p>
              </div>
            </div>
          )}
        </div>
      </div>
    </main>
  );
}
