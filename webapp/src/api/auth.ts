export const AUTH_SCHEME = 'tma'

export const authorizationHeader = (rawInitData: string): string => `${AUTH_SCHEME} ${rawInitData}`
