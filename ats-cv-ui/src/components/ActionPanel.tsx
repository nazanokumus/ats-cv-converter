// src/components/ActionPanel.tsx

import React from 'react';

interface ActionPanelProps {
  isLoading: boolean;
  error: string; // statusMessage'ı kaldırdık
  isFileSelected: boolean;
  onUpload: () => void;
}

export const ActionPanel: React.FC<ActionPanelProps> = ({
  isLoading,
  error,
  isFileSelected,
  onUpload,
}) => {
  return (
    <div className="action-section">
      {/* Yükleme mesajı artık burada DEĞİL */}
      {error && <div className="error-text">{error}</div>}

      {isFileSelected && (
        <button onClick={onUpload} className="upload-button" disabled={isLoading}>
          {isLoading ? 'İşleniyor...' : '3. Oluştur ve İndir'}
        </button>
      )}
    </div>
  );
};
