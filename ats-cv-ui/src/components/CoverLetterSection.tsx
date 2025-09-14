// src/components/CoverLetterSection.tsx

import React from 'react';

interface CoverLetterSectionProps {
  isCompleted: boolean;
  generateCoverLetter: boolean;
  onCheckboxChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  jobDescription: string;
  onTextChange: (e: React.ChangeEvent<HTMLTextAreaElement>) => void;
  isLoading: boolean;
  onStepComplete: () => void;
}

export const CoverLetterSection: React.FC<CoverLetterSectionProps> = ({
  isCompleted,
  generateCoverLetter,
  onCheckboxChange,
  jobDescription,
  onTextChange,
  isLoading,
  onStepComplete,
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
        <>
          <textarea
            className="job-description-textarea"
            placeholder="Başvurduğunuz işin ilan metnini buraya yapıştırın..."
            value={jobDescription}
            onChange={onTextChange}
            disabled={isLoading}
          />

          {/*
            Buton, sadece metin alanı doluyken VE adım henüz onaylanmamışken görünür.
            Böylece butona basıldıktan sonra (ve isCompleted true olunca) kaybolur.
          */}
          {jobDescription.trim() !== '' && !isCompleted && (
            <div className="step-confirm-button-container">
              <button
                type="button"
                className="action-button primary"
                onClick={onStepComplete}
                disabled={isLoading}
              >
                Onayla ve Devam Et
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
};