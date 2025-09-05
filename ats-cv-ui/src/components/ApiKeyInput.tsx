// src/components/ApiKeyInput.tsx

import React, { useState } from 'react';

interface ApiKeyInputProps {
  isCompleted: boolean;
  apiKey: string;
  onApiKeyChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  isLoading: boolean;
}

export function ApiKeyInput({ isCompleted, apiKey, onApiKeyChange, isLoading }: ApiKeyInputProps) {
  const [isHelpVisible, setIsHelpVisible] = useState(false);

  const toggleHelp = (e: React.MouseEvent) => {
    e.preventDefault();
    setIsHelpVisible(!isHelpVisible);
  };

  return (
    <div className="api-key-section">
      <div className="step-card-header">
        <h2>
          <span className="icon">{isCompleted ? '✅' : '🔑'}</span>
          Gemini API Anahtarınız
        </h2>
      </div>

      <input
        type="password"
        value={apiKey}
        onChange={onApiKeyChange}
        placeholder="API anahtarınızı buraya yapıştırın..."
        disabled={isLoading}
      />

      <div className="helper-text-container">
        <span className="helper-text">
          Google AI Studio'dan Gemini API anahtarınızı{' '}
          <a href="https://aistudio.google.com/app/apikey" target="_blank" rel="noopener noreferrer">
            ücretsiz alabilirsiniz
          </a>.
        </span>

        <button type="button" onClick={toggleHelp} className="help-toggle">
          {isHelpVisible ? 'Gizle' : 'Nasıl bulurum?'}
        </button>
      </div>

      {isHelpVisible && (
        <div className="help-box">
          <h4>API Anahtarını Bulma Adımları:</h4>
          <ol>
            <li>Yukarıdaki linke tıklayarak <strong>Google AI Studio</strong>'ya gidin.</li>
            <li><strong>"Get API key"</strong> (API anahtarı al) butonuna tıklayın.</li>
            <li>Açılan pencerede <strong>"Create API key in new project"</strong> (Yeni projede API anahtarı oluştur) seçeneğine tıklayın.</li>
            <li>Oluşturulan uzun karakter dizisini kopyalayın ve yukarıdaki kutucuğa yapıştırın. Hepsi bu kadar!</li>
          </ol>
        </div>
      )}
    </div>
  );
}