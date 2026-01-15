import type { Metadata } from 'next';
import './globals.css';
import React from "react";

export const metadata: Metadata = {
  title: 'AIDE - AI Documentation Search',
  description: 'AI-powered documentation search engine',
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
