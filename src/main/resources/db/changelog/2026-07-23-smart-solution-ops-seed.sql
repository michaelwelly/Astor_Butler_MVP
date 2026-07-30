-- Initial Smart_Soultion.com operational project map.
-- These rows turn the Telegram ops bot into a useful CRM surface immediately after startup.

INSERT INTO ops_projects (
    code, name, vertical, stage, status, owner_name, team_chat_id, progress_percent,
    deadline_at, next_call_at, launch_status, result_definition, description,
    metadata_json, created_at, updated_at
)
VALUES
    (
        'VIDEO',
        'Видео-продакшен',
        'VIDEO_PRODUCTION',
        'PRODUCTION',
        'ACTIVE',
        '@egor',
        NULL,
        45,
        TIMESTAMPTZ '2026-07-30 18:00:00+05',
        TIMESTAMPTZ '2026-07-24 16:00:00+05',
        'Видео-продакшен: у Егора в работе сценарий и съемочный план; нужен статус по монтажу и датам первого превью.',
        'Готовый пакет: согласованный сценарий, съемка, монтаж, финальный ролик и ссылки для команды.',
        'Поток видео-продакшена для Smart_Soultion.com и клиентских запусков.',
        '{"seed":"smart_solution_ops","source":"codex","version":"2026-07-23"}'::jsonb,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'MED',
        'Медицина / презентация',
        'MEDICINE',
        'REVIEW',
        'ACTIVE',
        '@michael',
        NULL,
        55,
        TIMESTAMPTZ '2026-07-29 17:00:00+05',
        TIMESTAMPTZ '2026-07-24 13:00:00+05',
        'Медицинское направление: презентация у Майкла, нужна финальная версия и согласование тезисов.',
        'Готовая презентация: проблема, AI-решение, сроки запуска, ответственные, следующий шаг для клиента.',
        'Медицинская вертикаль Smart Solution с фокусом на презентацию и запуск AI-решения.',
        '{"seed":"smart_solution_ops","source":"codex","version":"2026-07-23"}'::jsonb,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'IZI',
        'IZI / оплата и запуск',
        'AI_PROJECT',
        'LAUNCH',
        'WAITING_CLIENT',
        '@michael',
        NULL,
        70,
        TIMESTAMPTZ '2026-07-26 18:00:00+05',
        TIMESTAMPTZ '2026-07-24 12:00:00+05',
        'Ожидает оплату. После оплаты: финальный smoke test, доступы, подтверждение запуска.',
        'Оплата получена, доступы выданы, запуск подтвержден в командном чате.',
        'Проект IZI стоит в ожидании оплаты перед финальным запуском.',
        '{"seed":"smart_solution_ops","source":"codex","version":"2026-07-23"}'::jsonb,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'RESTO',
        'Рестораны / статусы запусков',
        'HORECA',
        'LAUNCH',
        'ACTIVE',
        '@team',
        NULL,
        60,
        TIMESTAMPTZ '2026-08-01 18:00:00+05',
        TIMESTAMPTZ '2026-07-25 14:00:00+05',
        'Собираем статусы запусков по ресторанам: бот, меню, бронь, быстрый контакт с командой, smoke test в Telegram.',
        'По каждому ресторану есть владелец, срок, запусковый чеклист и понятный статус до готового результата.',
        'Единый поток ресторанных запусков и сервисных сценариев Astor Butler.',
        '{"seed":"smart_solution_ops","source":"codex","version":"2026-07-23"}'::jsonb,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'PRINT',
        'Типография / материалы и сроки',
        'PRINTING',
        'PRODUCTION',
        'WAITING_TEAM',
        '@print_team',
        NULL,
        35,
        TIMESTAMPTZ '2026-07-31 18:00:00+05',
        TIMESTAMPTZ '2026-07-25 11:00:00+05',
        'Типография: фиксируем макеты, сроки печати, ответственного и точку готовности по каждому материалу.',
        'Все макеты проверены, переданы в печать, сроки и ответственные видны команде.',
        'Печатные материалы, макеты и производственные сроки для запусков.',
        '{"seed":"smart_solution_ops","source":"codex","version":"2026-07-23"}'::jsonb,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'SITE',
        'Smart_Soultion.com / сайт и внутренняя CRM',
        'WEBSITE',
        'PRODUCTION',
        'ACTIVE',
        '@michael',
        NULL,
        40,
        TIMESTAMPTZ '2026-08-02 18:00:00+05',
        TIMESTAMPTZ '2026-07-24 18:00:00+05',
        'Собираем сайт и Telegram CRM как единый командный контур: статусы, задачи, презентации, сроки и pipeline.',
        'Команда видит проекты в Telegram, сайт показывает Smart Solution как AI-решение проблем, статусы обновляются без хаоса.',
        'Внутренний сайт и CRM-контур для команды реализаторов всех проектов.',
        '{"seed":"smart_solution_ops","source":"codex","version":"2026-07-23"}'::jsonb,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'ADS',
        'Яндекс Бизнес / Директ / Карты',
        'MARKETING',
        'PLANNING',
        'ACTIVE',
        '@michael',
        NULL,
        20,
        TIMESTAMPTZ '2026-08-05 18:00:00+05',
        TIMESTAMPTZ '2026-07-25 16:00:00+05',
        'Growth/adtech контур: подключаем Яндекс Бизнес, Директ, Карты и приоритетное размещение как официальный внешний трафик.',
        'Готовый рекламный контур: карточка организации, кампании, бюджеты, UTM, отчеты, KPI и честная связка с RAG/AI внутри наших каналов.',
        'Спец-технология Smart Solution: внутренний RAG рекомендует релевантно, внешний трафик идет через официальные рекламные продукты Яндекса.',
        '{"seed":"smart_solution_ops","source":"codex","version":"2026-07-23","adtech":true}'::jsonb,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    )
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    vertical = EXCLUDED.vertical,
    stage = EXCLUDED.stage,
    status = EXCLUDED.status,
    owner_name = EXCLUDED.owner_name,
    progress_percent = EXCLUDED.progress_percent,
    deadline_at = EXCLUDED.deadline_at,
    next_call_at = EXCLUDED.next_call_at,
    launch_status = EXCLUDED.launch_status,
    result_definition = EXCLUDED.result_definition,
    description = EXCLUDED.description,
    metadata_json = ops_projects.metadata_json || EXCLUDED.metadata_json,
    updated_at = CURRENT_TIMESTAMP;

