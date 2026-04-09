import http from './http'

export function login(data) {
  return http.post('/auth/login', data)
}

export function register(data) {
  return http.post('/auth/register', data)
}

export function getMe() {
  return http.get('/auth/me')
}

export function changePassword(data) {
  return http.post('/auth/change-password', data)
}

export function createKb(data) {
  return http.post('/kb', data)
}

export function listKb(params) {
  return http.get('/kb', { params })
}

export function deleteKb(id) {
  return http.delete(`/kb/${id}`)
}

export function uploadDocument(formData, onUploadProgress) {
  return http.post('/documents/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress
  })
}

export function batchUploadDocuments(formData, onUploadProgress) {
  return http.post('/documents/batch-upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress
  })
}

export function uploadDocumentVersion(id, formData, onUploadProgress) {
  return http.post(`/documents/${id}/new-version`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress
  })
}

export function listDocuments(params) {
  return http.get('/documents', { params })
}

export function reviewDocument(id, params) {
  return http.post(`/documents/${id}/review`, null, { params })
}

export function setDocumentTags(id, tags) {
  return http.post(`/documents/${id}/tags`, null, { params: { tags } })
}

export function batchDeleteDocuments(ids) {
  return http.post('/documents/batch-delete', ids)
}

export function deleteDocument(id) {
  return http.delete(`/documents/${id}`)
}

export function askQuestion(data) {
  return http.post('/qa/ask', data)
}

export function hotQuestions() {
  return http.get('/stats/hot-questions')
}

export function documentUsage() {
  return http.get('/stats/document-usage')
}

export function adminUsers() {
  return http.get('/admin/users')
}

export function updateAdminUser(id, data) {
  return http.put(`/admin/users/${id}`, data)
}

export function adminConfigs() {
  return http.get('/admin/configs')
}

export function saveAdminConfig(data) {
  return http.post('/admin/configs', data)
}

export function adminLogs(params) {
  return http.get('/admin/logs', { params })
}

export function adminOverview() {
  return http.get('/admin/overview')
}
