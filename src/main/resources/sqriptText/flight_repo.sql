create table airport
(
    code    char(3) primary key,
    country varchar(128) NOT NULL,
    city    varchar(128) NOT NULL
);

create table aircraft
(
    id    serial primary key,
    model varchar(128)
);

create table seat
(
    aircraft_id int REFERENCES aircraft,
    seat_no     char(4) NOT NULL,
    PRIMARY KEY (aircraft_id, seat_no)
);

create table flight
(
    id                     bigserial primary key,
    flight_no              varchar(16)                       NOT NULL,
    departure_date         timestamp                         NOT NULL,
    departure_airport_code char(3) REFERENCES airport (code) NOT NULL,
    arrival_date           timestamp                         NOT NULL,
    arrival_airport_code   char(3) REFERENCES airport (code) NOT NULL,
    aircraft_id            int REFERENCES aircraft           NOT NULL,
    status                 varchar(32)                       NOT NULL
);

create table ticket
(
    id BIGSERIAL primary key ,
    passport_no varchar(64) NOT NULL ,
    passenger_name varchar(256) NOT NULL ,
    flight_id bigint REFERENCES flight NOT NULL,
    seat_no char(4) NOT NULL,
    cost NUMERIC(8, 2) NOT NULL ,
    UNIQUE (flight_id, seat_no)
);

insert into airport (code, country, city)
values ('MNK', 'Беларусь', 'Минск'),
       ('LDN', 'Англия', 'Лондон'),
       ('MSK', 'Россия', 'Москва'),
       ('BSL', 'Испания', 'Барселона');

insert into aircraft (model)
values ('Боинг 777-300'),
       ('Боинг 737-300'),
       ('Аэробус A320-200'),
       ('Суперджет-100');

insert into seat (aircraft_id, seat_no)
select id, s.column1
from aircraft
         cross join (values ('A1'), ('A2'), ('B1'), ('B2'), ('C1'), ('C2'), ('D1'), ('D2') order by 1) s;

insert into flight (flight_no, departure_date, departure_airport_code, arrival_date, arrival_airport_code, aircraft_id,
                    status)
values
    ('MN3002', '2020-06-14T14:30', 'MNK', '2020-06-14T18:07', 'LDN', 1, 'ARRIVED'),
    ('MN3002', '2020-06-16T09:15', 'LDN', '2020-06-16T13:00', 'MNK', 1, 'ARRIVED'),
    ('BC2801', '2020-07-28T23:25', 'MNK', '2020-07-29T02:43', 'LDN', 2, 'ARRIVED'),
    ('BC2801', '2020-08-01T11:00', 'LDN', '2020-08-01T14:15', 'MNK', 2, 'DEPARTED'),
    ('TR3103', '2020-05-03T13:10', 'MSK', '2020-05-03T18:38', 'BSL', 3, 'ARRIVED'),
    ('TR3103', '2020-05-10T07:15', 'BSL', '2020-05-10T012:44', 'MSK', 3, 'CANCELLED'),
    ('CV9827', '2020-09-09T18:00', 'MNK', '2020-09-09T19:15', 'MSK', 4, 'SCHEDULED'),
    ('CV9827', '2020-09-19T08:55', 'MSK', '2020-09-19T10:05', 'MNK', 4, 'SCHEDULED'),
    ('QS8712', '2020-12-18T03:35', 'MNK', '2020-12-18T06:46', 'LDN', 2, 'ARRIVED');

