import http from 'k6/http';
import { check, group, sleep } from 'k6';
import exec from 'k6/execution';

export const options = {
  scenarios: {
    weekend_guest_matrix: {
      executor: 'shared-iterations',
      vus: 9,
      iterations: 9,
      maxDuration: '3m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<2500'],
    checks: ['rate>0.98'],
  },
};

const baseUrl = __ENV.K6_BASE_URL || 'http://localhost:8080';

const guests = [
  {
    chatId: 900001001,
    firstName: 'Анна',
    username: 'weekend_anna',
    intent: 'Хочу забронировать столик завтра на 20:00 на двоих',
    followUp: 'Стол 12, пожалуйста',
  },
  {
    chatId: 900001002,
    firstName: 'Илья',
    username: 'weekend_ilya',
    intent: 'Покажи меню бара',
    followUp: 'Что посоветуешь из коктейлей?',
  },
  {
    chatId: 900001003,
    firstName: 'Мария',
    username: 'weekend_maria',
    intent: 'Какая афиша на выходные?',
    followUp: 'Хочу забронировать место на событие',
  },
  {
    chatId: 900001004,
    firstName: 'Сергей',
    username: 'weekend_sergey',
    intent: 'Хочу поддержать благотворительность',
    followUp: 'Как купить картину и помочь?',
  },
  {
    chatId: 900001005,
    firstName: 'Дарья',
    username: 'weekend_darya',
    intent: 'Как участвовать в аукционе картин?',
    followUp: 'Какие есть лоты?',
  },
  {
    chatId: 900001006,
    firstName: 'Павел',
    username: 'weekend_pavel',
    intent: 'Расскажи про картины в зале',
    followUp: 'Можно подробнее про автора?',
  },
  {
    chatId: 900001007,
    firstName: 'Ольга',
    username: 'weekend_olga',
    intent: 'Позови менеджера',
    followUp: 'Мне нужен человек по вопросу банкета',
  },
  {
    chatId: 900001008,
    firstName: 'Егор',
    username: 'weekend_egor',
    intent: 'Привет',
    followUp: '',
    payload: { mediaKind: 'VOICE', transcription: 'Хочу столик сегодня вечером' },
  },
  {
    chatId: 900001009,
    firstName: 'Наталья',
    username: 'weekend_natalia',
    intent: 'Не понимаю что выбрать, помогите',
    followUp: 'Хочу красиво провести вечер',
  },
];

export default function () {
  const guest = guests[exec.scenario.iterationInTest % guests.length];
  group(`guest ${guest.chatId} ${guest.username}`, () => {
    sendMessage(guest, 'Привет', null, 'first touch');
    sleep(0.2);

    sendMessage(guest, 'Делюсь контактом', `+7999${guest.chatId}`, 'contact share');
    sleep(0.2);

    sendMessage(guest, guest.intent, null, 'scenario intent', guest.payload);
    sleep(0.2);

    if (guest.followUp) {
      sendMessage(guest, guest.followUp, null, 'scenario follow-up');
    }
  });
}

function sendMessage(guest, text, contactPhone, label, payload = undefined) {
  const body = {
    channel: 'TELEGRAM',
    externalUserId: String(guest.chatId),
    chatId: guest.chatId,
    text,
    contactPhone,
    firstName: guest.firstName,
    username: guest.username,
    correlationId: `${guest.chatId}-${label.replaceAll(' ', '-')}-${Date.now()}`,
    payload: payload || {},
  };

  const response = http.post(`${baseUrl}/api/messages`, JSON.stringify(body), {
    headers: { 'Content-Type': 'application/json' },
    tags: { step: label },
  });

  check(response, {
    [`${label}: status is 200`]: (res) => res.status === 200,
    [`${label}: has FSM state`]: (res) => {
      const parsed = parseJson(res);
      return Boolean(parsed && parsed.nextState);
    },
    [`${label}: has guest-facing text`]: (res) => {
      const parsed = parseJson(res);
      return Boolean(parsed && parsed.text && parsed.text.length > 0);
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
