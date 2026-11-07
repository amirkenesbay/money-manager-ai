import { useState } from 'react'
import { strings } from '../strings'
import { useHomeScreen } from '../telegram/homeScreen'
import { Button } from '../ui/Button'
import { PhoneIcon } from '../ui/icons'
import { HomeScreenGuide } from './HomeScreenGuide'

export function HomeScreenOffer() {
  const homeScreen = useHomeScreen()
  const [postponed, setPostponed] = useState(false)
  const [guideOpen, setGuideOpen] = useState(false)
  if (homeScreen.state !== 'available' || postponed) return null

  return (
    <section className="home-screen-offer" aria-label={strings.homeScreenOfferTitle}>
      <span className="home-screen-offer-icon" aria-hidden="true"><PhoneIcon /></span>
      <span className="row-main">
        <span className="row-title">{strings.homeScreenOfferTitle}</span>
        <span className="caption">{strings.homeScreenOfferBody}</span>
        <button type="button" className="link-button home-screen-offer-how" onClick={() => setGuideOpen(true)}>{strings.homeScreenHowTo}</button>
      </span>
      <span className="home-screen-offer-actions">
        <Button size="small" onClick={homeScreen.add}>{strings.homeScreenAdd}</Button>
        <button type="button" className="link-button" onClick={() => setPostponed(true)}>{strings.homeScreenLater}</button>
      </span>
      {guideOpen && <HomeScreenGuide onAdd={() => { setGuideOpen(false); homeScreen.add() }} onClose={() => setGuideOpen(false)} />}
    </section>
  )
}