WITH seeded_tasks(code, title, owner_name, status, priority, pipeline_stage, due_at, deliverable_url, notes, metadata_json) AS (
    VALUES
        ('VIDEO', 'Егор: подтвердить текущий статус сценария и съемочного плана', '@egor', 'IN_PROGRESS', 'HIGH', 'BRIEFING', TIMESTAMPTZ '2026-07-24 18:00:00+05', NULL::text, 'Первый командный статус нужен в Telegram.', '{"seed":"smart_solution_ops","source":"codex"}'::jsonb),
        ('VIDEO', 'Собрать пайплайн видео до готового результата', '@egor', 'TODO', 'HIGH', 'PRODUCTION', TIMESTAMPTZ '2026-07-25 18:00:00+05', NULL::text, 'От сценария до финального ролика: этапы, сроки, ответственные.', '{"seed":"smart_solution_ops","source":"codex"}'::jsonb),
        ('MED', 'Майкл: довести медицинскую презентацию до версии для показа', '@michael', 'IN_PROGRESS', 'URGENT', 'REVIEW', TIMESTAMPTZ '2026-07-24 20:00:00+05', 'https://smart-soultion.com/ops/med/presentation', 'Нужна версия, которую можно отправлять и обсуждать с командой.', '{"seed":"smart_solution_ops","source":"codex"}'::jsonb),
        ('MED', 'Собрать блоки: проблема, AI-решение, сроки запуска, ответственные', '@michael', 'TODO', 'HIGH', 'PLANNING', TIMESTAMPTZ '2026-07-25 18:00:00+05', NULL::text, 'Структура презентации для медицинской вертикали.', '{"seed":"smart_solution_ops","source":"codex"}'::jsonb),
        ('IZI', 'Проверить оплату IZI и отметить запуск', '@michael', 'BLOCKED', 'URGENT', 'LAUNCH', TIMESTAMPTZ '2026-07-24 15:00:00+05', NULL::text, 'Блокер: ожидает оплату.', '{"seed":"smart_solution_ops","source":"codex"}'::jsonb),
        ('IZI', 'Подготовить сообщение клиенту после оплаты', '@michael', 'TODO', 'NORMAL', 'LAUNCH', TIMESTAMPTZ '2026-07-25 12:00:00+05', NULL::text, 'Сообщение должно подтверждать оплату, доступы и следующий шаг.', '{"seed":"smart_solution_ops","source":"codex"}'::jsonb),
        ('RESTO', 'Собрать статусы запусков по каждому ресторану', '@team', 'TODO', 'HIGH', 'LAUNCH', TIMESTAMPTZ '2026-07-25 18:00:00+05', NULL::text, 'Нужны статусы: бронь, меню, команда, smoke test.', '{"seed":"smart_solution_ops","source":"codex"}'::jsonb),
        ('RESTO', 'Проверить Telegram-сценарии брони, меню и быстрого контакта', '@michael', 'IN_PROGRESS', 'HIGH', 'LAUNCH', TIMESTAMPTZ '2026-07-26 18:00:00+05', NULL::text, 'Проверить end-to-end до ответа в командный чат.', '{"seed":"smart_solution_ops","source":"codex"}'::jsonb),
        ('PRINT', 'Собрать список макетов и сроков печати', '@print_team', 'IN_PROGRESS', 'HIGH', 'PRODUCTION', TIMESTAMPTZ '2026-07-24 17:00:00+05', NULL::text, 'Нужна таблица: материал, макет, дедлайн, ответственный.', '{"seed":"smart_solution_ops","source":"codex"}'::jsonb),
        ('PRINT', 'Проверить финальные файлы перед передачей в печать', '@michael', 'TODO', 'NORMAL', 'REVIEW', TIMESTAMPTZ '2026-07-28 18:00:00+05', NULL::text, 'Контроль форматов и финальных версий.', '{"seed":"smart_solution_ops","source":"codex"}'::jsonb),
        ('SITE', 'Сверстать первый экран Smart_Soultion.com вокруг AI-решения проблем', '@michael', 'IN_PROGRESS', 'HIGH', 'PRODUCTION', TIMESTAMPTZ '2026-07-26 18:00:00+05', NULL::text, 'Главный экран должен сразу объяснять: решение проблем с помощью AI.', '{"seed":"smart_solution_ops","source":"codex"}'::jsonb),
        ('SITE', 'Собрать dashboard проектов для команды реализаторов', '@team', 'TODO', 'HIGH', 'PRODUCTION', TIMESTAMPTZ '2026-07-29 18:00:00+05', NULL::text, 'Вид: проекты, задачи, коллы, презентации, сроки, пайплайн.', '{"seed":"smart_solution_ops","source":"codex"}'::jsonb),
        ('ADS', 'Подготовить карточку ресторана в Яндекс Бизнес: фото, меню, часы, контакты, бронирование', '@michael', 'TODO', 'HIGH', 'BRIEFING', TIMESTAMPTZ '2026-07-26 18:00:00+05', NULL::text, 'Без полной карточки рекламный трафик будет терять конверсию.', '{"seed":"smart_solution_ops","source":"codex","channel":"yandex_business"}'::jsonb),
        ('ADS', 'Собрать кампании Яндекс Директ: гео, ключи, объявления, UTM, дневной бюджет', '@michael', 'TODO', 'HIGH', 'PLANNING', TIMESTAMPTZ '2026-07-28 18:00:00+05', NULL::text, 'Официальный внешний трафик для ресторанов и Smart Solution.', '{"seed":"smart_solution_ops","source":"codex","channel":"yandex_direct"}'::jsonb),
        ('ADS', 'Описать honest ranking policy для AI-рекомендаций внутри Smart Solution', '@michael', 'IN_PROGRESS', 'NORMAL', 'REVIEW', TIMESTAMPTZ '2026-07-27 18:00:00+05', NULL::text, 'Партнерские рекомендации должны быть релевантными и прозрачными.', '{"seed":"smart_solution_ops","source":"codex","channel":"owned_ai"}'::jsonb)
)
INSERT INTO ops_tasks (
    project_id, title, owner_name, status, priority, pipeline_stage, due_at,
    deliverable_url, notes, metadata_json, created_at, updated_at
)
SELECT p.id, t.title, t.owner_name, t.status, t.priority, t.pipeline_stage, t.due_at,
       t.deliverable_url, t.notes, t.metadata_json, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM seeded_tasks t
