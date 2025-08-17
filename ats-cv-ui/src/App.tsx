import React, { useState } from 'react';
import axios from 'axios';
import './App.css';

function App() {
  // === STATE DEĞİŞKENLERİ ===
  // Kullanıcının seçtiği dosyayı tutar.
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  // Yükleme (API çağrısı) durumunu tutar. Butonları pasif yapmak için.
  const [isLoading, setIsLoading] = useState<boolean>(false);
  // Hata mesajlarını kullanıcıya göstermek için.
  const [error, setError] = useState<string>('');
  // Kullanıcıya süreç hakkında bilgi vermek için durum mesajı.
  const [statusMessage, setStatusMessage] = useState<string>('');

  // Dosya seçme input'u her değiştiğinde bu fonksiyon çalışır.
  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      setSelectedFile(file);
      setError(''); // Yeni dosya seçildiğinde eski hataları temizle.
      setStatusMessage(''); // Yeni dosya seçildiğinde eski durum mesajını temizle.
    }
  };

  // "Dönüştür ve İndir" butonuna tıklandığında bu fonksiyon çalışır.
  const handleUpload = async () => {
    // Önce kontrol: Dosya seçili mi?
    if (!selectedFile) {
      setError('Lütfen önce bir PDF dosyası seçin!');
      return;
    }

    // Yükleme sürecini başlat
    setIsLoading(true);
    setError('');
    setStatusMessage('CV dönüştürme süreci başlatılıyor...');

    const formData = new FormData();
    // Bu 'file' anahtarı, backend'deki @RequestParam("file") ile aynı olmalı.
    formData.append('file', selectedFile);

    try {
      // Backend API'ına POST isteği gönderiyoruz.
      const response = await axios.post('http://localhost:8080/api/v1/cv/convert', formData, {
        // Gelen cevabın bir dosya (binary data) olacağını belirtiyoruz.
        responseType: 'blob',
      });

      setStatusMessage('Yeni CV başarıyla oluşturuldu! İndiriliyor...');

      // Gelen "blob" verisinden geçici bir URL oluşturuyoruz.
      const fileURL = window.URL.createObjectURL(new Blob([response.data]));

      // Bu URL'i kullanarak dosyayı indirmek için görünmez bir link yaratıyoruz.
      const link = document.createElement('a');
      link.href = fileURL;
      link.setAttribute('download', 'ATS_Uyumlu_CV.pdf'); // İndirilen dosyanın adı
      document.body.appendChild(link);

      // Linke programatik olarak tıklayıp indirmeyi başlatıyoruz.
      link.click();

      // Kullandıktan sonra o geçici linki ve URL'i temizliyoruz.
      link.remove();
      window.URL.revokeObjectURL(fileURL);

    } catch (err: any) {
      console.error('Dosya dönüştürülürken bir hata oluştu!', err);
      // Backend'den gelen özel hata mesajını göstermeye çalış.
      if (err.response && err.response.data) {
        // Blob veriyi metne dönüştürmek için bir FileReader kullan.
        const reader = new FileReader();
        reader.onload = () => {
          const errorMessage = reader.result as string;
          setError(`Hata: ${errorMessage}`);
        };
        reader.readAsText(err.response.data);
      } else {
        // Genel bir ağ hatası veya başka bir sorun.
        setError('Bir hata oluştu. Sunucuya ulaşılamıyor veya PDF dosyası geçersiz.');
      }
    } finally {
      // İşlem başarılı da olsa, hatalı da olsa yükleme durumunu bitir.
      setIsLoading(false);
      setStatusMessage('');
    }
  };

  return (
    <div className="App">
      <div className="converter-container">
        <h1>ATS Uyumlu CV Dönüştürücü</h1>
        <p>PDF formatındaki CV'nizi yükleyerek Aday Takip Sistemleri (ATS) ile uyumlu, temiz bir versiyonunu oluşturun.</p>

        <label htmlFor="file-input" className="file-input-label">
          PDF Dosyası Seç
        </label>
        <input
          id="file-input"
          type="file"
          accept=".pdf"
          onChange={handleFileChange}
        />

        {selectedFile && (
          <div className="file-name">
            Seçilen Dosya: {selectedFile.name}
          </div>
        )}

        {isLoading && <div className="loading-text">{statusMessage}</div>}
        {error && <div className="error-text">{error}</div>}

        {selectedFile && (
          <button onClick={handleUpload} className="upload-button" disabled={isLoading}>
            {isLoading ? 'Dönüştürülüyor...' : 'Dönüştür ve İndir'}
          </button>
        )}
      </div>
    </div>
  );
}

export default App;