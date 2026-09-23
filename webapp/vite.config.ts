import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

const API_PATH = '/api'
const DEFAULT_API_TARGET = 'http://localhost:8080'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [react()],
    server: {
      proxy: {
        [API_PATH]: env.API_PROXY_TARGET ?? DEFAULT_API_TARGET,
      },
    },
  }
})