JOIN ops_projects p ON p.code = t.code
WHERE NOT EXISTS (
    SELECT 1
    FROM ops_tasks existing
    WHERE existing.project_id = p.id
      AND existing.title = t.title
);

WITH seeded_calls(code, title, starts_at, owner_name, status, notes, metadata_json) AS (
    VALUES
        ('VIDEO', 'Видео-продакшен: статус Егора', TIMESTAMPTZ '2026-07-24 16:00:00+05', '@egor', 'SCHEDULED', 'Понять сценарий, съемку, монтаж и дату первого превью.', '{"seed":"smart_solution_ops","source":"codex"}'::jsonb),
        ('MED', 'Медицина: ревью презентации', TIMESTAMPTZ '2026-07-24 13:00:00+05', '@michael', 'SCHEDULED', 'Проверить структуру презентации и следующий шаг.', '{"seed":"smart_solution_ops","source":"codex"}'::jsonb),
        ('IZI', 'IZI: чек оплаты и запуск', TIMESTAMPTZ '2026-07-24 12:00:00+05', '@michael', 'SCHEDULED', 'Оплата, доступы, финальный smoke test.', '{"seed":"smart_solution_ops","source":"codex"}'::jsonb),
        ('RESTO', 'Рестораны: weekly launch review', TIMESTAMPTZ '2026-07-25 14:00:00+05', '@team', 'SCHEDULED', 'Собрать статусы по ресторанам и блокеры.', '{"seed":"smart_solution_ops","source":"codex"}'::jsonb),
        ('PRINT', 'Типография: макеты и сроки', TIMESTAMPTZ '2026-07-25 11:00:00+05', '@print_team', 'SCHEDULED', 'Проверить список материалов, сроки и ответственных.', '{"seed":"smart_solution_ops","source":"codex"}'::jsonb),
        ('SITE', 'Smart_Soultion.com: сайт и CRM sync', TIMESTAMPTZ '2026-07-24 18:00:00+05', '@michael', 'SCHEDULED', 'Синхронизация сайта, Telegram CRM и командного пайплайна.', '{"seed":"smart_solution_ops","source":"codex"}'::jsonb),
        ('ADS', 'Yandex ads: карта каналов и бюджеты', TIMESTAMPTZ '2026-07-25 16:00:00+05', '@michael', 'SCHEDULED', 'Развести Яндекс Бизнес, Директ, Карты, приоритетное размещение, KPI и отчетность.', '{"seed":"smart_solution_ops","source":"codex"}'::jsonb)
)
INSERT INTO ops_calls (
    project_id, title, starts_at, owner_name, status, notes, metadata_json,
    created_at, updated_at
)
SELECT p.id, c.title, c.starts_at, c.owner_name, c.status, c.notes, c.metadata_json,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM seeded_calls c
JOIN ops_projects p ON p.code = c.code
WHERE NOT EXISTS (
    SELECT 1
    FROM ops_calls existing
    WHERE existing.project_id = p.id
      AND existing.title = c.title
      AND existing.starts_at = c.starts_at
);

