"use client";

import { useMemo, useState } from "react";
import { ArrowUpRight, Check, ShieldCheck } from "lucide-react";
import { CURRENT_POLICY } from "@/lib/consent";

const MOCK_YANDEX_PROFILE = {
  name: "Анна Иванова",
  email: "anna@example.ru",
};

export function YandexIdConsentPanel() {
  const [serviceConsent, setServiceConsent] = useState(false);
  const [proposalConsent, setProposalConsent] = useState(false);
  const [profileLoaded, setProfileLoaded] = useState(false);
  const [phone, setPhone] = useState("");
  const [telegram, setTelegram] = useState("");
  const [previewOpen, setPreviewOpen] = useState(false);

  const consentEvidence = useMemo(
    () => ({
      version: CURRENT_POLICY.version,
      source: "c3ag-contact-yandex-id-mock",
      acceptedAt: serviceConsent ? new Date().toISOString() : null,
    }),
    [serviceConsent],
  );

  const yandexFields = profileLoaded ? MOCK_YANDEX_PROFILE : { name: "", email: "" };

  return (
    <div className="identity-panel" aria-label="Макет согласия и Yandex ID">
      <div className="identity-panel-head">
        <ShieldCheck size={18} />
        <div>
          <p>Контакт без лишних данных</p>
          <span>Yandex ID опционален. Можно продолжить без входа.</span>
        </div>
      </div>

      <label className="identity-check">
        <input
          type="checkbox"
          checked={serviceConsent}
          onChange={(e) => setServiceConsent(e.target.checked)}
        />
        <span>
          Я согласен на обработку контактных данных для ответа на заявку и последующего
          уточнения проекта.{" "}
          <a href={CURRENT_POLICY.url} target="_blank" rel="noopener noreferrer">
            Открыть политику
          </a>
        </span>
      </label>

      <div className="identity-actions">
        <button
          type="button"
          className="identity-yandex"
          disabled={!serviceConsent}
          onClick={() => setProfileLoaded(true)}
        >
          Yandex ID <ArrowUpRight size={14} />
        </button>
        <button type="button" onClick={() => setProfileLoaded(false)}>
          Продолжить без входа
        </button>
      </div>

      <p className="identity-note">
        Макет OAuth: реальный client ID, redirect URI и scopes не подключены. При включении
        запрашиваются только поля, разрешённые Yandex ID и выбранными scopes; телефон не
        подтягивается автоматически.
      </p>

      <div className="identity-fields">
        <label>
          <span>Имя из Yandex ID</span>
          <input value={yandexFields.name} readOnly placeholder="появится после Yandex ID" />
        </label>
        <label>
          <span>Email из Yandex ID</span>
          <input value={yandexFields.email} readOnly placeholder="для связи, если разрешено" />
        </label>
        <label>
          <span>Телефон, если нужен звонок</span>
          <input
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            placeholder="+7..."
            inputMode="tel"
          />
        </label>
        <label>
          <span>Telegram для переписки</span>
          <input
            value={telegram}
            onChange={(e) => setTelegram(e.target.value)}
            placeholder="@username или ссылка"
          />
        </label>
      </div>

      <label className="identity-check">
        <input
          type="checkbox"
          checked={proposalConsent}
          onChange={(e) => setProposalConsent(e.target.checked)}
        />
        <span>Email можно использовать для коммерческого предложения по этой заявке.</span>
      </label>

      <button
        type="button"
        className="identity-preview-button"
        disabled={!serviceConsent}
        onClick={() => setPreviewOpen((open) => !open)}
      >
        {previewOpen ? "Скрыть предпросмотр" : "Показать данные перед отправкой"}
      </button>

      {previewOpen && (
        <div className="identity-preview">
          <p>
            <Check size={14} /> Будет создан черновик lead только после финального подтверждения.
          </p>
          <dl>
            <div>
              <dt>Имя</dt>
              <dd>{yandexFields.name || "не указано"}</dd>
            </div>
            <div>
              <dt>Email</dt>
              <dd>{proposalConsent ? yandexFields.email || "не указан" : "не использовать для КП"}</dd>
            </div>
            <div>
              <dt>Телефон</dt>
              <dd>{phone || "не указан"}</dd>
            </div>
            <div>
              <dt>Telegram</dt>
              <dd>{telegram || "не указан"}</dd>
            </div>
            <div>
              <dt>Consent audit</dt>
              <dd>
                {consentEvidence.version} · {consentEvidence.source}
              </dd>
            </div>
          </dl>
        </div>
      )}
    </div>
  );
}
