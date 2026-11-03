export const HERO_SLIDES = ['spent', 'safe', 'balance'] as const

export type HeroSlide = (typeof HERO_SLIDES)[number]

const STORAGE_KEY = 'home-hero-slide'

export function loadHeroSlide(): HeroSlide {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    return HERO_SLIDES.find((slide) => slide === saved) ?? HERO_SLIDES[0]
  } catch {
    return HERO_SLIDES[0]
  }
}

export function saveHeroSlide(slide: HeroSlide): void {
  try {
    localStorage.setItem(STORAGE_KEY, slide)
  } catch {
    return
  }
}
