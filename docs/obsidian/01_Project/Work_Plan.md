# Work Plan

Дата: 2026-06-04

## Цель ближайшего этапа

Собрать Astor Butler MVP как управляемый Telegram/FSM backend для ресторана/event-площадки: первый контакт гостя, Consent Vault, сохранение профиля и сообщений, Kafka event trail, админ-уведомления, далее - сценарии бронирования и Slot Keeper.

## Принцип работы

FSM остается источником истины:

- AI помогает понять текст клиента и извлечь сущности;
- бизнес-логика живет в состояниях, переходах, доменных сервисах и валидаторах;
- Telegram остается транспортом;
- legacy-репозиторий используется как источник голоса, тона и поведенческого стиля, но не как источник бизнес-правил.

## Фаза 0. Гигиена репозитория

Статус: продолжается.

Сделано:

- вычищены `.env` и `target/**` из локального коммита перед push;
- добавлен `.gitignore`;
- `main` синхронизирован с `origin/main` на baseline `268662c Add consent vault events and local gateway stack`;
- локальная Maven-проверка на JDK 21 проходила успешно на предыдущих этапах;
- удаляются неактуальные sandbox-упражнения и локальные `.codex*` артефакты.

Осталось:

- не трогать локальную `.idea/dataSources.xml`, пока не ясно, нужна ли она в проекте;
- в будущем добавить GitHub Actions для Maven build/test.
- после удаления sandbox проверить сборку и сделать отдельный cleanup commit.

## Фаза 1. Документы от Яны

Цель: получить реальные материалы, чтобы FSM не был фантазией.

Нужно получить:

- банкетные меню, фуршеты, пакеты, доп. услуги;
- прайсы, депозиты, предоплаты, отмены, переносы;
- коммерческие предложения;
- договоры, приложения, счета, акты, чек-листы;
- текущий путь клиента от первого сообщения до подтверждения;
- типовые вопросы и ответы менеджеров;
- обезличенные переписки;
- обязательные поля для бронирования;
- документы и требования по подрядчикам;
- ограничения площадки;
- критерии "готовой брони".

Результат фазы:

- `02_Product/Event_Booking_Process.md`
- `02_Product/Required_Documents.md`
- `02_Product/Booking_Data_Model.md`

## Фаза 2. Product/FSM spec

Цель: превратить процесс бронирования в явный FSM-сценарий.

Сценарий `EVENT_BOOKING`:

- приветствие и определение намерения;
- тип мероприятия;
- дата и время;
- количество гостей;
- формат;
- бюджет;
- меню и напитки;
- технические требования;
- подрядчики;
- контактные данные;
- подтверждение заявки;
- передача менеджеру;
- fallback и эскалация на живого менеджера.

Результат фазы:

- список состояний;
- список событий;
- обязательные поля на каждом шаге;
- валидаторы;
- правила возврата к предыдущему шагу;
- правила эскалации;
- структура итоговой заявки.

## Фаза 3. Доменные модели MVP

Цель: добавить минимальный домен event booking без смешивания с Telegram.

Предварительные сущности:

- `GuestProfile`
- `Venue`
- `EventBooking`
- `EventDetails`
- `BookingContact`
- `BookingDocument`
- `BookingStatus`

PostgreSQL:

- пользователи;
- бронирования;
- статусы;
- структурированные поля заявки;
- связи между гостем, менеджером и заявкой.

MongoDB:

- файлы;
- документы;
- обезличенные примеры;
- метаданные загруженных материалов.

Redis:

- FSM hot context;
- idempotency;
- быстрый кеш документов/шаблонов;
- временные черновики заявки.

Дополнение 2026-06-05: для обычной посадки столов вводится отдельный `Table Booking` домен, чтобы не смешивать банкетные/event-заявки и конкретную посадку гостя в зале. Первый слой уже описан через `venue_tables`, `table_reservation_orders`, `table_reservation_holds` и документацию `docs/fsm/TABLE_BOOKING.md`.

