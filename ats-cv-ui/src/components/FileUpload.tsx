// src/components/FileUpload.tsx

import React from 'react';

interface FileUploadProps {
  selectedFile: File | null;
  onFileChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
  onFileRemove: () => void;
  isLoading: boolean;
}

export const FileUpload: React.FC<FileUploadProps> = ({
  selectedFile,
  onFileChange,
  onFileRemove,
  isLoading,
}) => {
  return (
    <div className="file-upload-section">
      {!selectedFile && (
        <label htmlFor="file-input" className="file-input-label">
          1. PDF CV'nizi Seçin
        </label>
      )}

      <input
        id="file-input"
        type="file"
        accept=".pdf"
        onChange={onFileChange}
        disabled={isLoading}
        onClick={(event) => (event.currentTarget.value = '')}
      />

      {selectedFile && (
        <div className="file-display-box">
          <span className="file-name">📄 {selectedFile.name}</span>
          <div className="file-actions">
            <label htmlFor="file-input" className="action-button primary">Değiştir</label>
            <button onClick={onFileRemove} className="action-button secondary">Kaldır</button>
          </div>
        </div>
      )}
    </div>
  );
};