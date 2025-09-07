// src/components/QuillPenIcon.tsx

import React from 'react';

export const QuillPenIcon = () => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 512 512"
    className="quill-pen-overlay"
  >
    <path
      fill="#DDCBBE" // Kalem rengi (açık kahve/kemik)
      stroke="#4A4A4A" // Kalem dış çizgisi
      strokeWidth="8"
      strokeLinecap="round"
      strokeLinejoin="round"
      d="M16 448l32-128 272-272a31.99 31.99 0 0145.28 0l89.02 89.02a31.99 31.99 0 010 45.28L182.59 496H48a32 32 0 01-32-32v-8z"
    />
    <path
      fill="none" // Tüy çizgileri
      stroke="#4A4A4A"
      strokeWidth="8"
      strokeLinecap="round"
      strokeLinejoin="round"
      d="M320 64l-48 48m-48-48l-48 48m192.01-42.85L224 112"
    />
  </svg>
);