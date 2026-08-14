export const decodeJWT = (token) => {
  try {
    const payload = token.split(".")[1]
    return JSON.parse(atob(payload))
  } catch {
    return null
  }
}

export const isTokenExpired = (decodedToken) => {
  if (!decodedToken?.exp) return true
  return decodedToken.exp * 1000 < Date.now()
}

// 🔁 Map JWT → frontend user object
export const mapJwtToUser = (decoded) => ({
  id: decoded.userId,
  email: decoded.sub,
  name: decoded.name,
  role: decoded.role,
})
