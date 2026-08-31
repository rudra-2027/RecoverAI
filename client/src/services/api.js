import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
});

api.interceptors.request.use((config) => {
  const apiKey = localStorage.getItem('recoverai_api_key') || import.meta.env.VITE_RECOVERAI_API_KEY;
  if (apiKey) {
    config.headers['X-API-Key'] = apiKey;
  }
  return config;
});

export const fetchMetrics = () => api.get('/metrics').then((res) => res.data);
export const fetchMetricTrends = () => api.get('/metrics/trends').then((res) => res.data);
export const fetchMerchants = () => api.get('/ingest/merchants').then((res) => res.data);
export const fetchFailedMandates = () => api.get('/ingest/failed-mandates').then((res) => res.data);
export const createFailedMandate = (payload) => api.post('/ingest/failed-mandates', payload).then((res) => res.data);
export const fetchDecisions = () => api.get('/decisions').then((res) => res.data);
export const fetchAuditLogs = (mandateId) => api.get(`/audit/${mandateId}`).then((res) => res.data);
export const fetchBatches = () => api.get('/batches').then((res) => res.data);
export const fetchOutcomes = () => api.get('/outcomes').then((res) => res.data);
export const runAgent = (mandateId) => api.post(`/agent/run/${mandateId}`).then((res) => res.data);
export const runAllAgents = () => api.post('/agent/run-all').then((res) => res.data);
export const uploadBatch = (file, process = false) => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('process', String(process));
  return api.post('/batches/upload', formData).then((res) => res.data);
};
export const runBatch = (batchId) => api.post(`/batches/${batchId}/run`).then((res) => res.data);
export const processDecisionBatch = (mandateIds) => api.post('/decisions/process-batch', { mandateIds }).then((res) => res.data);
export const confirmDecision = (mandateId) => api.post(`/decisions/${mandateId}/confirm`).then((res) => res.data);
export const overrideDecision = (mandateId, payload) => api.put(`/decisions/${mandateId}/override`, payload).then((res) => res.data);
export const askAi = (question) => api.post('/ai/chat', { question }).then((res) => res.data);
export const fetchAiInsights = () => api.post('/ai/insights').then((res) => res.data);
export const fetchAiSummary = () => api.post('/ai/summary').then((res) => res.data);
export const fetchRecoverySettings = () => api.get('/settings/recovery').then((res) => res.data);
export const updateRecoverySettings = (payload) => api.put('/settings/recovery', payload).then((res) => res.data);
export const updateMerchantSettings = (merchantId, payload) => api.put(`/settings/merchants/${merchantId}`, payload).then((res) => res.data);
export const fetchSystemStatus = () => api.get('/settings/status').then((res) => res.data);
export const regenerateApiKey = () => api.post('/settings/api-key/regenerate').then((res) => {
  if (res.data?.apiKey) {
    localStorage.setItem('recoverai_api_key', res.data.apiKey);
  }
  return res.data;
});
export const simulateRecovery = (mandate) => api.post('/simulator/recovery', { mandate }).then((res) => res.data);
export const createFeedback = (payload) => api.post('/feedback', payload).then((res) => res.data);
export const fetchFeedback = () => api.get('/feedback').then((res) => res.data);

export const downloadBlob = async (url, fallbackFileName) => {
  const response = await api.get(url, { responseType: 'blob' });
  const disposition = response.headers['content-disposition'] || '';
  const match = disposition.match(/filename="?([^"]+)"?/);
  const fileName = match?.[1] || fallbackFileName;
  const href = URL.createObjectURL(response.data);
  const link = document.createElement('a');
  link.href = href;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(href);
};

export const downloadFailedMandatesCsv = () => downloadBlob('/ingest/failed-mandates/export', 'failed-mandates.csv');
export const downloadAuditCsv = (mandateId) => downloadBlob(`/audit/${mandateId}/export`, `audit-${mandateId}.csv`);
export const downloadBatchReport = (batchId) => downloadBlob(`/batches/${batchId}/report`, `recoverai-batch-${batchId}-report.csv`);

export default api;
