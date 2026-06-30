-- Product seating map for AERIS table booking.
-- sort_order is the "подбери сам" preference rating: lower is better.

INSERT INTO venue_tables (
    venue_code,
    table_code,
    display_name,
    zone,
    capacity_min,
    capacity_max,
    combinable_group,
    bookable,
    active,
    plan_page,
    plan_ref,
    sort_order
) VALUES
    ('AERIS', '4', 'Стол 4 · у окна', 'WINDOW', 1, 5, NULL, TRUE, TRUE, 2, 'Window line: tables 1-6', 1),
    ('AERIS', '5', 'Стол 5 · у окна', 'WINDOW', 1, 4, NULL, TRUE, TRUE, 2, 'Window line: tables 1-6', 2),
    ('AERIS', '3', 'Стол 3 · у окна', 'WINDOW', 1, 4, NULL, TRUE, TRUE, 2, 'Window line: tables 1-6', 3),
    ('AERIS', '11', 'Стол 11 · камерная гостиная', 'SOFT_LOUNGE', 1, 6, 'SOFT_LOUNGE_11_12', TRUE, TRUE, 2, 'Soft lounge pair: tables 11/12', 4),
    ('AERIS', '17', 'Стол 17 · центр зала', 'CENTER_STAGE', 1, 4, 'CENTER_STAGE_16_18', TRUE, TRUE, 2, 'Center stage: tables 16/17/18', 5),
    ('AERIS', '16', 'Стол 16 · центр зала', 'CENTER_STAGE', 1, 4, 'CENTER_STAGE_16_18', TRUE, TRUE, 2, 'Center stage: tables 16/17/18', 6),
    ('AERIS', '15', 'Стол VIP 15 · отдельная зона', 'VIP_ZONE', 1, 6, 'VIP_13_14_15', TRUE, TRUE, 2, 'VIP zone: tables 13/14/15', 7),
    ('AERIS', '13', 'Стол VIP 13 · отдельная зона', 'VIP_ZONE', 1, 6, 'VIP_13_14_15', TRUE, TRUE, 2, 'VIP zone: tables 13/14/15', 8),
    ('AERIS', '14', 'Стол VIP 14 · отдельная зона', 'VIP_ZONE', 1, 6, 'VIP_13_14_15', TRUE, TRUE, 2, 'VIP zone: tables 13/14/15', 9),
    ('AERIS', '18', 'Стол 18 · центр зала', 'CENTER_STAGE', 1, 4, 'CENTER_STAGE_16_18', TRUE, TRUE, 2, 'Center stage: tables 16/17/18', 10),
    ('AERIS', '12', 'Стол 12 · камерная гостиная', 'SOFT_LOUNGE', 1, 6, 'SOFT_LOUNGE_11_12', TRUE, TRUE, 2, 'Soft lounge pair: tables 11/12', 11),
    ('AERIS', '6', 'Стол 6 · у окна', 'WINDOW', 1, 4, NULL, TRUE, TRUE, 2, 'Window line: tables 1-6', 12),
    ('AERIS', '2', 'Стол 2 · у окна', 'WINDOW', 1, 5, NULL, TRUE, TRUE, 2, 'Window line: tables 1-6', 13),
    ('AERIS', '1', 'Стол 1 · у окна', 'WINDOW', 1, 4, NULL, TRUE, TRUE, 2, 'Window line: tables 1-6', 14),
    ('AERIS', '10', 'Стол 10 · у бара', 'BAR', 1, 4, NULL, TRUE, TRUE, 2, 'Bar zone: tables 9/10', 15),
    ('AERIS', '9', 'Стол 9 · у бара', 'BAR', 1, 4, NULL, TRUE, TRUE, 2, 'Bar zone: tables 9/10', 16),
    ('AERIS', '7', 'Стол 7 · винная комната', 'WINE_ROOM', 1, 6, 'WINE_ROOM_7_8', TRUE, TRUE, 2, 'Wine room: tables 7/8', 17),
    ('AERIS', '8', 'Стол 8 · винная комната', 'WINE_ROOM', 1, 6, 'WINE_ROOM_7_8', TRUE, TRUE, 2, 'Wine room: tables 7/8', 18),
    ('AERIS', '19', 'Стол 19 · угловой столик', 'CORNER', 1, 2, NULL, TRUE, TRUE, 2, 'Corner table', 19),
    ('AERIS', 'BAR', 'Bar seating · барная посадка', 'BAR', 1, 18, 'BAR_COUNTER', TRUE, TRUE, 2, 'Bar counter seating', 20)
ON CONFLICT (venue_code, table_code) DO UPDATE SET
    display_name = EXCLUDED.display_name,
    zone = EXCLUDED.zone,
    capacity_min = EXCLUDED.capacity_min,
    capacity_max = EXCLUDED.capacity_max,
    combinable_group = EXCLUDED.combinable_group,
    bookable = EXCLUDED.bookable,
    active = EXCLUDED.active,
    plan_page = EXCLUDED.plan_page,
    plan_ref = EXCLUDED.plan_ref,
    sort_order = EXCLUDED.sort_order,
    updated_at = CURRENT_TIMESTAMP;
