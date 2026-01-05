create table authors (
    id bigserial primary key,
    first_name varchar(100) not null,
    last_name varchar(100) not null
);

create table books (
    id bigserial primary key,
    title varchar(255) not null,
    publish_year int,
    pages int,
    author_id bigint not null,
    constraint fk_books_author foreign key (author_id) references authors(id)

)

select * from authors;
select * from books;

insert into authors (first_name, last_name)
values ('Лев', 'Толстой'),
       ('Фёдор', 'Достоевский'),
       ('Антон', 'Чехов'),
       ('Джордж', 'Оруэлл');

select * from authors;

insert into books (title, publish_year, pages, author_id)
values ('Война и мир', 1869, 1225, 1),
       ('Анна Каренина', 1877, 864, 1),

       ('Преступление и наказание', 1866, 671, 2),
       ('Идиот', 1869, 640, 2),
       ('Братья Карамазовы', 1880, 824, 2),

       ('Вишнёвый сад', 1904, 256, 3),
       ('Чайка', 1896, 240, 3),

       ('1984', 1949, 328, 4),
       ('Скотный двор', 1945, 112, 4),
       ('Да здравствует фикус!', 1936, 256, 4);




SELECT * FROM books;

'Выбрать название книги, год издания, ФИО автора,
отсортировать по году издания по убыванию'

select b.title, b.publish_year, a.first_name, a.last_name
from books b
join authors a
on b.author_id = a.id
order by b.publish_year desc;

'Выбрать название книги, год издания, ФИО автора,
отсортировать по году издания по возврастанию'

select b.title, b.publish_year, a.first_name, a.last_name
from books b
         join authors a
              on b.author_id = a.id
order by b.publish_year asc;

'Вспомним левый джоин'

INSERT INTO authors (first_name, last_name)
VALUES ('Неизвестный', 'Автор');

select a.first_name, a.last_name, b.title, b.publish_year
from authors a
left join books b
on a.id = b.author_id
order by b.publish_year;

'Выбрать книги заданного автора по его имени и фамилии';

select a.first_name, a.last_name, b.title
from books b
join authors a
on a.id = b.author_id
where a.first_name = 'Лев'
and a.last_name = 'Толстой';

'Эта задача на вложенный запрос:'
'Выбрать книги, у которых количество страниц больше среднего количества страниц по всем книгам';

select a.first_name, a.last_name, b.title, b.pages
from books b
join authors a
    on a.id = b.author_id
where b.pages > (select avg(b.pages) from books b);

'Эта задача на вложенный запрос:'
'Выбрать 3 самые старые книги и вывести суммарное количество страниц у этих книг'

select id, title, pages
from books
order by publish_year asc
limit 3;

select sum(pages) as total_pages
from (select id, title, pages
      from books
      order by publish_year asc
      limit 3) as oldest_book;


'Обновить год издания на текущий год для одной самой маленькой книги каждого автора'

'Для каждого автора найти одну книгу с минимальным количеством страниц'

select b.author_id, min(pages) as min_pages
from books b
group by author_id
order by author_id;

'Найти книги, которые этим условиям соответствуют'

select b.id, b.title, b.pages, b.author_id
from books b
join (
    select b.author_id, min(pages) as min_pages
    from books b
    group by author_id
    order by author_id) as m
on b.author_id = m.author_id
and b.pages = m.min_pages;

'Только теперь UPDATE'

update books
set publish_year = extract(year from current_date)
where id in (
    select min(b.id)
    from books b
    join (select b.author_id, min(pages) as min_pages
from books b
group by author_id
order by author_id) as m
    on b.author_id = m.author_id
    and b.pages = m.min_pages
    group by b.author_id
    );

'Проверка'

select a.first_name, a.last_name, b.title, b.publish_year
from books b
join authors a
on b.author_id = a.id
order by b.publish_year;

'Удалить автора, написавшего самую большую книгу'

'Найти самую большую книгу вместе с автором'

DELETE FROM books
WHERE author_id = 1;

DELETE FROM authors
WHERE id = 1;

select *
from authors a
join books b on a.id = b.author_id
order by a.id
