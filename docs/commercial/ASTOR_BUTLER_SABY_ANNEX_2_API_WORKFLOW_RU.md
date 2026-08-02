# Приложение N 2

к Договору оказания услуг N ___ от «___» __________ 2026 г.

# API-интеграция Saby и workflow подтверждения бронирований

Статус: черновик. В документе разделены проверенные факты о Saby и желаемые возможности интеграции. Конкретные endpoints, payloads, credentials и ограничения не фиксируются до получения официального API-контракта/документации от Saby или представителя.

## 1. Проверенные факты о Saby

По официальным публичным источникам:

- бренд называется Saby; исторически он указывается как Saby, ранее СБИС/sbis;
- Saby Presto - продукт Saby для автоматизации ресторанов, кафе, баров, кофеен и столовых;
- официальная справка Saby Presto описывает варианты Presto Front, Presto Front Offline, Presto и Presto Android;
- официальная страница Saby про API описывает интеграцию внешних программ с Saby через программный интерфейс как общий подход;
- публичная статья Saby Presto упоминает возможность настройки виджета бронирования и схемы зала, но это не является API-контрактом.

Источники:

- `https://saby.ru/`
- `https://saby.ru/presto`
- `https://saby.ru/help/presto`
- `https://saby.ru/help/integration/api`
- `https://saby.ru/articles/presto/kak_uvelichit_pribyl_v_restorane`

## 2. Неподтвержденные части

Не подтверждены и требуют документации/ответа Saby:

- наличие публичного API именно для бронирования столов;
- метод проверки доступности столов/зон;
- метод создания брони;
- метод изменения/отмены брони;
- структура venue/table/zone identifiers;
- поддержка idempotency keys;
- webhook/status callbacks;
- rate limits;
- тестовый sandbox;
- требования к авторизации;
- тарификация API;
- правовая роль при передаче персональных данных гостей.

## 3. Архитектурный принцип

Saby подключается как external reservation provider, а не как часть FSM.

```text
Telegram/Web/Voice
  -> Astor Butler ingress
  -> Consent + Message Journal
  -> FSM / Table Booking Scenario
  -> TableReservationService
  -> ExternalReservationProvider
      -> LocalReservationProvider
      -> SabyReservationProvider
  -> Operator/Hostess confirmation
  -> Guest reply
```

FSM остается источником состояния диалога и локального audit trail. Saby после подключения становится внешним источником статуса доступности/брони, но не должен обходить FSM и согласия.

## 4. Целевой port

Предлагаемый интерфейс:

```text
ExternalReservationProvider
  checkAvailability(request)
  createReservation(command)
  modifyReservation(command)
  cancelReservation(command)
  getReservationStatus(externalId)
  health()
```

До подключения Saby используется local provider на PostgreSQL:

- `venue_tables`;
- `table_reservation_orders`;
- `table_reservation_holds`.

После подключения Saby локальная запись сохраняется как журнал и fallback/cache.

## 5. Маппинг возможностей

| Сценарий | Сейчас в Astor Butler | Нужно от Saby | Граница безопасности |
| --- | --- | --- | --- |
| Проверить доступность | Local availability по holds/orders | read-only availability API | без побочных эффектов |
| Подобрать стол | Локальные зоны/вместимость | справочник столов/зон/статусов | показать кандидата, спросить гостя |
| Создать заявку | Local order + HELD hold + карточка хостес | create reservation API | idempotency key = local order id |
| Подтвердить | Hostess buttons / REST fallback | external confirmation/status policy | подтверждение человеком или согласованный workflow |
| Перенести | Local change request | update reservation API | явное подтверждение гостя |
| Отменить | Local cancel flow | cancel reservation API | idempotent cancel by external id |
| Сверить статус | Local status | status endpoint/webhook | Saby sync не ломает FSM |

## 6. Минимальный payload Astor -> Saby

Точный payload зависит от API-контракта. Предварительный список полей, которые Astor Butler должен уметь передать после согласия гостя:

- local order id;
- idempotency key;
- venue/restaurant id;
- table/zone id или пожелание;
- start/end time;
- party size;
- guest name, если предоставлено;
- guest phone/email/Telegram contact, если предоставлены и разрешены;
- guest comment;
- source channel;
- consent/audit reference.

## 7. Минимальный payload Saby -> Astor

Ожидаемые данные, если API это поддерживает:

- external reservation id;
- external status;
- confirmed table/zone;
- confirmed time window;
- rejection/conflict reason;
- updated/cancelled timestamp;
- raw provider response reference for audit, без сохранения лишних персональных данных.

## 8. Confirmation workflow

Базовый безопасный workflow:

1. Гость сообщает намерение.
2. Astor Butler собирает обязательные поля.
3. Astor Butler показывает гостю краткое резюме.
4. Гость подтверждает отправку заявки.
5. Astor Butler создает local order.
6. Если Saby недоступен или не подключен: заявка идет сотруднику.
7. Если Saby подключен: adapter создает/проверяет бронь с idempotency key.
8. Сотрудник или согласованный Saby workflow подтверждает итог.
9. Astor Butler сообщает гостю финальный статус.

Автоматическое финальное подтверждение без шага 8 допускается только если это явно согласовано договором, API-контрактом и операционным регламентом Заказчика.

## 9. Ошибки и fallback

При ошибке Saby Astor Butler должен:

- сохранить локальную заявку;
- не терять сообщение гостя;
- уведомить оператора;
- сообщить гостю, что заявка передана команде и требует подтверждения;
- не обещать финальную бронь;
- логировать correlation id, provider status и безопасное описание ошибки.

## 10. Требуемые доступы

Перед разработкой Saby adapter нужны:

- официальный API contract или developer documentation;
- тестовый аккаунт/организация;
- test restaurant/venue id;
- способ авторизации;
- sandbox/base URL;
- credentials для runtime secret storage;
- разрешение на обработку персональных данных;
- контакты ответственного представителя Saby/Заказчика;
- limits/tariffs;
- список полей, разрешенных к передаче.

