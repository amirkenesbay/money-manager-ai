const ANY_WHITESPACE = /\s/g

export const asRendered = (text: string): string => text.replace(ANY_WHITESPACE, ' ')
