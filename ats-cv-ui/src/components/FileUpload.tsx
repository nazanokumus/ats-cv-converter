// src/components/FileUpload.tsx

import React from 'react';

interface FileUploadProps {
  onFileChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
  isLoading: boolean;
}

export const FileUpload: React.FC<FileUploadProps> = ({ onFileChange, isLoading }) => {
  return (
    <input
      id="file-input"
      type="file"
      accept=".pdf"
      style={{ display: 'none' }} // Gizli kalmaya devam ediyor
      onChange={onFileChange}
      disabled={isLoading}
      onClick={(event) => (event.currentTarget.value = '')}
    />
  );
};