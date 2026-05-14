const STORAGE_KEY = 'guizhou_job_user'
const AUTH_EVENT = 'guizhou-job-auth-change'

export function getCurrentUser() {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null
  try {
    const user = JSON.parse(raw)
    if (user && !user.token) {
      localStorage.removeItem(STORAGE_KEY)
      return null
    }
    return user
  } catch {
    localStorage.removeItem(STORAGE_KEY)
    return null
  }
}

export function getAuthToken() {
  return getCurrentUser()?.token || ''
}

export function isAdminUser(user = getCurrentUser()) {
  return String(user?.role || '').toUpperCase() === 'ADMIN'
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
