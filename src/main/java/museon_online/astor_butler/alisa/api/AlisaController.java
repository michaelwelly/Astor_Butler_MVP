//package museon_online.astor_butler.alisa.api;
//
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import museon_online.astor_butler.alisa.AlisaClient;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
///**
// * 🔮 Контроллер для проверки и отладки интеграции с Яндекс LLM.
// * Позволяет:
// *  - проверить соединение;
// *  - отправить промпт;
// *  - завершить сессию (очистка FSM).
// */
//@RestController
//@RequestMapping("/api/alisa")
//@RequiredArgsConstructor
//@Tag(name = "Alisa LLM API", description = "Интеграция с Yandex Cloud LLM (YandexGPT)")
//public class AlisaController {
//
//    private final AlisaClient alisaClient;
//
//    @GetMapping("/ping")
//    @Operation(summary = "Проверка соединения", description = "Возвращает Pong, если сервис жив")
//    public ResponseEntity<String> ping() {
//        return ResponseEntity.ok("pong 🤖");
//    }
//
//    @PostMapping("/prompt")
//    @Operation(summary = "Отправить промпт в Yandex LLM", description = "Отправляет текст и возвращает ответ от Яндекс LLM (Astor Butler mode)")
//    public ResponseEntity<String> sendPrompt(@RequestParam String text) {
//        String reply = alisaClient.ask(text);
//        return ResponseEntity.ok(reply);
//    }
//
//    @PostMapping("/reset")
//    @Operation(summary = "Завершить сессию", description = "Очищает данные FSM/Redis и завершает текущую AI-сессию")
//    public ResponseEntity<String> resetSession() {
//        // TODO: очистить FSMStorage при интеграции с Redis
//        return ResponseEntity.ok("🔄 Сессия завершена. Astor Butler ожидает новых команд.");
//    }
//}