import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'
import { mockApi } from './dev/mockApi.ts'

const API_PATH = '/api'
const DEFAULT_API_TARGET = 'http://localhost:8080'
const MOCK_MODE = 'mock'
const LAZY_3D_CHUNK_LIMIT_KB = 600
const PRODUCTION_BASE = '/app/'
const DEV_BASE = '/'
const TUNNEL_HOST_SUFFIXES = ['.trycloudflare.com', '.ngrok-free.app', '.ngrok-free.dev']

export default defineConfig(({ mode, command }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const mocked = mode === MOCK_MODE
  return {
    base: command === 'build' ? PRODUCTION_BASE : DEV_BASE,
    plugins: mocked ? [react(), mockApi()] : [react()],
    build: { chunkSizeWarningLimit: LAZY_3D_CHUNK_LIMIT_KB },
    server: {
      allowedHosts: TUNNEL_HOST_SUFFIXES,
      proxy: mocked ? undefined : { [API_PATH]: env.API_PROXY_TARGET ?? DEFAULT_API_TARGET },
    },
  }
})
