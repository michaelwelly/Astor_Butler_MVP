package museon_online.astor_butler.telegram.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.fsm.core.event.InboundEvent;
import museon_online.astor_butler.fsm.core.idempotency.IdempotencyGuard;
import museon_online.astor_butler.domain.booking.HostessReservationApprovalService;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.MessageGatewayService;
import museon_online.astor_butler.service.message.OutgoingMessage;
import museon_online.astor_butler.storage.ObjectStorageService;
import museon_online.astor_butler.telegram.exeption.TelegramExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Audio;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.ResponseParameters;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.Voice;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramRouter {

    private final TelegramExceptionHandler exceptionHandler;
    private final IdempotencyGuard idempotencyGuard;
    private final MessageGatewayService messageGatewayService;
    private final TelegramChatViewService chatViewService;
    private final TelegramVoiceTranscriptionService voiceTranscriptionService;
    private final HostessReservationApprovalService hostessReservationApprovalService;
    private final ResourceLoader resourceLoader;
    private final ObjectStorageService objectStorageService;

    @Value("${telegram.ui.preview-enabled:true}")
    private boolean previewEnabled;

    @Value("${telegram.ui.cleanup-enabled:true}")
    private boolean cleanupEnabled;

    @Value("${telegram.ui.delete-user-messages-enabled:true}")
    private boolean deleteUserMessagesEnabled;

    @Value("${telegram.ui.preview-version:2026-06-05-aeris}")
    private String previewVersion;

    @Value("${telegram.ui.preview-avatar-path:classpath:telegram/aeris-butler-preview.png}")
    private Resource previewAvatar;


    public void handle(Update update, AbsSender sender) {
        try {
            if (!acceptInboundEvent(update)) {
                return;
            }

            if (handleCallback(update, sender)) {
                return;
            }

            IncomingMessage incoming = toIncomingMessage(update);
            if (incoming == null) {
                log.debug("📭 [TG] Update ignored: cannot map to IncomingMessage");
                return;
            }
            incoming = voiceTranscriptionService.enrich(incoming, sender);

            log.info("📨 [TG] Incoming message from {}: {}", incoming.chatId(), incoming.text());
            if (hostessReservationApprovalService.handle(incoming)) {
                return;
            }

            OutgoingMessage outgoing = messageGatewayService.handle(incoming);
            ensurePreview(incoming, sender, isStartCommand(incoming));
            cleanupPreviousExchangeIfSafe(incoming, outgoing, sender);
            sendDocumentIfPresent(outgoing, sender);
            sendVideoIfPresent(outgoing, sender);
            send(incoming, outgoing, sender);
            trackCurrentUserMessage(incoming);
            sendAdminAlert(outgoing, sender);

        } catch (Exception e) {
            log.error("💥 [TG] Exception while handling update: {}", e.getMessage(), e);
            exceptionHandler.handle(update, e, sender);
        }
    }

    private boolean handleCallback(Update update, AbsSender sender) {
        if (update == null || !update.hasCallbackQuery()) {
            return false;
        }

        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage() == null ? null : callbackQuery.getMessage().getChatId();
        HostessReservationApprovalService.CallbackResult result = hostessReservationApprovalService.handleCallback(
                callbackQuery.getData(),
                chatId
        );
        if (!result.handled()) {
            return false;
        }

        execute(sender, AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQuery.getId())
                .text(result.answerText())
                .showAlert(false)
                .build());
        return true;
    }
    private boolean acceptInboundEvent(Update update) {
        try {
            InboundEvent inboundEvent = InboundEvent.from(update);

            if (inboundEvent == null) {
                log.debug("📭 [PIPELINE] Update ignored (cannot be mapped to InboundEvent)");
                return true;
            }

            boolean accepted = idempotencyGuard.accept(inboundEvent);
            if (!accepted) {
                log.info(
                        "🔁 [PIPELINE] Duplicate event ignored (eventId={}, chatId={})",
                        inboundEvent.getEventId(),
                        inboundEvent.getChatId()
                );
                return false;
            }

            log.info(
                    "➡️ [PIPELINE] InboundEvent accepted by idempotency guard → forwarding to MessageGateway (eventId={}, type={}, chatId={})",
                    inboundEvent.getEventId(),
                    inboundEvent.getType(),
                    inboundEvent.getChatId()
            );

            return true;

        } catch (Exception e) {
            log.error("💥 [PIPELINE] Error while processing inbound event", e);
            return true;
        }
    }

    private IncomingMessage toIncomingMessage(Update update) {
        if (update == null || update.getMessage() == null) {
            return null;
        }

        Message message = update.getMessage();
        Long chatId = message.getChatId();
        User user = message.getFrom();
        Contact contact = message.getContact();
        Map<String, Object> payload = mediaPayload(message);

        return IncomingMessage.telegram(
                chatId,
                user == null ? null : user.getId(),
                message.getMessageId(),
                update.getUpdateId(),
                message.hasText() ? message.getText() : "",
                contact == null ? null : contact.getPhoneNumber(),
                user == null ? null : user.getFirstName(),
                user == null ? null : user.getLastName(),
                user == null ? null : user.getUserName(),
                user == null ? null : user.getLanguageCode(),
                user == null ? null : user.getIsBot(),
                update.getUpdateId() == null ? UUID.randomUUID().toString() : update.getUpdateId().toString(),
                payload
        );
    }

    private void send(IncomingMessage incoming, OutgoingMessage outgoing, AbsSender sender) {
        if (outgoing == null || outgoing.chatId() == null || outgoing.text() == null || outgoing.text().isBlank()) {
            return;
        }

        SendMessage.SendMessageBuilder builder = SendMessage.builder()
                .chatId(outgoing.chatId().toString())
                .text(outgoing.text());

        if (outgoing.html()) {
            builder.parseMode("HTML");
        }
        if (outgoing.requestContact()) {
            builder.replyMarkup(contactKeyboard());
        } else if (outgoing.removeKeyboard()) {
            builder.replyMarkup(ReplyKeyboardRemove.builder().removeKeyboard(true).build());
        }

        Message sentMessage = execute(sender, builder.build());
        if (sentMessage != null && shouldTrackAsDisposable(outgoing)) {
            chatViewService.saveLastBotMessageId(incoming.telegramUserId(), sentMessage.getMessageId());
        }
    }

    private void sendAdminAlert(OutgoingMessage outgoing, AbsSender sender) {
        if (outgoing == null
                || outgoing.adminAlert() == null
                || !outgoing.adminAlert().required()
                || outgoing.adminAlert().chatId() == null
                || outgoing.adminAlert().chatId().isBlank()) {
            return;
        }

        execute(sender, SendMessage.builder()
                .chatId(outgoing.adminAlert().chatId())
                .text(outgoing.adminAlert().text())
                .parseMode("HTML")
                .build());
    }

    private void sendDocumentIfPresent(OutgoingMessage outgoing, AbsSender sender) {
        if (outgoing == null || outgoing.chatId() == null || outgoing.metadata() == null) {
            return;
        }
        for (Map<String, Object> document : documentMetadata(outgoing)) {
            sendDocument(outgoing.chatId(), document, sender);
        }

        Object location = outgoing.metadata().get("documentResource");
        if (location == null || location.toString().isBlank()) {
            return;
        }
        sendDocument(outgoing.chatId(), Map.of(
                "resource", location.toString(),
                "filename", text(outgoing.metadata().get("documentFilename"), ""),
                "caption", text(outgoing.metadata().get("documentCaption"), "")
        ), sender);
    }

    private void sendDocument(Long chatId, Map<String, Object> metadata, AbsSender sender) {
        Object location = metadata.get("resource");
        if (chatId == null || location == null || location.toString().isBlank()) {
            return;
        }
        try {
            Resource resource = resourceLoader.getResource(location.toString());
            String filename = text(metadata.get("filename"), resource.getFilename() == null ? "document.pdf" : resource.getFilename());
            try (InputStream inputStream = resource.getInputStream()) {
                execute(sender, SendDocument.builder()
                        .chatId(chatId.toString())
                        .document(new InputFile(inputStream, filename))
                        .caption(text(metadata.get("caption"), ""))
                        .build());
                log.info("Telegram document sent: chatId={}, resource={}", chatId, location);
            }
        } catch (Exception e) {
            log.warn("Telegram document was not sent: {}", e.getMessage());
        }
    }

    private List<Map<String, Object>> documentMetadata(OutgoingMessage outgoing) {
        Object value = outgoing.metadata().get("documents");
        if (!(value instanceof List<?> rawDocuments)) {
            return List.of();
        }
        List<Map<String, Object>> documents = new ArrayList<>();
        for (Object rawDocument : rawDocuments) {
            if (rawDocument instanceof Map<?, ?> rawMap) {
                Map<String, Object> document = new LinkedHashMap<>();
                rawMap.forEach((key, documentValue) -> {
                    if (key != null) {
                        document.put(key.toString(), documentValue);
                    }
                });
                documents.add(document);
            }
        }
        return List.copyOf(documents);
    }

    private void sendVideoIfPresent(OutgoingMessage outgoing, AbsSender sender) {
        if (outgoing == null || outgoing.chatId() == null || outgoing.metadata() == null) {
            return;
        }
        Object objectKey = outgoing.metadata().get("videoObjectKey");
        if (objectKey == null || objectKey.toString().isBlank()) {
            return;
        }
        String filename = text(outgoing.metadata().get("videoFilename"), "video.mp4");
        String caption = text(outgoing.metadata().get("videoCaption"), "");
        if ("DOCUMENT".equalsIgnoreCase(text(outgoing.metadata().get("videoSendMode"), ""))) {
            sendMediaObjectAsDocument(outgoing.chatId(), objectKey.toString(), filename, caption, sender);
            return;
        }
        try (InputStream inputStream = objectStorageService.openMediaObject(objectKey.toString())) {
            execute(sender, SendVideo.builder()
                    .chatId(outgoing.chatId().toString())
                    .video(new InputFile(inputStream, filename))
                    .caption(caption)
                    .build());
            log.info("Telegram video sent: chatId={}, objectKey={}", outgoing.chatId(), objectKey);
        } catch (Exception e) {
            log.warn("Telegram video was not sent: {}", e.getMessage());
        }
    }

    private void sendMediaObjectAsDocument(Long chatId, String objectKey, String filename, String caption, AbsSender sender) {
        if (chatId == null || objectKey == null || objectKey.isBlank()) {
            return;
        }
        try (InputStream inputStream = objectStorageService.openMediaObject(objectKey)) {
            execute(sender, SendDocument.builder()
                    .chatId(chatId.toString())
                    .document(new InputFile(inputStream, filename == null || filename.isBlank() ? "media.bin" : filename))
                    .caption(caption == null ? "" : caption)
                    .build());
            log.info("Telegram media object sent as document: chatId={}, objectKey={}", chatId, objectKey);
        } catch (Exception e) {
            log.warn("Telegram media object document was not sent: {}", e.getMessage());
        }
    }

    private String text(Object value, String fallback) {
        return value == null ? fallback : value.toString();
    }

    private ReplyKeyboardMarkup contactKeyboard() {
        KeyboardButton shareContact = KeyboardButton.builder()
                .text("Согласиться и поделиться контактом")
                .requestContact(true)
                .build();

        return ReplyKeyboardMarkup.builder()
                .keyboard(List.of(new KeyboardRow(List.of(shareContact))))
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();
    }

    private void ensurePreview(IncomingMessage incoming, AbsSender sender, boolean force) {
        if (!previewEnabled
                || incoming == null
                || incoming.chatId() == null
                || incoming.telegramUserId() == null
                || incoming.chatId() < 0
                || (!force && chatViewService.findPreviewMessageId(incoming.telegramUserId(), previewVersion) != null)) {
            return;
        }

        String filename = previewAvatar.getFilename() == null ? "aeris-butler-preview.png" : previewAvatar.getFilename();
        try (InputStream inputStream = previewAvatar.getInputStream()) {
            Message preview = sender.execute(SendPhoto.builder()
                    .chatId(incoming.chatId().toString())
                    .photo(new InputFile(inputStream, filename))
                    .caption(previewText())
                    .parseMode("HTML")
                    .build());

            if (preview != null) {
                chatViewService.savePreviewMessageId(incoming.telegramUserId(), preview.getMessageId(), previewVersion);
                pinPreview(incoming, preview, sender);
            }
        } catch (Exception e) {
            log.warn("Telegram preview was not sent: {}", e.getMessage());
        }
    }

    private void pinPreview(IncomingMessage incoming, Message preview, AbsSender sender) {
        if (incoming == null || preview == null || incoming.chatId() == null || preview.getMessageId() == null) {
            return;
        }
        try {
            execute(sender, PinChatMessage.builder()
                    .chatId(incoming.chatId().toString())
                    .messageId(preview.getMessageId())
                    .disableNotification(true)
                    .build());
        } catch (Exception e) {
            log.debug("Telegram preview pin skipped: {}", e.getMessage());
        }
    }

    private void cleanupPreviousExchangeIfSafe(IncomingMessage incoming, OutgoingMessage outgoing, AbsSender sender) {
        if (!cleanupEnabled
                || incoming == null
                || outgoing == null
                || incoming.telegramUserId() == null
                || incoming.chatId() == null
                || incoming.chatId() < 0
                || outgoing.adminAlert().required()) {
            return;
        }

        deleteMessage(sender, incoming.chatId(), chatViewService.findLastBotMessageId(incoming.telegramUserId()));

        if (deleteUserMessagesEnabled) {
            deleteMessage(sender, incoming.chatId(), chatViewService.findLastUserMessageId(incoming.telegramUserId()));
        }
    }

    private void trackCurrentUserMessage(IncomingMessage incoming) {
        if (!deleteUserMessagesEnabled
                || incoming == null
                || incoming.telegramUserId() == null
                || incoming.telegramMessageId() == null
                || incoming.chatId() == null
                || incoming.chatId() < 0) {
            return;
        }
        chatViewService.saveLastUserMessageId(incoming.telegramUserId(), incoming.telegramMessageId());
    }

    private void deleteMessage(AbsSender sender, Long chatId, Integer messageId) {
        if (chatId == null || messageId == null) {
            return;
        }
        execute(sender, DeleteMessage.builder()
                .chatId(chatId.toString())
                .messageId(messageId)
                .build());
    }

    private boolean shouldTrackAsDisposable(OutgoingMessage outgoing) {
        return cleanupEnabled
                && outgoing != null
                && outgoing.chatId() != null
                && !outgoing.adminAlert().required();
    }

    private boolean isStartCommand(IncomingMessage incoming) {
        return incoming != null && incoming.text() != null && "/start".equalsIgnoreCase(incoming.text().trim());
    }

    private Map<String, Object> mediaPayload(Message message) {
        if (message == null) {
            return Map.of();
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        if (message.hasVoice()) {
            Voice voice = message.getVoice();
            payload.put("mediaKind", "VOICE");
            payload.put("telegramFileId", voice.getFileId());
            payload.put("durationSeconds", voice.getDuration());
            payload.put("mimeType", voice.getMimeType());
        } else if (message.hasAudio()) {
            Audio audio = message.getAudio();
            payload.put("mediaKind", "AUDIO");
            payload.put("telegramFileId", audio.getFileId());
            payload.put("durationSeconds", audio.getDuration());
            payload.put("mimeType", audio.getMimeType());
            payload.put("fileName", audio.getFileName());
        }
        return payload;
    }

    private String previewText() {
        return """
                <b><a href="https://aeris.bar/">AERIS</a> · Astor Butler</b>

                Я на связи: помогу с меню, бронью, видео-туром, событиями и быстрым контактом с командой.

                Можно писать или говорить голосом:
                <i>хочу стол на завтра в 20:00</i>
                <i>покажи меню и винную карту</i>
                <i>расскажи про AERIS</i>

                <a href="https://michaelwelly.github.io/Astor_Butler_MVP/docs/guest-guide.html">Короткая инструкция гостя</a>
                """;
    }

    private <T extends Serializable> T execute(AbsSender sender, BotApiMethod<T> method) {
        try {
            return sender.execute(method);
        } catch (TelegramApiRequestException e) {
            Long migratedChatId = migratedChatId(e);
            if (migratedChatId != null && method instanceof SendMessage sendMessage) {
                log.warn(
                        "Telegram chat migrated to supergroup. Update TELEGRAM_ADMIN_CHAT_ID/TELEGRAM_ANALYTICS_CHAT_ID to {}. Retrying once.",
                        migratedChatId
                );
                try {
                    sendMessage.setChatId(migratedChatId);
                    @SuppressWarnings("unchecked")
                    T result = (T) sender.execute(sendMessage);
                    return result;
                } catch (Exception retryException) {
                    log.error("Telegram API retry after chat migration failed: {}", method.getMethod(), retryException);
                    return null;
                }
            }
            log.error("Telegram API call failed: {}", method.getMethod(), e);
        } catch (Exception e) {
            log.error("Telegram API call failed: {}", method.getMethod(), e);
        }
        return null;
    }

    private Message execute(AbsSender sender, SendDocument method) {
        try {
            return sender.execute(method);
        } catch (Exception e) {
            log.error("Telegram API call failed: {}", method.getMethod(), e);
            return null;
        }
    }

    private Message execute(AbsSender sender, SendVideo method) {
        try {
            return sender.execute(method);
        } catch (Exception e) {
            log.error("Telegram API call failed: {}", method.getMethod(), e);
            return null;
        }
    }

    private Long migratedChatId(TelegramApiRequestException e) {
        ResponseParameters parameters = e.getParameters();
        if (parameters != null && parameters.getMigrateToChatId() != null) {
            return parameters.getMigrateToChatId();
        }

        String apiResponse = e.getApiResponse();
        if (apiResponse == null || apiResponse.isBlank()) {
            return null;
        }

        int marker = apiResponse.indexOf("migrate_to_chat_id");
        if (marker < 0) {
            return null;
        }

        String suffix = apiResponse.substring(marker).replaceAll("[^0-9-]", " ").trim();
        if (suffix.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(suffix.split("\\s+")[0]);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