WITH seeded_artifacts(code, title, artifact_type, status, owner_name, artifact_url, notes, metadata_json) AS (
    VALUES
        ('VIDEO', 'Бриф видео-продакшена', 'BRIEF', 'DRAFT', '@egor', 'https://smart-soultion.com/ops/video/brief', 'Рабочий бриф и пайплайн до финального ролика.', '{"seed":"smart_solution_ops","source":"codex"}'::jsonb),
        ('MED', 'Медицинская презентация', 'PRESENTATION', 'IN_REVIEW', '@michael', 'https://smart-soultion.com/ops/med/presentation', 'Статус: у Майкла, идет сборка финальной версии.', '{"seed":"smart_solution_ops","source":"codex"}'::jsonb),
        ('IZI', 'Счет / платежные детали IZI', 'CONTRACT', 'SENT', '@michael', 'https://smart-soultion.com/ops/izi/payment', 'Статус: ожидает оплату.', '{"seed":"smart_solution_ops","source":"codex"}'::jsonb),
        ('RESTO', 'Матрица запусков ресторанов', 'REPORT', 'DRAFT', '@team', 'https://smart-soultion.com/ops/resto/launch-matrix', 'Сводная таблица ресторанов, статусов и ответственных.', '{"seed":"smart_solution_ops","source":"codex"}'::jsonb),
        ('PRINT', 'Пакет макетов для типографии', 'DESIGN', 'DRAFT', '@print_team', 'https://smart-soultion.com/ops/print/assets', 'Макеты, форматы, сроки печати.', '{"seed":"smart_solution_ops","source":"codex"}'::jsonb),
        ('SITE', 'Логотип Smart_Soultion.com', 'ASSET', 'APPROVED', '@michael', 'https://smart-soultion.com/brand/logo', 'Выбранный логотип Smart_Soultion.com для AI-решения проблем.', '{"seed":"smart_solution_ops","source":"codex"}'::jsonb),
        ('ADS', 'Yandex Growth Playbook', 'REPORT', 'DRAFT', '@michael', 'https://smart-soultion.com/ops/ads/yandex-growth-playbook', 'Плейбук: AI/RAG внутри owned channels + Яндекс Бизнес/Директ/Карты для внешнего спроса.', '{"seed":"smart_solution_ops","source":"codex"}'::jsonb)
)
INSERT INTO ops_artifacts (
    project_id, title, artifact_type, status, owner_name, artifact_url, notes,
    metadata_json, created_at, updated_at
)
SELECT p.id, a.title, a.artifact_type, a.status, a.owner_name, a.artifact_url, a.notes,
       a.metadata_json, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM seeded_artifacts a
JOIN ops_projects p ON p.code = a.code
WHERE NOT EXISTS (
    SELECT 1
    FROM ops_artifacts existing
    WHERE existing.project_id = p.id
      AND existing.title = a.title
);
