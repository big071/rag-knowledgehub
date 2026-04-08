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

export function createKb(data) {
  return http.post('/kb', data)
}

export function listKb(params) {
  return http.get('/kb', { params })
}

export function deleteKb(id) {
  return http.delete(`/kb/${id}`)
}

export function uploadDocument(formData) {
  return http.post('/documents/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function listDocuments(params) {
  return http.get('/documents', { params })
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
