import React, { useState, useEffect } from 'react';
import './App.css';

import appLogoBackground from './assets/images/karakter-arka-plan.png';
import appLogoArm from './assets/images/karakter-kol.png';

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
  const [isStep2Confirmed, setIsStep2Confirmed] = useState(false);
  const [isDragging, setIsDragging] = useState(false);
  const [apiKey, setApiKey] = useState<string>(() => localStorage.getItem('geminiApiKey') || '');

  useEffect(() => { localStorage.setItem('geminiApiKey', apiKey); }, [apiKey]);

  const isStep1Complete = !!selectedFile;
  const isStep2Complete = isStep1Complete && (!generateCoverLetter || isStep2Confirmed);
  const isStep3Complete = apiKey.trim() !== '';

  useEffect(() => {
    const step1 = document.getElementById('step-card-1');
    const step2 = document.getElementById('step-card-2');
    const step3 = document.getElementById('step-card-3');
    [step1, step2, step3].forEach(step => step?.classList.remove('highlight'));
    step1?.classList.toggle('completed', isStep1Complete);
    step2?.classList.toggle('completed', isStep2Complete);
    step3?.classList.toggle('completed', isStep3Complete);
    if (!isStep1Complete) step1?.classList.add('highlight');
    else if (!isStep2Complete) step2?.classList.add('highlight');
    else if (!isStep3Complete) step3?.classList.add('highlight');
  }, [isStep1Complete, isStep2Complete, isStep3Complete]);

  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) { setSelectedFile(file); setError(''); setStatusMessage(''); }
    event.target.value = '';
  };
  const handleFileRemove = () => { setSelectedFile(null); setIsStep2Confirmed(false); };
  const handleStep2Confirm = () => { setIsStep2Confirmed(true); };
  const handleCoverLetterCheckboxChange = (e: React.ChangeEvent<HTMLInputElement>) => { setGenerateCoverLetter(e.target.checked); setIsStep2Confirmed(false); };
  const handleDragOver = (event: React.DragEvent<HTMLLabelElement>) => { event.preventDefault(); setIsDragging(true); };
  const handleDragLeave = (event: React.DragEvent<HTMLLabelElement>) => { event.preventDefault(); setIsDragging(false); };
  const handleDrop = (event: React.DragEvent<HTMLLabelElement>) => {
    event.preventDefault(); setIsDragging(false);
    const file = event.dataTransfer.files[0];
    if (file && file.type === "application/pdf") { setSelectedFile(file); setError(''); setStatusMessage(''); }
    else { setError('Lütfen sadece PDF formatında bir dosya sürükleyin.'); }
  };

  const handleUpload = async () => {
    if (!selectedFile || !apiKey.trim() || (generateCoverLetter && !isStep2Confirmed)) {
        setError('Lütfen tüm adımları tamamladığınızdan emin olun.');
        return;
    }

    setIsLoading(true);
    setError('');
    setStatusMessage('Sunucuya bağlanılıyor...');

    const formData = new FormData();
    formData.append('file', selectedFile);
    formData.append('jobDescription', jobDescription);
    formData.append('generateCoverLetter', String(generateCoverLetter));
    formData.append('apiKey', apiKey);

    const processLine = (line: string) => {
        if (!line.startsWith('data:')) return;

        try {
            const jsonData = line.substring(5).trim();
            if (!jsonData) return;

            const update = JSON.parse(jsonData);
            if (update.message) { // Sadece message alanı varsa UI'ı güncelle
                setStatusMessage(update.message);
            }

            if (update.stage === 'DOWNLOAD_READY') {
                const filename = update.message;
                const fileId = update.data;
                const downloadUrl = `http://localhost:8080/api/v1/cv/download?fileId=${fileId}&filename=${filename}`;

                const link = document.createElement('a');
                link.href = downloadUrl;
                link.setAttribute('download', filename);
                document.body.appendChild(link);
                link.click();
                link.remove();

                setIsLoading(false);
            } else if (update.stage === 'ERROR') {
                throw new Error(update.message);
            }
        } catch (e: any) {
            console.error('Gelen JSON verisi ayrıştırılamadı:', line, e);
            setError(`Bir hata oluştu: ${e.message}`);
            setIsLoading(false);
        }
    };

    try {
        const response = await fetch('http://localhost:8080/api/v1/cv/generate-stream', {
            method: 'POST',
            body: formData,
            headers: { 'Cache-Control': 'no-cache' },
        });

        if (!response.ok || !response.body) {
            throw new Error(`Sunucu hatası: ${response.status} ${response.statusText}`);
        }

        const reader = response.body.pipeThrough(new TextDecoderStream()).getReader();
        let buffer = '';

        while (true) {
            const { value, done } = await reader.read();
            if (done) break;

            buffer += value;
            while (buffer.includes('\n')) {
                const newlineIndex = buffer.indexOf('\n');
                const line = buffer.substring(0, newlineIndex);
                buffer = buffer.substring(newlineIndex + 1);
                processLine(line);
            }
        }
        if (buffer.length > 0) {
            processLine(buffer);
        }

    } catch (err: any) {
        console.error('İşlem sırasında bir hata oluştu!', err);
        setError(err.message || 'Beklenmedik bir hata oluştu.');
        setIsLoading(false);
        setStatusMessage('');
    }
  };

  return (
    <div className="main-layout">
        <div className="branding-column">
            <div className={`logo-container ${isLoading ? 'is-writing' : ''}`}>
                <img src={appLogoBackground} alt="ATS Kariyer Asistanı" className="logo-background" />
                <img src={appLogoArm} alt="Yazan kol" className="writing-arm" />
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
                    <input type="file" id="file-input" accept=".pdf" style={{ display: 'none' }} onChange={handleFileChange} disabled={isLoading} />
                    <div id="step-card-1" className="step-card">
                        <div className="step-card-header"><h2><span className="icon">{isStep1Complete ? '✅' : '🚀'}</span>1. Adım: CV'nizi Yükleyin</h2></div>
                        {!selectedFile ? (
                            <label htmlFor="file-input" className={`upload-prompt ${isDragging ? 'dragging-over' : ''}`} onDragOver={handleDragOver} onDragLeave={handleDragLeave} onDrop={handleDrop}>
                                <div className="upload-prompt-text"><p>{isDragging ? "Dosyayı Buraya Bırakın" : "Başlamak için PDF dosyanızı buraya tıklayarak seçin veya sürükleyip bırakın."}</p></div>
                            </label>
                        ) : (
                            <div className="file-display-box">
                                <span className="file-name">📄 {selectedFile.name}</span>
                                <div className="file-actions"><label htmlFor="file-input" className="action-button primary">Değiştir</label><button onClick={handleFileRemove} className="action-button secondary">Kaldır</button></div>
                            </div>
                        )}
                    </div>
                    <div id="step-card-2" className="step-card">
                        <CoverLetterSection isCompleted={isStep2Complete} generateCoverLetter={generateCoverLetter} onCheckboxChange={handleCoverLetterCheckboxChange} jobDescription={jobDescription} onTextChange={(e) => setJobDescription(e.target.value)} isLoading={isLoading} onStepComplete={handleStep2Confirm} />
                    </div>
                    <div id="step-card-3" className="step-card">
                        <ApiKeyInput isCompleted={isStep3Complete} apiKey={apiKey} onApiKeyChange={(e) => setApiKey(e.target.value)} isLoading={isLoading} />
                        <ActionPanel isLoading={isLoading} error={error} isFileSelected={!!selectedFile} onUpload={handleUpload} />
                    </div>
                </div>
            </div>
        </div>
    </div>
  );
}

export default App;