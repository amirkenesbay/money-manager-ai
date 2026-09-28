import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'
import { mockApi } from './dev/mockApi.ts'

const API_PATH = '/api'
const DEFAULT_API_TARGET = 'http://localhost:8080'
const MOCK_MODE = 'mock'
const TUNNEL_HOST_SUFFIXES = ['.trycloudflare.com', '.ngrok-free.app', '.ngrok-free.dev']

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const mocked = mode === MOCK_MODE
  return {
    plugins: mocked ? [react(), mockApi()] : [react()],
    server: {
      allowedHosts: TUNNEL_HOST_SUFFIXES,
      proxy: mocked ? undefined : { [API_PATH]: env.API_PROXY_TARGET ?? DEFAULT_API_TARGET },
    },
  }
})
