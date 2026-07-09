# Ручной E2E-тест: бронь стола голосом

Дата: 2026-07-08

## Цель

Проверить путь: голос Натальи -> transcript -> FSM -> бронь -> карточка в staff chat -> подтверждение -> изменение гостей -> отмена.

## Перед стартом

Проверить без вывода секретов:

```bash
docker compose ps
```

Для AERIS-профиля нужны настройки:

```text
AERIS_ASTOR_BUTLER_BOT_ENABLED=true
AERIS_ASTOR_BUTLER_BOT_TOKEN или TELEGRAM_BOT_TOKEN_DEV задан
AERIS_ASTOR_BUTLER_STAFF_CHAT_ID или TELEGRAM_HOSTESS_CHAT_ID задан
AERIS_STT_ENABLED=true
ASTOR_STT_COMMAND=python3 /app/stt_faster_whisper.py {file}
```

Staff chat:

- бот добавлен в чат;
- у бота есть право отправлять сообщения;
- id staff chat совпадает с `TELEGRAM_HOSTESS_CHAT_ID`;
- в чате видны inline-кнопки `Да` и `Нет`.

## Запуск стенда

Для полного Telegram/STT-прогона использовать AERIS profile:

```bash
docker compose --profile telegram up -d
```

Проверить health:

```bash
curl -fsS http://localhost:8089/actuator/health
```

Ожидаемый ответ:

```json
{"status":"UP"}
```

## Сценарий 1. Создать бронь

Наталья отправляет голосом:

```text
Хочу забронировать стол завтра в 20:00 на двоих, тихий стол у окна.
```

Ожидаемое поведение:

- бот может отправить план зала, если нужен выбор стола;
- если стол не указан, Наталья отвечает:

```text
Подбери сам.
```

Ожидаемый результат у гостя:

```text
Готово. Заявку #... передал команде AERIS на подтверждение.
```

Ожидаемый результат в staff chat:

- карточка `Новая заявка на бронь стола`;
- номер заказа;
- гость Наталья;
- дата;
- время;
- гостей: 2;
- пожелание: тихий стол у окна;
- исходный запрос;
- кнопки `Да` и `Нет`.

Хостес нажимает `Да`.

Ожидаемый результат у гостя:

```text
Бронь подтверждена
```

## Сценарий 2. Изменить количество гостей

Наталья отправляет:

```text
Изменить гостей
```

или нажимает кнопку `👥 Изменить гостей`.

Если бот показывает активную бронь, выбрать действие изменения гостей.

Наталья отправляет:

```text
Нас будет четверо.
```

Ожидаемый результат:

- бот отвечает, что обновил бронь и отправил команде на повторное подтверждение;
- в staff chat приходит новая карточка подтверждения;
- в карточке указано `Гостей: 4`;
- хостес снова нажимает `Да`;
- гость получает обновленное подтверждение.

## Сценарий 3. Отменить бронь

Наталья отправляет:

```text
Отмени бронь #НОМЕР.
```

Где `#НОМЕР` - id из предыдущей заявки.

Ожидаемый результат у гостя:

```text
Готово. Я отменил бронь стола #... и освободил слот.
```

Ожидаемый результат в staff chat:

- сообщение `Гость отменил бронь стола`;
- заказ;
- гость;
- стол;
- дата;
- время;
- статус `CANCELLED`.

## Если голос не сработал

Проверить:

- включен ли `ASTOR_STT_ENABLED`;
- доступен ли `stt_faster_whisper.py` внутри контейнера;
- есть ли модель faster-whisper в volume `huggingface-cache`;
- хватает ли времени `ASTOR_STT_TIMEOUT_SECONDS`;
- есть ли у контейнера доступ к Telegram file URL.

Быстрый fallback для презентации: отправить те же фразы текстом. Архитектурно это тот же FSM-путь после transport normalization.

## Кодовые точки для показа

```text
TelegramRouter.handle(...)
TelegramVoiceTranscriptionService.enrich(...)
MessageGatewayService.handle(...)
ScenarioRouter.route(...)
GuestInputUnderstandingService.understand(...)
TableBookingScenario.handle(...)
TableBookingDraftMerger.merge(...)
TableReservationService.createReservation(...)
TableReservationNotificationService.notifyHostessApprovalRequest(...)
HostessReservationApprovalService.handleCallback(...)
ChangeCancelScenario.handle(...)
TableReservationService.changeByGuest(...)
TableReservationService.cancelByGuest(...)
```

