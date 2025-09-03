// src/components/CoverLetterSection.tsx
import React from 'react';

interface CoverLetterSectionProps {
  generateCoverLetter: boolean;
  onCheckboxChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  jobDescription: string;
  onTextChange: (e: React.ChangeEvent<HTMLTextAreaElement>) => void;
  isLoading: boolean;
}

export const CoverLetterSection: React.FC<CoverLetterSectionProps> = ({
  generateCoverLetter,
  onCheckboxChange,
  jobDescription,
  onTextChange,
  isLoading,
}) => {
  return (
    <div className="cover-letter-section">
      <div className="checkbox-container">
        <input
          type="checkbox"
          id="cover-letter-checkbox"
          checked={generateCoverLetter}
          onChange={onCheckboxChange}
          disabled={isLoading}
        />
        <label htmlFor="cover-letter-checkbox">2. İsteğe Bağlı: Ön Yazı Oluştur</label>
      </div>
      {generateCoverLetter && (
        <textarea
          className="job-description-textarea"
          placeholder="Başvurduğunuz işin ilan metnini buraya yapıştırın..."
          value={jobDescription}
          onChange={onTextChange}
          disabled={isLoading}
        />
      )}
    </div>
  );
};