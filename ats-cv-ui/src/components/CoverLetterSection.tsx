// src/components/CoverLetterSection.tsx

import React from 'react';

interface CoverLetterSectionProps {
  isCompleted: boolean;
  generateCoverLetter: boolean;
  onCheckboxChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  jobDescription: string;
  onTextChange: (e: React.ChangeEvent<HTMLTextAreaElement>) => void;
  isLoading: boolean;
}

export const CoverLetterSection: React.FC<CoverLetterSectionProps> = ({
  isCompleted,
  generateCoverLetter,
  onCheckboxChange,
  jobDescription,
  onTextChange,
  isLoading,
}) => {
  return (
    <div className="cover-letter-section">
      <div className="step-card-header">
        <h2>
          <span className="icon">{isCompleted ? '✅' : '📄'}</span>
          2. İsteğe Bağlı: Ön Yazı Oluştur
        </h2>
      </div>

      <label className="checkbox-container">
        <input
          type="checkbox"
          id="cover-letter-checkbox"
          checked={generateCoverLetter}
          onChange={onCheckboxChange}
          disabled={isLoading}
        />
        <span className="checkbox-label-text">Evet, ilana özel bir ön yazı oluşturulsun.</span>
      </label>

      <p className="checkbox-helper-text">
        Bu seçeneği işaretlerseniz, yapay zeka CV'nizi ve iş ilanı metnini analiz ederek size özel bir ön yazı hazırlayacaktır.
      </p>

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