const TOKEN_KEY = 'access_token'
const REFRESH_TOKEN_KEY = 'refresh_token'

let accessToken = sessionStorage.getItem(TOKEN_KEY) || null
let refreshToken = sessionStorage.getItem(REFRESH_TOKEN_KEY) || null

const channel = typeof BroadcastChannel !== 'undefined'
  ? new BroadcastChannel('auth_channel')
  : null

if (channel) {
  channel.onmessage = (event) => {
    if (event.data.type === 'TOKEN_UPDATE') {
      accessToken = event.data.token
      if (event.data.token) {
        sessionStorage.setItem(TOKEN_KEY, event.data.token)
      } else {
        sessionStorage.removeItem(TOKEN_KEY)
      }
    }
  }
}

export function getToken() {
  return accessToken
}

export function getRefreshToken() {
  return refreshToken
}

export function setToken(access, refresh) {
  accessToken = access
  if (refresh !== undefined) {
    refreshToken = refresh
    if (refresh) {
      sessionStorage.setItem(REFRESH_TOKEN_KEY, refresh)
    } else {
      sessionStorage.removeItem(REFRESH_TOKEN_KEY)
    }
  }
  if (access) {
    sessionStorage.setItem(TOKEN_KEY, access)
  } else {
    sessionStorage.removeItem(TOKEN_KEY)
  }
  if (channel) {
    channel.postMessage({ type: 'TOKEN_UPDATE', token: access })
  }
}

export function clearToken() {
  accessToken = null
  refreshToken = null
  sessionStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(REFRESH_TOKEN_KEY)
  if (channel) {
    channel.postMessage({ type: 'TOKEN_UPDATE', token: null })
  }
}