## Фаза 4. Кодовая реализация

Статус: начато 2026-05-05.

Порядок реализации:

1. Создать пакет `domain/booking`. - done
2. Добавить черновик заявки и Redis-хранилище draft. - done
3. Добавить Liquibase changelog для PostgreSQL. - done
4. Добавить MongoDB dependency/config для файлового слоя.
5. Описать порт хранения документов без жесткой привязки к Telegram.
6. Расширить `BotState` состояниями `EVENT_BOOKING`. - done
7. Добавить handlers для шагов бронирования. - first version done
8. Добавить response builder/templates.
9. Покрыть FSM-переходы unit-тестами. - first tests done
10. Добавить smoke-тест сборки в GitHub Actions.

Что реализовано:

- `EventBookingDraft`
- `EventBookingDraftStorage`
- `RedisEventBookingDraftStorage`
- `EventBooking`
- `BookingStatus`
- `EventBookingRepository`
- `EventBookingService`
- `EventBookingSummaryFormatter`
- `EventBookingManagerNotifier`
- Liquibase changelog `2026-05-05-create-event-bookings.yaml`
- `EventBookingHandler`
- callback-aware `CommandContext`
- callback/contact-aware `InboundEvent`
- `FSMHandler#getStates()` для одного handler на несколько состояний
- вход в сценарий через `/event_booking`, старый `/table_booking` и intent-фразы
- сохранение подтвержденной заявки в PostgreSQL со статусом `READY_FOR_MANAGER`
- сохранение эскалации в PostgreSQL со статусом `MANAGER_REVIEW`
- опциональное уведомление менеджера через `ASTOR_MANAGER_TELEGRAM_CHAT_ID`
- если manager chat id не задан, summary логируется без падения приложения
- unit-тесты `EventBookingHandlerTest`, `EventBookingServiceTest`, `EventBookingSummaryFormatterTest`, `EventBookingManagerNotifierTest`

Проверка:

- `JAVA_HOME=/Users/michaelwelly/Library/Java/JavaVirtualMachines/jbrsdk_jcef-21.0.10/Contents/Home mvn test`
- результат: `BUILD SUCCESS`, 8 tests passed
- локальный старт с Postgres/Redis прошел
- Liquibase создал таблицу `event_bookings`
- `/actuator/health` вернул `{"status":"UP"}`

## Фаза 5. Legacy voice

Цель: взять из `Astor_Butler_Legacy` стиль общения, но не тащить legacy-архитектуру.

Нужно сделать:

- получить/клонировать `Astor_Butler_Legacy`;
- выделить tone-of-voice;
- описать правила общения Astor;
- создать `06_References/Legacy_Voice.md`;
- использовать tone layer в шаблонах ответов.

Правило:

Тон может быть живым, теплым и узнаваемым, но FSM-ответы должны оставаться ясными, проверяемыми и безопасными для клиента ресторана.

## Фаза 6. Notion и рабочая база знаний

Цель: перенести лучшие практики из Notion в локальную проектную память и затем в docs/specs.

Источники:

- `Surviving Java Interviews / Как выжить на Java-собеседовании`
- `New data source`
- `План Защиты Симл(ви)и CV`

Нужно извлечь:

- стек;
- инженерные правила;
- стиль ведения документации;
- практики тестирования;
- практики описания задач.

## Ближайшие 3 шага

1. Закончить cleanup: удалить sandbox/Codex-мусор, проверить git status, сборку и отсутствие `.env`/`target` в tracked files.
2. Довести Table Booking слой: реализовать `TableBookingScenario` поверх описанной FSM-спеки, связать slot extraction с `TableReservationService`, отправлять AERIS plan и создавать hold только после сбора date/time/party/table.
3. После стабилизации первого контакта и посадки перейти к `EVENT_BOOKING`/Slot Keeper и подготовить System Design ДЗ на базе реального API/FSM flow.
