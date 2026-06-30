UPDATE venue_tables
SET display_name = CASE table_code
    WHEN '4' THEN 'Стол 4 · у окна'
    WHEN '5' THEN 'Стол 5 · у окна'
    WHEN '3' THEN 'Стол 3 · у окна'
    WHEN '11' THEN 'Стол 11 · камерная гостиная'
    WHEN '17' THEN 'Стол 17 · центр зала'
    WHEN '16' THEN 'Стол 16 · центр зала'
    WHEN '15' THEN 'Стол VIP 15 · отдельная зона'
    WHEN '13' THEN 'Стол VIP 13 · отдельная зона'
    WHEN '14' THEN 'Стол VIP 14 · отдельная зона'
    WHEN '18' THEN 'Стол 18 · центр зала'
    WHEN '12' THEN 'Стол 12 · камерная гостиная'
    WHEN '6' THEN 'Стол 6 · у окна'
    WHEN '2' THEN 'Стол 2 · у окна'
    WHEN '1' THEN 'Стол 1 · у окна'
    WHEN '10' THEN 'Стол 10 · у бара'
    WHEN '9' THEN 'Стол 9 · у бара'
    WHEN '7' THEN 'Стол 7 · винная комната'
    WHEN '8' THEN 'Стол 8 · винная комната'
    WHEN '19' THEN 'Стол 19 · угловой столик'
    ELSE display_name
END,
updated_at = CURRENT_TIMESTAMP
WHERE venue_code = 'AERIS'
  AND table_code IN ('1','2','3','4','5','6','7','8','9','10','11','12','13','14','15','16','17','18','19');
