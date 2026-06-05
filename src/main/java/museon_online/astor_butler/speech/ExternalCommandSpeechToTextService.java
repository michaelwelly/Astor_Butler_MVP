package museon_online.astor_butler.speech;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ExternalCommandSpeechToTextService implements SpeechToTextService {

    @Value("${astor.speech-to-text.enabled:false}")
    private boolean enabled;

    @Value("${astor.speech-to-text.command:}")
    private String command;

    @Value("${astor.speech-to-text.timeout-seconds:30}")
    private long timeoutSeconds;

    @Override
    public SpeechToTextResult transcribe(Path audioFile, Map<String, Object> context) {
        if (!enabled) {
            return SpeechToTextResult.unavailable("STT disabled");
        }
        if (command == null || command.isBlank()) {
            return SpeechToTextResult.unavailable("STT command is not configured");
        }
        if (audioFile == null) {
            return SpeechToTextResult.failed("Audio file is missing", context);
        }

        List<String> commandLine = commandLine(audioFile);
        try {
            Process process = new ProcessBuilder(commandLine)
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(Math.max(1, timeoutSeconds), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return SpeechToTextResult.failed("STT command timed out after " + timeoutSeconds + " seconds", Map.of(
                        "command", command,
                        "timeout", Duration.ofSeconds(timeoutSeconds).toString()
                ));
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0) {
                log.warn("STT command failed with exitCode={}, output={}", process.exitValue(), output);
                return SpeechToTextResult.failed("STT command failed with exit code " + process.exitValue(), Map.of(
                        "exitCode", process.exitValue(),
                        "output", output
                ));
            }
            if (output.isBlank()) {
                return SpeechToTextResult.failed("STT command returned blank text", Map.of("command", command));
            }

            return SpeechToTextResult.transcribed(output, Map.of(
                    "provider", "external-command",
                    "command", command
            ));
        } catch (Exception e) {
            log.warn("STT command execution failed: {}", e.getMessage());
            return SpeechToTextResult.failed(e.getClass().getSimpleName() + ": " + e.getMessage(), Map.of("command", command));
        }
    }

    private List<String> commandLine(Path audioFile) {
        String audioPath = audioFile.toAbsolutePath().toString();
        String rendered = command.contains("{file}") ? command.replace("{file}", audioPath) : command + " " + audioPath;
        List<String> result = new ArrayList<>();
        for (String part : rendered.split("\\s+")) {
            if (!part.isBlank()) {
                result.add(part);
            }
        }
        return result;
    }
}
