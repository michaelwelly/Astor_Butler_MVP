import type { Metadata } from "next";
import { StudioCabinet } from "@/components/studio/StudioCabinet";

export const metadata: Metadata = {
  title: "C3 Studio — кабинет продакшна",
  description: "C3 Studio: demo-кабинет для каталога видео, загрузки рендеров, приёмки и клиентских чатов.",
};

export default function StudioPage() {
  return <StudioCabinet />;
}
