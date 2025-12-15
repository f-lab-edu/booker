'use client';

import { useState } from 'react';

interface CodeEditorProps {
  code: string;
  onChange: (code: string) => void;
  language?: 'html' | 'css' | 'javascript';
}

export function CodeEditor({ code, onChange, language = 'html' }: CodeEditorProps) {
  return (
    <div className="h-full flex flex-col bg-gray-900 rounded-lg overflow-hidden">
      <div className="flex items-center justify-between px-4 py-2 bg-gray-800 border-b border-gray-700">
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 rounded-full bg-red-500"></div>
          <div className="w-3 h-3 rounded-full bg-yellow-500"></div>
          <div className="w-3 h-3 rounded-full bg-green-500"></div>
        </div>
        <span className="text-xs text-gray-400 uppercase font-mono">{language}</span>
      </div>
      <textarea
        value={code}
        onChange={(e) => onChange(e.target.value)}
        className="flex-1 p-4 bg-gray-900 text-white font-mono text-sm resize-none focus:outline-none"
        spellCheck={false}
        placeholder={`Enter ${language} code here...`}
      />
    </div>
  );
}
