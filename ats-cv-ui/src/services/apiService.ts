// src/services/apiService.ts

import axios from 'axios';

/**
 * API istekleri için merkezi konfigürasyon.
 * Backend adresi değişirse sadece burayı güncellememiz yeterli olacak.
 */
const API_BASE_URL = 'http://localhost:8080/api/v1/cv';

/**
 * generateCvPackage fonksiyonuna gönderilecek parametreler için bir tip tanımı.
 * Bu, kodumuzu daha güvenli ve okunabilir hale getirir. Ne beklediğimiz çok net.
 */
export interface GeneratePackageParams {
  file: File;
  jobDescription: string;
  generateCoverLetter: boolean;
  onUploadProgress: (progressEvent: any) => void;
}

/**
 * Backend'e CV ve diğer bilgileri göndererek ATS uyumlu CV ve/veya ön yazı paketini oluşturan ana servis fonksiyonu.
 * @param params - `GeneratePackageParams` tipinde, gerekli tüm bilgileri içeren bir obje.
 * @returns Oluşturulan PDF veya ZIP dosyasını içeren bir `Blob` nesnesi döndürür.
 * @throws API'den bir hata dönerse veya ağ hatası olursa, axios hatasını direkt fırlatır.
 */
export const generateCvPackage = async ({
  file,
  jobDescription,
  generateCoverLetter,
  onUploadProgress,
}: GeneratePackageParams): Promise<Blob> => {

  // Backend'e göndereceğimiz verileri hazırlıyoruz.
  const formData = new FormData();
  formData.append('file', file);
  formData.append('jobDescription', jobDescription);
  formData.append('generateCoverLetter', String(generateCoverLetter)); // boolean'ı string'e çeviriyoruz.

  // API isteğini atıyoruz.
  // Not: try...catch bloğu burada DEĞİL, bu fonksiyonu çağıran component'te (App.tsx) olmalı.
  // Servisin görevi isteği yapmak ve başarılıysa veriyi, başarısızsa hatayı döndürmektir.
  // Component'in görevi ise o hatayı yakalayıp kullanıcıya göstermektir. Bu, "separation of concerns" ilkesidir.
  const response = await axios.post(`${API_BASE_URL}/generate`, formData, {
    responseType: 'blob', // Backend'den dosya (binary data) beklediğimizi belirtiyoruz.
    onUploadProgress,     // Yükleme ilerlemesini takip etmek için callback fonksiyonu.
  });

  // İstek başarılıysa, response'un içindeki 'data' kısmını (yani dosyanın kendisini) döndürüyoruz.
  return response.data;
};