insert into ticket (passport_no, passenger_name, flight_id, seat_no, cost)
values ('112233', 'Иван Иванов', 1, 'A1', 200),
       ('23234A', 'Петр Петров', 1, 'B1', 180),
       ('SS988D', 'Светлана Светикова', 1, 'B2', 175),
       ('QYASDE', 'Андрей Андреев', 1, 'C2', 175),
       ('POQ234', 'Иван Кожемякин', 1, 'D1', 160),
       ('898123', 'Олег Рубцов', 1, 'A2', 198),
       ('555321', 'Екатерина Петренко', 2, 'A1', 250),
       ('QO23OO', 'Иван Розмаринов', 2, 'B2', 225),
       ('9883IO', 'Иван Кожемякин', 2, 'C1', 217),
       ('123UI2', 'Андрей Буйнов', 2, 'C2', 227),
       ('SS988D', 'Светлана Светикова', 2, 'D2', 277),
       ('EE2344', 'Дмитрий Трусцов', 3, 'А1', 300),
       ('AS23PP', 'Максим Комсомольцев', 3, 'А2', 285),
       ('322349', 'Эдуард Щеглов', 3, 'B1', 99),
       ('DL123S', 'Игорь Беркутов', 3, 'B2', 199),
       ('MVM111', 'Алексей Щербин', 3, 'C1', 299),
       ('ZZZ111', 'Денис Колобков', 3, 'C2', 230),
       ('234444', 'Иван Старовойтов', 3, 'D1', 180),
       ('LLLL12', 'Людмила Старовойтова', 3, 'D2', 224),
       ('RT34TR', 'Степан Дор', 4, 'A1', 129),
       ('999666', 'Анастасия Шепелева', 4, 'A2', 152),
       ('234444', 'Иван Старовойтов', 4, 'B1', 140),
       ('LLLL12', 'Людмила Старовойтова', 4, 'B2', 140),
       ('LLLL12', 'Роман Дронов', 4, 'D2', 109),
       ('112233', 'Иван Иванов', 5, 'С2', 170),
       ('NMNBV2', 'Лариса Тельникова', 5, 'С1', 185),
       ('DSA586', 'Лариса Привольная', 5, 'A1', 204),
       ('DSA583', 'Артур Мирный', 5, 'B1', 189),
       ('DSA581', 'Евгений Кудрявцев', 6, 'A1', 204),
       ('EE2344', 'Дмитрий Трусцов', 6, 'A2', 214),
       ('AS23PP', 'Максим Комсомольцев', 6, 'B2', 176),
       ('112233', 'Иван Иванов', 6, 'B1', 135),
       ('309623', 'Татьяна Крот', 6, 'С1', 155),
       ('319623', 'Юрий Дувинков', 6, 'D1', 125),
       ('322349', 'Эдуард Щеглов', 7, 'A1', 69),
       ('DIOPSL', 'Евгений Безфамильная', 7, 'A2', 58),
       ('DIOPS1', 'Константин Швец', 7, 'D1', 65),
       ('DIOPS2', 'Юлия Швец', 7, 'D2', 65),
       ('1IOPS2', 'Ник Говриленко', 7, 'C2', 73),
       ('999666', 'Анастасия Шепелева', 7, 'B1', 66),
       ('23234A', 'Петр Петров', 7, 'C1', 80),
       ('QYASDE', 'Андрей Андреев', 8, 'A1', 100),
       ('1QAZD2', 'Лариса Потемнкина', 8, 'A2', 89),
       ('5QAZD2', 'Карл Хмелев', 8, 'B2', 79),
       ('2QAZD2', 'Жанна Хмелева', 8, 'С2', 77),
       ('BMXND1', 'Светлана Хмурая', 8, 'В2', 94),
       ('BMXND2', 'Кирилл Сарычев', 8, 'D1', 81),
       ('SS988D', 'Светлана Светикова', 9, 'A2', 222),
       ('SS978D', 'Андрей Желудь', 9, 'A1', 198),
       ('SS968D', 'Дмитрий Воснецов', 9, 'B1', 243),
       ('SS958D', 'Максим Гребцов', 9, 'С1', 251),
       ('112233', 'Иван Иванов', 9, 'С2', 135),
       ('NMNBV2', 'Лариса Тельникова', 9, 'B2', 217),
       ('23234A', 'Петр Петров', 9, 'D1', 189),
       ('123951', 'Полина Зверева', 9, 'D2', 234);


'Кто летел рейсом Минск (MNK) → Лондон (LDN) 2020-07-28 место B1'

select t.passport_no, t.passenger_name
from ticket t
         join flight f
              on t.flight_id = f.id
where f.departure_airport_code = 'MNK'
  and f.arrival_airport_code = 'LDN'
  and date (f.departure_date) = date '2020-07-28'
  and t.seat_no = 'B1';

'Какие 2 перелёта были самыми длительными за всё время'

select f.id, f.flight_no, (f.departure_date - f.arrival_date) as d
from flight f
order by d asc
limit 2;

'Какая максимальная и минимальная продолжительность перелётов
между Минском и Лондоном и сколько всего было таких перелётов'

select f.id, f.flight_no, f.departure_airport_code, f.arrival_airport_code,  (f.departure_date - f.arrival_date) as d
from flight f
where (f.departure_airport_code = 'MNK' and f.arrival_airport_code = 'LDN')
   or
    (f.departure_airport_code = 'LDN' and f.arrival_airport_code = 'MNK');

select min (f.departure_date - f.arrival_date) as d_min,
       max (f.departure_date - f.arrival_date) as max,
       count(*) as f_count

from flight f
where (f.departure_airport_code = 'MNK' and f.arrival_airport_code = 'LDN')
   or
    (f.departure_airport_code = 'LDN' and f.arrival_airport_code = 'MNK');

'Сколько мест осталось незанятыми 2020-06-14 рейс MN3002'

select count(*) as free_seats
from seat s
         join flight f
              on s.aircraft_id = f.aircraft_id
         left join ticket t
                   on f.id = t.flight_id
                       and t.seat_no = s.seat_no
where f.flight_no = 'MN3002'
  and date (f.departure_date) = date ('2020-06-14')
  and t.id is null;

'Какие имена пассажиров встречаются чаще всего
и какую долю от числа всех пассажиров они составляют'

select t.passenger_name, count(*) cnt
from ticket t
group by t.passenger_name
order by cnt desc;

select t.passenger_name,
       count(*) as tickets_count,
       round(count(*) /sum(count(*)) over (), 4) as share
from ticket t
group by t.passenger_name
order by tickets_count desc;

'Вывести имена пассажиров
и сколько билетов каждый пассажир купил за всё время'

select passenger_name, count(*) as tickets_count
from ticket
group by passenger_name
order by tickets_count desc;

'Вывести стоимость всех маршрутов по убыванию
и для каждой строки показать разницу со следующим рейсом'

select cost,
       lead(cost) over (order by cost desc) as next_cost,
       cost - lead(cost) over (order by cost desc) as diff_with_next
from ticket
order by cost desc;