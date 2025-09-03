// src/components/ActionPanel.tsx - DÜZELTİLMİŞ SON HALİ

import React from 'react';

interface ActionPanelProps {
  isLoading: boolean;
  statusMessage: string;
  error: string;
  isFileSelected: boolean;
  onUpload: () => void;
}

export const ActionPanel: React.FC<ActionPanelProps> = ({
  isLoading,
  statusMessage,
  error,
  isFileSelected,
  onUpload,
}) => {
  return (
    <div className="action-section">
      {isLoading && <div className="loading-text">{statusMessage}</div>}
      {error && <div className="error-text">{error}</div>}

      {isFileSelected && (
        <button onClick={onUpload} className="upload-button" disabled={isLoading}>
          {isLoading ? 'İşleniyor...' : '3. Oluştur ve İndir'}
        </button>
      )}
    </div>
  );
};