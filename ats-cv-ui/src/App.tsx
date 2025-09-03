import React, { useState } from 'react';
import axios from 'axios';
import './App.css';

function App() {
  // === STATE DEĞİŞKENLERİ ===
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string>('');
  const [statusMessage, setStatusMessage] = useState<string>('');

  // Yeni özellikler için yeni state'ler
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

  const handleUpload = async () => {
    // Önce kontroller
    if (!selectedFile) {
      setError('Lütfen önce bir PDF dosyası seçin!');
      return;
    }
    if (generateCoverLetter && !jobDescription.trim()) {
      setError('Ön yazı oluşturmak için lütfen iş ilanı metnini girin.');
      return;
    }

    // Süreci başlat
    setIsLoading(true);
    setError('');
    setStatusMessage('Süreç başlatılıyor...');

    const formData = new FormData();
    formData.append('file', selectedFile);
    formData.append('jobDescription', jobDescription);
    formData.append('generateCoverLetter', String(generateCoverLetter));

    try {
      setStatusMessage('Dosyalarınız sunucuya yükleniyor...');
      const response = await axios.post('http://localhost:8080/api/v1/cv/generate', formData, {
        responseType: 'blob',
        // Kullanıcıya yükleme ilerlemesini göstermek için bir bonus (isteğe bağlı)
        onUploadProgress: (progressEvent) => {
          if (progressEvent.total) {
            const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
            setStatusMessage(`Yükleniyor... ${percentCompleted}%`);
          }
        }
      });

      setStatusMessage('Yapay zeka verileri işliyor... Bu işlem biraz sürebilir.');

      // Axios'un cevabı tamamlanana kadar beklediğini varsayıyoruz.
      // Gerçek bir ilerleme çubuğu için WebSocket gibi daha karmaşık bir yapı gerekir.
      // Bu haliyle, indirme başlayana kadar bu mesaj görünecek.

      const downloadFilename = generateCoverLetter ? "CV_ve_On_Yazi.zip" : "ATS_Uyumlu_CV.pdf";

      const fileURL = window.URL.createObjectURL(new Blob([response.data]));
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

        {/* Adım 1: CV Yükleme */}
        <label htmlFor="file-input" className="file-input-label">
          1. PDF CV'nizi Seçin
        </label>
        <input
          id="file-input"
          type="file"
          accept=".pdf"
          onChange={handleFileChange}
          disabled={isLoading}
        />
        {selectedFile && <div className="file-name">Seçilen: {selectedFile.name}</div>}

        {/* Adım 2: Ön Yazı (İsteğe Bağlı) */}
        <div className="cover-letter-section">
          <div className="checkbox-container">
            <input
              type="checkbox"
              id="cover-letter-checkbox"
              checked={generateCoverLetter}
              onChange={(e) => setGenerateCoverLetter(e.target.checked)}
              disabled={isLoading}
            />
            <label htmlFor="cover-letter-checkbox">2. İsteğe Bağlı: Ön Yazı Oluştur</label>
          </div>
          {generateCoverLetter && (
            <textarea
              className="job-description-textarea"
              placeholder="Başvurduğunuz işin ilan metnini buraya yapıştırın..."
              value={jobDescription}
              onChange={(e) => setJobDescription(e.target.value)}
              disabled={isLoading}
            />
          )}
        </div>

        {/* Sonuç ve Buton */}
        <div className="action-section">
            {isLoading && <div className="loading-text">{statusMessage}</div>}
            {error && <div className="error-text">{error}</div>}

            {selectedFile && (
            <button onClick={handleUpload} className="upload-button" disabled={isLoading}>
                {isLoading ? 'İşleniyor...' : '3. Oluştur ve İndir'}
            </button>
            )}
        </div>
      </div>
    </div>
  );
}

export default App;