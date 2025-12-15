'use client';

import { useMemo } from 'react';

interface CodePreviewProps {
  html: string;
  css: string;
  javascript: string;
}

export function CodePreview({ html, css, javascript }: CodePreviewProps) {
  const srcDoc = useMemo(() => {
    return `
      <!DOCTYPE html>
      <html>
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <style>
            * {
              margin: 0;
              padding: 0;
              box-sizing: border-box;
            }
            body {
              padding: 20px;
              font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            }
            ${css}
          </style>
        </head>
        <body>
          ${html}
          <script>
            try {
              ${javascript}
            } catch (error) {
              console.error('Error:', error);
              document.body.innerHTML += '<div style="color: red; padding: 10px; background: #fee; border: 1px solid red; margin-top: 10px;">Error: ' + error.message + '</div>';
            }
          </script>
        </body>
      </html>
    `;
  }, [html, css, javascript]);

  return (
    <div className="h-full flex flex-col bg-white rounded-lg overflow-hidden">
      <div className="flex items-center justify-between px-4 py-2 bg-gray-100 border-b border-gray-300">
        <span className="text-sm text-gray-600 font-medium">Preview</span>
        <div className="flex gap-2">
          <button className="text-xs text-gray-500 hover:text-gray-700">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path>
              <polyline points="15 3 21 3 21 9"></polyline>
              <line x1="10" y1="14" x2="21" y2="3"></line>
            </svg>
          </button>
        </div>
      </div>
      <iframe
        key={srcDoc}
        srcDoc={srcDoc}
        className="flex-1 w-full border-0"
        sandbox="allow-scripts"
        title="Code Preview"
      />
    </div>
  );
}
