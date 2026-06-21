import { NextRequest, NextResponse } from "next/server";

type Message = { from: "bot" | "user"; text: string };

const QUESTIONS = [
  "Понял! Какая главная цель этого материала — что должен почувствовать или сделать зритель после просмотра?",
  "Какие услуги нужны: только съёмка, съёмка + монтаж, или полный продакшн под ключ?",
  "Формат: вертикальный рилс или горизонтальное видео? Примерный хронометраж готового материала?",
  "Когда нужен готовый материал? Укажите дедлайн.",
  "Есть ориентир по бюджету? Любая вилка поможет подобрать правильный состав команды.",
  "Отлично! Как с вами связаться? Укажите имя и телеграм или телефон.",
];

const LABELS = ["Проект", "Цель", "Услуги", "Формат", "Дедлайн", "Бюджет", "Контакт"];

function buildKP(userMessages: Message[]): string {
  const lines = userMessages
    .map((m, i) => `• ${LABELS[i] ?? "Доп."}: ${m.text}`)
    .join("\n");

  return (
    `✅ КП сформировано!\n\n${lines}\n\n` +
    `Мы свяжемся с вами в течение 2–3 часов. ` +
    `Пока посмотрите похожие работы в каталоге выше ↑`
  );
}

export async function POST(req: NextRequest) {
  const { messages }: { messages: Message[] } = await req.json();
  const userMessages = messages.filter((m) => m.from === "user");
  const step = userMessages.length;

  const reply =
    step > 0 && step <= QUESTIONS.length
      ? QUESTIONS[step - 1]
      : step > QUESTIONS.length
      ? buildKP(userMessages)
      : QUESTIONS[0];

  return NextResponse.json({ reply });
}
