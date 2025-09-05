// src/App.tsx

import React, { useState, useEffect } from 'react';
import { generateCvPackage } from './services/apiService';
import './App.css';

import appLogo from './assets/images/app-logo.png';
import { CoverLetterSection } from './components/CoverLetterSection';
import { ApiKeyInput } from './components/ApiKeyInput';
import { ActionPanel } from './components/ActionPanel';

function App() {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string>('');
  const [statusMessage, setStatusMessage] = useState<string>('');
  const [jobDescription, setJobDescription] = useState<string>('');
  const [generateCoverLetter, setGenerateCoverLetter] = useState<boolean>(false);
  const [apiKey, setApiKey] = useState<string>('');
  const [scrolledToStep, setScrolledToStep] = useState(0);

  const isStep1Complete = !!selectedFile;
  const isStep2Complete = !generateCoverLetter || (generateCoverLetter && jobDescription.trim() !== '');
  const isStep3Complete = apiKey.trim() !== '';

  useEffect(() => {
    const step1 = document.getElementById('step-card-1');
    const step2 = document.getElementById('step-card-2');
    const step3 = document.getElementById('step-card-3');

    [step1, step2, step3].forEach(step => {
      step?.classList.remove('step-highlight', 'step-completed');
    });

    if (isStep1Complete) step1?.classList.add('step-completed');
    if (isStep2Complete) step2?.classList.add('step-completed');
    if (isStep3Complete) step3?.classList.add('step-completed');

    const scrollToStep = (stepElement: HTMLElement | null, stepNumber: number) => {
      if (stepElement && scrolledToStep < stepNumber) {
        setTimeout(() => {
          stepElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }, 100);
        setScrolledToStep(stepNumber);
      }
    };

    if (!isStep1Complete) return;

    if (!isStep2Complete) {
      step2?.classList.add('step-highlight');
      scrollToStep(step2, 2);
    } else if (!isStep3Complete) {
      step3?.classList.add('step-highlight');
      scrollToStep(step3, 3);
    }
  }, [isStep1Complete, isStep2Complete, isStep3Complete, scrolledToStep]);

  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    // =======================================================
    // === DÜZELTME BURADA YAPILDI: ?.; yerine ?.[0] ===
    // =======================================================
    const file = event.target.files?.[0];

    if (file) {
      setSelectedFile(file);
      setError('');
      setStatusMessage('');
      setScrolledToStep(1);
    }
    event.target.value = '';
  };

  const handleFileRemove = () => {
    setSelectedFile(null);
    setScrolledToStep(0);
  };

  const handleUpload = async () => {
    if (!selectedFile) { setError('Lütfen önce bir PDF dosyası seçin!'); return; }
    if (!apiKey.trim()) { setError('Lütfen Gemini API anahtarınızı girin.'); return; }
    if (generateCoverLetter && !jobDescription.trim()) { setError('Ön yazı oluşturmak için lütfen iş ilanı metnini girin.'); return; }

    setIsLoading(true);
    setError('');
    setStatusMessage('Süreç başlatılıyor...');

    try {
      const blob = await generateCvPackage({
        file: selectedFile,
        jobDescription,
        generateCoverLetter,
        apiKey: apiKey,
        onUploadProgress: (progressEvent: any) => {
          if (progressEvent.total) {
            const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
            setStatusMessage(`Yükleniyor... ${percentCompleted}%`);
          }
        },
      });
      setStatusMessage('Yapay zeka verileri işliyor...');
      const downloadFilename = generateCoverLetter ? "CV_ve_On_Yazi.zip" : "ATS_Uyumlu_CV.pdf";
      const fileURL = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = fileURL;
      link.setAttribute('download', downloadFilename);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(fileURL);
    } catch (err: any) {
      console.error('İşlem sırasında bir hata oluştu!', err);
      // Hata mesajı gösterme mantığınızı buraya ekleyebilirsiniz.
    } finally {
      setIsLoading(false);
      setStatusMessage('');
    }
  };

  return (
    <div className="main-layout">
      <div className="branding-column">
        <img src={appLogo} alt="ATS Kariyer Asistanı Logosu" className="app-logo-dominant" />
        <h1 className="main-title">ATS Kariyer Asistanı</h1>
        <p className="main-subtitle">CV'nizi geleceğe hazırlayın. Tek tıkla.</p>
      </div>

      <div className="form-column">
        <div className="container-wrapper">
          {isLoading && (
            <div className="loading-overlay">
              <div className="spinner"></div>
              <div className="loading-text-overlay">{statusMessage}</div>
            </div>
          )}

          <div className="converter-container">
            <input
              type="file"
              id="file-input"
              accept=".pdf"
              style={{ display: 'none' }}
              onChange={handleFileChange}
              disabled={isLoading}
            />

            <div id="step-card-1" className="step-card">
              <div className="step-card-header">
                <h2>
                  <span className="icon">{isStep1Complete ? '✅' : '🚀'}</span>
                  1. Adım: CV'nizi Yükleyin
                </h2>
              </div>
              {!selectedFile ? (
                <label htmlFor="file-input" className="upload-prompt">
                  <div className="upload-prompt-text">
                    <p>Başlamak için PDF dosyanızı buraya tıklayarak seçin.</p>
                  </div>
                </label>
              ) : (
                <div className="file-display-box">
                  <span className="file-name">📄 {selectedFile.name}</span>
                  <div className="file-actions">
                    <label htmlFor="file-input" className="action-button primary">Değiştir</label>
                    <button onClick={handleFileRemove} className="action-button secondary">Kaldır</button>
                  </div>
                </div>
              )}
            </div>

            <div id="step-card-2" className="step-card">
              <CoverLetterSection
                isCompleted={isStep2Complete}
                generateCoverLetter={generateCoverLetter}
                onCheckboxChange={(e) => setGenerateCoverLetter(e.target.checked)}
                jobDescription={jobDescription}
                onTextChange={(e) => setJobDescription(e.target.value)}
                isLoading={isLoading}
              />
            </div>

            <div id="step-card-3" className="step-card">
              <ApiKeyInput
                isCompleted={isStep3Complete}
                apiKey={apiKey}
                onApiKeyChange={(e) => setApiKey(e.target.value)}
                isLoading={isLoading}
              />
            </div>

            <ActionPanel
              isLoading={isLoading}
              error={error}
              isFileSelected={!!selectedFile}
              onUpload={handleUpload}
            />
          </div>
        </div>
      </div>
    </div>
  );
}

export default App;