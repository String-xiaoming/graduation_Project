const STORAGE_KEY = 'guizhou_job_user'
const AUTH_EVENT = 'guizhou-job-auth-change'

export function getCurrentUser() {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    localStorage.removeItem(STORAGE_KEY)
    return null
  }
}

export function setCurrentUser(user) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(user))
  window.dispatchEvent(new CustomEvent(AUTH_EVENT, { detail: user }))
}

export function clearCurrentUser() {
  localStorage.removeItem(STORAGE_KEY)
  window.dispatchEvent(new CustomEvent(AUTH_EVENT, { detail: null }))
}

export function onAuthChange(callback) {
  const handler = (event) => callback(event.detail)
  window.addEventListener(AUTH_EVENT, handler)
  return () => window.removeEventListener(AUTH_EVENT, handler)
}
