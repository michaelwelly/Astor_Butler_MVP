# Приложение N 4

к Договору оказания услуг N ___ от «___» __________ 2026 г.

# Этапы, результаты и критерии приемки

Статус: черновик. Сроки, стоимость и ответственные лица заполняются после согласования коммерческой модели и доступов.

## 1. Этапы проекта

| Этап | Результат | Зависимости | Приемка |
| --- | --- | --- | --- |
| 1. Discovery | карта процессов ресторана, правила бронирования, доступы, риски | интервью, материалы ресторана | утвержденный scope и open questions |
| 2. Базовый Astor Butler | Telegram/FSM сценарии, local booking, DB, consent journal | VM, Telegram bot, PostgreSQL/Redis | smoke: guest -> order -> operator |
| 3. Operator workflow | карточки заявок, подтверждение/отказ/перенос | ответственные чаты/роли | заявка подтверждается человеком и гость получает статус |
| 4. AI/voice слой | intent, normalization, summaries, optional STT/TTS | ключи, HTTPS, бюджет, политика | тестовые диалоги без ложных обещаний |
| 5. Saby discovery | подтвержденный API contract, auth, sandbox, limits | представитель Saby/Заказчика | documented API map and feasibility |
| 6. Saby adapter | external provider, idempotency, sync status | stage/test credentials | sandbox E2E без production side effects |
| 7. Production rollout | production env, monitoring, rollback | подписанная приемка предыдущих этапов | controlled production smoke |
| 8. Сопровождение | исправления, мониторинг, change requests | оплаченный период | monthly report / agreed support checks |

## 2. Acceptance checks

Минимальная приемка базового контура:

- гость отправляет сообщение;
- сообщение сохраняется в backend;
- FSM выбирает корректный сценарий;
- заявка создается с уникальным ID;
- история диалога сохраняется;
- оператор получает карточку;
- подтверждение/отказ меняет статус;
- гость получает итоговый ответ;
- повторное сообщение не создает дубликат без необходимости;
- при ошибке внешнего сервиса заявка не теряется.

## 3. Acceptance checks для Saby

До production:

- sandbox credentials работают;
- доступность проверяется read-only методом или согласованным equivalent;
- создание заявки идемпотентно;
- внешний ID сохраняется в local order;
- изменение и отмена проходят только после подтверждения;
- provider timeout не ломает FSM;
- circuit breaker/fallback возвращает заявку оператору;
- тесты покрывают success, conflict, timeout, duplicate, cancel.

## 4. Acceptance checks для данных и согласий

- первый контакт показывает понятное consent сообщение;
- policy link доступен;
- consent version/time/source сохраняются;
- phone/email/Telegram contact передаются только если гость их дал;
- маркетинговое/коммерческое согласие отделено от service consent;
- данные в Telegram/operator card минимальны;
- secrets не попадают в git/logs/frontend.

## 5. Production guardrails

Перед включением production:

- домен/HTTPS подтверждены, если используются web/voice;
- Telegram bot credentials настроены server-side;
- Saby credentials настроены server-side;
- rate limiting включен;
- timeouts/circuit breakers включены;
- rollback path описан;
- smoke test согласован;
- массовая нагрузка запрещена без отдельного подтверждения.

## 6. Открытые вопросы

Нужно заполнить:

- точное юридическое имя Заказчика;
- бренд ресторана/площадки;
- список каналов запуска;
- кто отвечает за подтверждение бронирований;
- финальная цена запуска и сопровождения;
- налоговый режим;
- SLA/окна поддержки;
- Saby tariff/API availability;
- Saby test/prod credentials;
- Saby organization/restaurant identifiers;
- final personal data policy;
- согласованные тексты для гостя;
- список материалов ресторана;
- дата production rollout.

