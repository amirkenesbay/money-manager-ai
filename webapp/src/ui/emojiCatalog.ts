export const EMOJI_GROUPS = ['food', 'transport', 'home', 'health', 'shopping', 'fun', 'family', 'money', 'other'] as const

export type EmojiGroup = (typeof EMOJI_GROUPS)[number]

export interface CatalogEmoji {
  emoji: string
  group: EmojiGroup
  words: string[]
}

const entry = (emoji: string, group: EmojiGroup, words: string): CatalogEmoji => ({ emoji, group, words: words.split(' ') })

export const EMOJI_CATALOG: CatalogEmoji[] = [
  entry('🛒', 'food', 'продукт магазин супермаркет еда grocer food market азық дүкен тамақ'),
  entry('🍔', 'food', 'фастфуд бургер еда fast burger food тамақ'),
  entry('🍽️', 'food', 'кафе ресторан обед ужин завтрак еда cafe restaurant lunch dinner meal мейрамхана кафе тамақ'),
  entry('☕', 'food', 'кофе кофейня чай латте капучино coffee tea кофе шай'),
  entry('🍕', 'food', 'пицца доставка pizza delivery'),
  entry('🍣', 'food', 'суши роллы sushi'),
  entry('🥖', 'food', 'хлеб пекарня выпечка bread bakery нан'),
  entry('🍎', 'food', 'фрукт овощ fruit vegetable жеміс көкөніс'),
  entry('🍰', 'food', 'сладкое десерт торт кондитер sweet dessert cake тәтті'),
  entry('🍺', 'food', 'бар пиво алкоголь вино bar beer alcohol wine сыра'),
  entry('🥩', 'food', 'мясо meat ет'),
  entry('🚗', 'transport', 'машина авто автомобиль car auto көлік машина'),
  entry('🚕', 'transport', 'такси яндекс uber bolt taxi такси'),
  entry('⛽', 'transport', 'бензин заправка топливо газ fuel gas petrol жанармай бензин'),
  entry('🚌', 'transport', 'автобус транспорт проезд bus transport автобус'),
  entry('🚇', 'transport', 'метро metro subway метро'),
  entry('🅿️', 'transport', 'парковка стоянка parking тұрақ'),
  entry('🔧', 'transport', 'ремонт сервис сто шиномонтаж repair service жөндеу'),
  entry('✈️', 'transport', 'самолёт самолет перелёт авиабилет путешеств отпуск flight plane travel trip ұшақ саяхат демалыс'),
  entry('🚆', 'transport', 'поезд жд train пойыз'),
  entry('🚲', 'transport', 'велосипед самокат bike scooter велосипед'),
  entry('🏠', 'home', 'дом квартира аренда жильё жилье ипотека home house rent flat mortgage үй пәтер жалға'),
  entry('💡', 'home', 'коммуналка свет электричество utilities electricity коммунал жарық'),
  entry('💧', 'home', 'вода water су'),
  entry('🔥', 'home', 'отопление газ heating жылу'),
  entry('🌐', 'home', 'интернет wifi internet интернет'),
  entry('📱', 'home', 'связь телефон мобильн сотовый phone mobile телефон байланыс'),
  entry('🛋️', 'home', 'мебель интерьер furniture жиһаз'),
  entry('🧹', 'home', 'уборка химия бытов клининг cleaning тазалық'),
  entry('🛠️', 'home', 'ремонт инструмент стройка repair tools жөндеу'),
  entry('💊', 'health', 'аптека лекарств здоровье pharmacy medicine health дәрі денсаулық'),
  entry('🏥', 'health', 'врач больница клиника анализ doctor hospital clinic дәрігер аурухана'),
  entry('🦷', 'health', 'стоматолог зуб dentist teeth тіс'),
  entry('🏋️', 'health', 'спорт зал фитнес тренировк gym fitness sport спорт'),
  entry('🧘', 'health', 'йога массаж yoga massage йога'),
  entry('💇', 'health', 'красота стрижка парикмахер салон маникюр beauty haircut barber salon сұлулық шаштараз'),
  entry('💄', 'health', 'косметика макияж cosmetics makeup косметика'),
  entry('👕', 'shopping', 'одежда шопинг шоппинг clothes shopping киім'),
  entry('👟', 'shopping', 'обувь кроссовки shoes sneakers аяқ киім'),
  entry('👜', 'shopping', 'сумка аксессуар bag accessories сөмке'),
  entry('💻', 'shopping', 'техника компьютер ноутбук электроника tech computer laptop electronics техника'),
  entry('🎧', 'shopping', 'наушники гаджет headphones gadget құлаққап'),
  entry('📦', 'shopping', 'другое посылка маркетплейс озон kaspi wildberries other parcel marketplace басқа'),
  entry('🎁', 'shopping', 'подарок подарки gift gifts сыйлық'),
  entry('💐', 'shopping', 'цветы flowers гүл'),
  entry('🎬', 'fun', 'кино развлечен фильм movie cinema entertainment кино ойын-сауық'),
  entry('📺', 'fun', 'подписк стриминг netflix youtube spotify subscription streaming жазылым'),
  entry('🎮', 'fun', 'игры игра game games ойын'),
  entry('🎵', 'fun', 'музыка концерт music concert музыка'),
  entry('🎭', 'fun', 'театр шоу theatre theater show театр'),
  entry('🏖️', 'fun', 'отпуск отдых пляж vacation beach holiday демалыс'),
  entry('🏨', 'fun', 'отель гостиница hotel қонақ үй'),
  entry('📚', 'family', 'образование книги учёба учеба курс education books study course білім кітап оқу'),
  entry('🎓', 'family', 'университет учёба учеба универ university tuition университет'),
  entry('🧸', 'family', 'дети ребёнок ребенок игрушк kids child toys бала ойыншық'),
  entry('🏫', 'family', 'школа садик school kindergarten мектеп балабақша'),
  entry('🐾', 'family', 'питомец животн кот собака ветеринар pet cat dog vet жануар мысық ит'),
  entry('👪', 'family', 'семья родител family parents отбасы ата-ана'),
  entry('💰', 'money', 'зарплата доход salary income жалақы табыс'),
  entry('💼', 'money', 'работа подработка фриланс бизнес job freelance business side жұмыс'),
  entry('💵', 'money', 'наличные кэш cash қолма-қол'),
  entry('💳', 'money', 'карта кредит кэшбэк банк card credit cashback bank карта несие'),
  entry('📈', 'money', 'инвестиц акции дивиденд процент invest stocks dividend interest инвестиция'),
  entry('🏦', 'money', 'банк вклад депозит сбережен bank deposit savings банк салым'),
  entry('🤝', 'money', 'долг займ возврат debt loan return қарыз'),
  entry('🧾', 'money', 'налог штраф счёт счет tax fine bill салық айыппұл'),
  entry('🪙', 'money', 'копилка накоплен piggy saving жинақ'),
  entry('🙏', 'other', 'благотворительн пожертвован charity donation садақа қайырымдылық'),
  entry('⛪', 'other', 'религия мечеть церковь religion mosque church мешіт'),
  entry('🚬', 'other', 'сигарет табак вейп cigarettes tobacco vape темекі'),
  entry('🎰', 'other', 'ставки лотерея casino lottery bets лотерея'),
  entry('❓', 'other', 'разное прочее misc unknown әртүрлі'),
]

const MIN_TOKEN_LENGTH = 2
const MIN_STEM_LENGTH = 4
const tokensOf = (text: string): string[] => text.toLocaleLowerCase().split(/[^\p{L}]+/u).filter((token) => token.length >= MIN_TOKEN_LENGTH)

const matches = (item: CatalogEmoji, tokens: string[]): boolean =>
  tokens.some((token) => item.words.some((word) => word.startsWith(token) || (word.length >= MIN_STEM_LENGTH && token.startsWith(word))))

export function searchEmoji(query: string): CatalogEmoji[] {
  const tokens = tokensOf(query)
  return tokens.length === 0 ? EMOJI_CATALOG : EMOJI_CATALOG.filter((item) => matches(item, tokens))
}

export function suggestEmoji(name: string): string | null {
  const tokens = tokensOf(name)
  if (tokens.length === 0) return null
  return EMOJI_CATALOG.find((item) => matches(item, tokens))?.emoji ?? null
}
