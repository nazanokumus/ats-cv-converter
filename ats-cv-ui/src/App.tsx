// src/App.tsx

import React, { useState } from 'react';
import { generateCvPackage } from './services/apiService';
import './App.css';

import { FileUpload } from './components/FileUpload';
import { CoverLetterSection } from './components/CoverLetterSection';
import { ActionPanel } from './components/ActionPanel';

function App() {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string>('');
  const [statusMessage, setStatusMessage] = useState<string>('');
  const [jobDescription, setJobDescription] = useState<string>('');
  const [generateCoverLetter, setGenerateCoverLetter] = useState<boolean>(false);

  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      setSelectedFile(file);
      setError('');
      setStatusMessage('');
    }
  };

  const handleFileRemove = () => {
    setSelectedFile(null);
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setError('Lütfen önce bir PDF dosyası seçin!');
      return;
    }
    if (generateCoverLetter && !jobDescription.trim()) {
      setError('Ön yazı oluşturmak için lütfen iş ilanı metnini girin.');
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
        onUploadProgress: (progressEvent: any) => {
          if (progressEvent.total) {
            const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
            setStatusMessage(`Yükleniyor... ${percentCompleted}%`);
          }
        },
      });

      setStatusMessage('Yapay zeka verileri işliyor... Bu işlem biraz sürebilir.');

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
      if (err.response && err.response.data) {
        const reader = new FileReader();
        reader.onload = () => {
          const errorMessage = reader.result as string;
          setError(`Hata: ${errorMessage}`);
        };
        reader.readAsText(err.response.data);
      } else {
        setError('Bir hata oluştu. Sunucuya ulaşılamıyor veya beklenmedik bir sorun yaşandı.');
      }
    } finally {
      setIsLoading(false);
      setStatusMessage('');
    }
  };

  return (
    <div className="App">
      <div className="converter-container">
        <h1>ATS Kariyer Asistanı</h1>
        <p>CV'nizi ATS uyumlu hale getirin ve başvurduğunuz işe özel bir ön yazı oluşturun.</p>

        <FileUpload
          selectedFile={selectedFile}
          onFileChange={handleFileChange}
          onFileRemove={handleFileRemove}
          isLoading={isLoading}
        />

        <hr className="divider" />

        <CoverLetterSection
          generateCoverLetter={generateCoverLetter}
          onCheckboxChange={(e) => setGenerateCoverLetter(e.target.checked)}
          jobDescription={jobDescription}
          onTextChange={(e) => setJobDescription(e.target.value)}
          isLoading={isLoading}
        />

        <hr className="divider" />

        <ActionPanel
          isLoading={isLoading}
          statusMessage={statusMessage}
          error={error}
          isFileSelected={!!selectedFile}
          onUpload={handleUpload}
        />
      </div>
    </div>
  );
}

export default App;