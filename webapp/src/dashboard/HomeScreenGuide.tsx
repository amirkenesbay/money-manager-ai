import { useState } from 'react'
import { strings } from '../strings'
import { telegramPlatform } from '../telegram/telegramEnv'
import { Button } from '../ui/Button'
import { PhoneIcon } from '../ui/icons'
import { Sheet } from '../ui/Sheet'

const ANDROID_PLATFORM = 'android'
const GUIDE_VIDEOS = {
  ios: `${import.meta.env.BASE_URL}guides/home-screen-ios.mp4`,
  android: `${import.meta.env.BASE_URL}guides/home-screen-android.mp4`,
}

const guideVideo = (): string => (telegramPlatform() === ANDROID_PLATFORM ? GUIDE_VIDEOS.android : GUIDE_VIDEOS.ios)

export function HomeScreenGuide({ onAdd, onClose }: { onAdd: () => void; onClose: () => void }) {
  const [videoMissing, setVideoMissing] = useState(false)
  return (
    <Sheet label={strings.homeScreenGuideTitle} onClose={onClose}>
      <h2 className="sheet-title">{strings.homeScreenGuideTitle}</h2>
      <div className="home-screen-guide-video">
        {videoMissing
          ? <span className="home-screen-guide-placeholder" aria-hidden="true"><PhoneIcon /></span>
          : <video src={guideVideo()} autoPlay muted loop playsInline aria-label={strings.homeScreenGuideTitle} onError={() => setVideoMissing(true)} />}
      </div>
      <ol className="home-screen-guide-steps">
        {strings.homeScreenGuideSteps(strings.homeScreenAdd).map((step) => <li key={step}>{step}</li>)}
      </ol>
      <Button onClick={onAdd}>{strings.homeScreenAdd}</Button>
    </Sheet>
  )
}
