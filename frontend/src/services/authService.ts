import axios, { AxiosError } from 'axios'
import type {
    RegisterRequest,
    LoginRequest,
    RefreshTokenRequest,
    AuthResponse,
    MessageResponse,
    ErrorResponse,
} from '../types/auth'

const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080',
    headers: {
        'Content-Type': 'application/json',
    },
})

api.interceptors.request.use((config) => {
    const token = sessionStorage.getItem('accessToken')
    if (token) {
        config.headers.Authorization = `Bearer ${token}`
    }
    return config
})

api.interceptors.response.use(
    (response) => response,
    async (error: AxiosError<ErrorResponse>) => {
        const original = error.config

        if (error.response?.status === 401 && original) {
            const refreshToken = localStorage.getItem('refreshToken')

            if (refreshToken) {
                try {
                    const { data } = await axios.post<AuthResponse>(
                        `${import.meta.env.VITE_API_URL ?? 'http://localhost:8080'}/v1/auth/refresh`,
                        { refreshToken }
                    )

                    sessionStorage.setItem('accessToken', data.accessToken)
                    localStorage.setItem('refreshToken', data.refreshToken)

                    original.headers.Authorization = `Bearer ${data.accessToken}`
                    return api(original)
                } catch {
                    sessionStorage.removeItem('accessToken')
                    localStorage.removeItem('refreshToken')
                    window.location.href = '/login'
                }
            } else {
                window.location.href = '/login'
            }
        }

        return Promise.reject(error)
    }
)

export const authService = {
    register: async (data: RegisterRequest): Promise<MessageResponse> => {
        const response = await api.post<MessageResponse>('/v1/auth/register', data)
        return response.data
    },

    login: async (data: LoginRequest): Promise<AuthResponse> => {
        const response = await api.post<AuthResponse>('/v1/auth/login', data)
        sessionStorage.setItem('accessToken', response.data.accessToken)
        localStorage.setItem('refreshToken', response.data.refreshToken)
        return response.data
    },

    logout: async (): Promise<MessageResponse> => {
        const response = await api.post<MessageResponse>('/v1/auth/logout')
        sessionStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        return response.data
    },

    refresh: async (data: RefreshTokenRequest): Promise<AuthResponse> => {
        const response = await api.post<AuthResponse>('/v1/auth/refresh', data)
        return response.data
    },
}

export default api