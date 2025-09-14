// src/App.tsx

import React, { useState, useEffect } from 'react';
import { generateCvPackage } from './services/apiService';
import './App.css';

// Resimleriniz doğru yerden, standart React yöntemiyle import ediliyor.
import appLogoBackground from './assets/images/karakter-arka-plan.png';
import appLogoArm from './assets/images/karakter-kol.png';

import { CoverLetterSection } from './components/CoverLetterSection';
import { ApiKeyInput } from './components/ApiKeyInput';
import { ActionPanel } from './components/ActionPanel';

function App() {
  // === STATE YÖNETİMİ ===
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string>('');
  const [statusMessage, setStatusMessage] = useState<string>('');
  const [jobDescription, setJobDescription] = useState<string>('');
  const [generateCoverLetter, setGenerateCoverLetter] = useState<boolean>(false);
  const [isStep2Confirmed, setIsStep2Confirmed] = useState(false);
  const [isDragging, setIsDragging] = useState(false);

  // API anahtarını tarayıcı hafızasından (localStorage) okuyarak başlatır.
  const [apiKey, setApiKey] = useState<string>(() => localStorage.getItem('geminiApiKey') || '');

  // apiKey her değiştiğinde, yeni değeri tarayıcı hafızasına kaydeder.
  useEffect(() => {
    localStorage.setItem('geminiApiKey', apiKey);
  }, [apiKey]);

  // === ADIMLARIN TAMAMLANMA DURUMUNU HESAPLA ===
  const isStep1Complete = !!selectedFile;
  const isStep2Complete = isStep1Complete && (!generateCoverLetter || isStep2Confirmed);
  const isStep3Complete = apiKey.trim() !== '';

  // === BEYİN: STİL GÜNCELLEME ===
  useEffect(() => {
    const step1 = document.getElementById('step-card-1');
    const step2 = document.getElementById('step-card-2');
    const step3 = document.getElementById('step-card-3');

    [step1, step2, step3].forEach(step => {
      step?.classList.remove('step-highlight');
    });

    step1?.classList.toggle('step-completed', isStep1Complete);
    step2?.classList.toggle('step-completed', isStep2Complete);
    step3?.classList.toggle('step-completed', isStep3Complete);

    if (!isStep1Complete) {
      step1?.classList.add('step-highlight');
    } else if (!isStep2Complete) {
      step2?.classList.add('step-highlight');
    } else if (!isStep3Complete) {
      step3?.classList.add('step-highlight');
    }
  }, [isStep1Complete, isStep2Complete, isStep3Complete]);

  // === EVENT HANDLER'LAR ===
  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      setSelectedFile(file);
      setError('');
      setStatusMessage('');
    }
    event.target.value = '';
  };

  const handleFileRemove = () => {
    setSelectedFile(null);
    setIsStep2Confirmed(false);
  };

  const handleStep2Confirm = () => {
    setIsStep2Confirmed(true);
  };

  const handleCoverLetterCheckboxChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setGenerateCoverLetter(e.target.checked);
    setIsStep2Confirmed(false);
  };

  const handleDragOver = (event: React.DragEvent<HTMLLabelElement>) => {
    event.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (event: React.DragEvent<HTMLLabelElement>) => {
    event.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (event: React.DragEvent<HTMLLabelElement>) => {
    event.preventDefault();
    setIsDragging(false);
    const file = event.dataTransfer.files[0];
    if (file && file.type === "application/pdf") {
      setSelectedFile(file);
      setError('');
      setStatusMessage('');
    } else {
      setError('Lütfen sadece PDF formatında bir dosya sürükleyin.');
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) { setError('Lütfen önce bir PDF dosyası seçin!'); return; }
    if (!apiKey.trim()) { setError('Lütfen Gemini API anahtarınızı girin.'); return; }
    if (generateCoverLetter && !isStep2Confirmed) {
      setError('Lütfen ön yazı adımını "Onayla ve Devam Et" butonuyla tamamlayın.');
      return;
    }

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
    } finally {
      setIsLoading(false);
      setStatusMessage('');
    }
  };

  // === RENDER BÖLÜMÜ ===
  return (
    <div className="main-layout">
      <div className="branding-column">
        <div className={`logo-container ${isLoading ? 'is-writing' : ''}`}>
          <img
            src={appLogoBackground}
            alt="ATS Kariyer Asistanı"
            className="logo-background"
          />
          <img
            src={appLogoArm}
            alt="Yazan kol"
            className="writing-arm"
          />
        </div>
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
                <label
                  htmlFor="file-input"
                  className={`upload-prompt ${isDragging ? 'dragging-over' : ''}`}
                  onDragOver={handleDragOver}
                  onDragLeave={handleDragLeave}
                  onDrop={handleDrop}
                >
                  <div className="upload-prompt-text">
                    <p>
                      {isDragging
                        ? "Dosyayı Buraya Bırakın"
                        : "Başlamak için PDF dosyanızı buraya tıklayarak seçin veya sürükleyip bırakın."}
                    </p>
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
                onCheckboxChange={handleCoverLetterCheckboxChange}
                jobDescription={jobDescription}
                onTextChange={(e) => setJobDescription(e.target.value)}
                isLoading={isLoading}
                onStepComplete={handleStep2Confirm}
              />
            </div>
            <div id="step-card-3" className="step-card">
              <ApiKeyInput
                isCompleted={isStep3Complete}
                apiKey={apiKey}
                onApiKeyChange={(e) => setApiKey(e.target.value)}
                isLoading={isLoading}
              />
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
    </div>
  );
}

export default App;