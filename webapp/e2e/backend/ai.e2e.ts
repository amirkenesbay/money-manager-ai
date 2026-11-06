import { test } from '@e2e-dev/web'
import { expect } from 'e2e'
import type { AnswerCard, VoiceResult } from '../../src/api/types.ts'
import { Api, categoryNamed, newUserId, openAs, seedGroup } from './session.ts'
import { fakeMicrophone, spokenWav } from './speech.ts'

const today = () => new Date().toISOString().slice(0, 10)

test('voice in the app turns speech into a saved expense', async ({ app, screen, agent, browser }) => {
  const api = new Api(app.baseUrl, newUserId())
  const { group, categories } = await seedGroup(api)
  await browser.addInitScript(fakeMicrophone, spokenWav('Такси три тысячи тенге').toString('base64'))
  await openAs(app, screen, api.userId)
  await agent.act('record a voice message with the microphone button, allowing the microphone if asked, let it record for about five seconds, then stop the recording')
  await agent.assert('the app recognized an expense of 3 000 ₸ for taxi and asks to confirm or save it')
  await agent.act('save the recognized operation')
  await expect.poll(async () => (await api.operations(group.id)).map((operation) => `${operation.categoryId}:${Number(operation.amount)}`)).toEqual([`${categoryNamed(categories, 'Такси').id}:3000`])
})

test('the voice endpoint parses several operations from one phrase', async ({ app }) => {
  const api = new Api(app.baseUrl, newUserId())
  const { group } = await seedGroup(api)
  const response = await api.raw(`/groups/${group.id}/voice`, { method: 'POST', headers: { 'Content-Type': 'audio/wav' }, body: spokenWav('Кофе две тысячи тенге. Продукты восемь тысяч тенге.') })
  expect(response.status).toBe(200)
  const result = await response.json() as VoiceResult
  expect(result.operations.map((operation) => Number(operation.amount)).sort((a, b) => a - b)).toEqual([2000, 8000])
})

test('asking a question by voice answers from real data', async ({ app, screen, agent, browser }) => {
  const api = new Api(app.baseUrl, newUserId())
  const { group, categories } = await seedGroup(api)
  for (const amount of ['2000', '3500']) {
    await api.post(`/groups/${group.id}/operations`, { type: 'EXPENSE', amount, categoryId: categoryNamed(categories, 'Такси').id, operationDate: today(), description: null })
  }
  await browser.addInitScript(fakeMicrophone, spokenWav('Сколько я потратил на такси в этом месяце?').toString('base64'))
  await openAs(app, screen, api.userId)
  await agent.act('record a voice message with the microphone button, allowing the microphone if asked, let it record for about five seconds, then stop the recording')
  await agent.assert('the app answers that 5 500 ₸ was spent on taxi this month')
})

test('the ask endpoint answers a typed question', async ({ app }) => {
  const api = new Api(app.baseUrl, newUserId())
  const { group, categories } = await seedGroup(api)
  await api.post(`/groups/${group.id}/operations`, { type: 'EXPENSE', amount: '12000', categoryId: categoryNamed(categories, 'Продукты').id, operationDate: today(), description: null })
  const card = await api.post<AnswerCard>(`/groups/${group.id}/ask`, { text: 'Сколько ушло на продукты в этом месяце?' })
  expect(card.understood).toBe(true)
  expect(JSON.stringify(card).replace(/\s/g, '')).toMatch(/12000|12 000/)
})
