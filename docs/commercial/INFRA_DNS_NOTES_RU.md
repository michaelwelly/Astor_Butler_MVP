# VCG / DNS / hosting notes

## Статус

Рабочая заметка. Термин `VCG` нужно уточнить перед техническим решением: это может быть VPS/VDS, cloud gateway, игровой контур или отдельный branded hosting layer.

## Идея

Astor Butler можно размещать не как “серый backend”, а как часть публичной digital-среды бренда:

- ресторанная витрина;
- игровые/ивент-механики;
- промо-страницы;
- ролики;
- lead capture;
- Telegram/web-chat entry.

Если проект будет идти рядом с игровой аудиторией, например с механикой в духе Mobile Legends / community events, DNS и hosting лучше проектировать как единый branded контур, а не набор случайных поддоменов.

## Гипотеза доменной схемы

```text
astor.<brand-domain>
api.<brand-domain>
bot.<brand-domain>
media.<brand-domain>
events.<brand-domain>
play.<brand-domain>
```

Для AERIS:

```text
astor.aeris.bar
api.aeris.bar
media.aeris.bar
events.aeris.bar
```

## Что надо решить

1. Где живет production DNS.
2. Кто владеет доменом: ресторан, управляющая компания или продуктовая команда.
3. Где размещается backend: Selectel, Yandex Cloud, другой VPS/VDS.
4. Где размещается media/object storage.
5. Нужно ли разделять restaurant runtime и promo/game runtime.
6. Кто оплачивает инфраструктуру и как она перевыставляется клиенту.

## Рекомендация

Для первого AERIS-пилота не усложнять:

- backend на удаленной машине Selectel;
- DNS под контролем владельца домена;
- отдельные поддомены для API/media/promo only if needed;
- расходы на машину и DNS перевыставлять по договору.

Игровой контур и Mobile Legends-style размещение оставить как отдельный creative/partnership track после первого ресторанного кейса.
