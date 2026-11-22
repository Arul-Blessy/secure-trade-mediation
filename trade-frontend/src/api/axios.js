import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "http://localhost:8080",
});

// Attach the JWT token to every request automatically.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// If the token is rejected (expired or invalid), log the user out.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const url = error.config?.url || "";
    const isAuthCall = url.includes("/api/auth/login") || url.includes("/api/auth/register");
    if (error.response?.status === 401 && !isAuthCall) {
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);

// Turns any error into a message we can show on screen.
export function errorMessage(error) {
  if (error.response?.data?.message) {
    return error.response.data.message;
  }
  if (error.response) {
    return `Request failed (${error.response.status})`;
  }
  return "Cannot reach the server. Is the backend running?";
}

export default api;