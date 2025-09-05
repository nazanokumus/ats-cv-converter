// src/services/apiService.ts
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/v1/cv'; // DEPLOY'DAN SONRA BURAYI DEĞİŞTİRECEĞİZ

export interface GeneratePackageParams {
  file: File;
  apiKey: string;
  jobDescription: string;
  generateCoverLetter: boolean;
  onUploadProgress: (progressEvent: any) => void;
}

export const generateCvPackage = async ({
  file,
  apiKey,
  jobDescription,
  generateCoverLetter,
  onUploadProgress,
}: GeneratePackageParams): Promise<Blob> => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('apiKey', apiKey);
  formData.append('jobDescription', jobDescription);
  formData.append('generateCoverLetter', String(generateCoverLetter));

  const response = await axios.post(`${API_BASE_URL}/generate`, formData, {
    responseType: 'blob',
    onUploadProgress,
  });

  return response.data;
};