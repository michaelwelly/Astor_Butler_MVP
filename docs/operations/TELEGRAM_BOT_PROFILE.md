# Telegram Bot Profile Copy

Эти тексты задаются через BotFather для AERIS-бота.

## Name

```text
Astor Butler by AERIS
```

## About

```text
Цифровой дворецкий AERIS: бронь столов, меню и винная карта, афиша, видео-тур, сабраж и быстрый контакт с командой.
```

## Description

```text
Astor Butler помогает гостям AERIS спокойно пройти путь от первого вопроса до подтвержденной заявки.

Можно выбрать действие кнопкой, написать своими словами или отправить голосовое: забронировать стол, посмотреть меню и винную карту, узнать афишу, попросить видео-тур, спросить про сабраж или позвать команду.

Адрес: Екатеринбург, Мамина-Сибиряка, 58.
```

## Short Description

```text
Цифровой дворецкий AERIS: бронь, меню, афиша, видео-тур, сабраж и связь с командой.
```

## BotFather Commands

```text
/setname
/setabouttext
/setdescription
```

После изменения profile copy в BotFather не требуется пересборка backend. Если меняется pinned preview внутри чата, обновляется `TelegramRouter.previewText()` и `TELEGRAM_UI_PREVIEW_VERSION`.
