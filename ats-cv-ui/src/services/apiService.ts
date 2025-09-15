import axios from 'axios';

/**
 * Backend API'nizin ana adresini ve temel ayarlarını içeren
 * merkezi bir Axios instance'ı oluşturuyoruz.
 */
const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

/*
 * =============================================================================
 * ÖNEMLİ NOT: GERÇEK ZAMANLI İLERLEME TAKİBİ (SERVER-SENT EVENTS)
 * =============================================================================
 *
 * Bu dosya, projedeki standart "istek-cevap" modelli API çağrıları için
 * bir temel olarak tasarlanmıştır.
 *
 * CV oluşturma sürecinin anlık ilerlemesini takip eden (/generate-stream)
 * Server-Sent Events (SSE) bağlantısı, tarayıcının yerel `fetch` API'si
 * kullanılarak doğrudan `App.tsx` bileşeni içerisinde yönetilmektedir.
 *
 * Axios kütüphanesi, SSE gibi anlık veri akışlarını (streaming) yönetmek için
 * ideal bir araç olmadığından, bu özel işlem için `fetch` tercih edilmiştir.
 *
 * Bu nedenle, CV oluşturma mantığı için bu dosyada bir fonksiyon bulunmamaktadır.
 * =============================================================================
 */

// Gelecekte eklenebilecek diğer standart API çağrıları için örnek:
/*
export const getSomeData = () => {
  return apiClient.get('/some-endpoint');
};

export const postSomeData = (data: any) => {
  return apiClient.post('/another-endpoint', data);
};
*/

export default apiClient;