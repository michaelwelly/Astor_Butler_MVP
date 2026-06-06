import http from 'k6/http';
import { check, group, sleep } from 'k6';
import exec from 'k6/execution';

export const options = {
  scenarios: {
    weekend_fsm_baseline: {
      executor: 'shared-iterations',
      vus: Number(__ENV.K6_VUS || 9),
      iterations: Number(__ENV.K6_ITERATIONS || 9),
      maxDuration: '5m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<3000'],
    checks: ['rate>0.99'],
  },
};

const baseUrl = __ENV.K6_BASE_URL || 'http://localhost:8080';
const chatIdBase = Number(__ENV.K6_CHAT_ID_BASE || 910100000);
const stepSleep = Number(__ENV.K6_STEP_SLEEP || 0.2);

const scenarios = [
  {
    firstName: 'Анна',
    username: 'load_anna',
    intent: 'Хочу забронировать столик завтра на 20:00 на двоих',
    expectedState: 'TABLE_BOOKING_WAIT_TABLE_SELECTION',
  },
  {
    firstName: 'Илья',
    username: 'load_ilya',
    intent: 'Покажи меню бара',
    expectedState: 'READY_FOR_DIALOG',
  },
  {
    firstName: 'Мария',
    username: 'load_maria',
    intent: 'Какая афиша на выходные?',
    expectedState: 'READY_FOR_DIALOG',
  },
  {
    firstName: 'Сергей',
    username: 'load_sergey',
    intent: 'Хочу поддержать благотворительность',
    expectedState: 'DONATION_COLLECT_AMOUNT',
  },
  {
    firstName: 'Дарья',
    username: 'load_darya',
    intent: 'Как участвовать в аукционе картин?',
    expectedState: 'AUCTION_WAIT_BID',
  },
  {
    firstName: 'Ольга',
    username: 'load_olga',
    intent: 'Позови менеджера',
    expectedState: 'READY_FOR_DIALOG',
  },
  {
    firstName: 'Егор',
    username: 'load_egor',
    intent: 'Хочу оставить чаевые 1000 рублей',
    expectedState: 'TIP_CONFIRMATION',
  },
  {
    firstName: 'Наталья',
    username: 'load_natalia',
    intent: 'Покажи impact и сколько собрали',
    expectedState: 'READY_FOR_DIALOG',
  },
  {
    firstName: 'Павел',
    username: 'load_pavel',
    intent: 'Нужен банкет на день рождения',
    expectedState: 'READY_FOR_DIALOG',
  },
];

export default function () {
  const iteration = exec.scenario.iterationInTest;
  const scenario = scenarios[iteration % scenarios.length];
  const guest = {
    ...scenario,
    chatId: chatIdBase + iteration,
  };

  group(`baseline guest ${guest.chatId} ${guest.username}`, () => {
    sendMessage(guest, 'Привет', null, 'first touch', 'CONSENT_REQUIRED');
    sleep(stepSleep);

    sendMessage(guest, 'Делюсь контактом', `+7999${guest.chatId}`, 'contact share', 'READY_FOR_DIALOG');
    sleep(stepSleep);

    sendMessage(guest, guest.intent, null, 'scenario intent', guest.expectedState);
  });
}

function sendMessage(guest, text, contactPhone, label, expectedState) {
  const body = {
    channel: 'TELEGRAM',
    externalUserId: String(guest.chatId),
    chatId: guest.chatId,
    text,
    contactPhone,
    firstName: guest.firstName,
    username: guest.username,
    correlationId: `${guest.chatId}-${label.replaceAll(' ', '-')}-${Date.now()}`,
    payload: {},
  };

  const response = http.post(`${baseUrl}/api/messages`, JSON.stringify(body), {
    headers: { 'Content-Type': 'application/json' },
    tags: { step: label },
    timeout: '10s',
  });

  check(response, {
    [`${label}: status is 200`]: (res) => res.status === 200,
    [`${label}: has guest-facing text`]: (res) => {
      const parsed = parseJson(res);
      return Boolean(parsed && parsed.text && parsed.text.length > 0);
    },
    [`${label}: next state is ${expectedState}`]: (res) => {
      const parsed = parseJson(res);
      return Boolean(parsed && parsed.nextState === expectedState);
    },
  });
}

function parseJson(response) {
  try {
    return response.json();
  } catch (e) {
    return null;
  }
}
