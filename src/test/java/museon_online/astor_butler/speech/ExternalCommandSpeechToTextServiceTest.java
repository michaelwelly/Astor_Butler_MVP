package museon_online.astor_butler.speech;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalCommandSpeechToTextServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void keepsCommandDiagnosticsOutOfTranscript() throws Exception {
        Path script = tempDir.resolve("stt-ok.sh");
        Files.writeString(script, """
                #!/bin/sh
                echo 'onnxruntime cpuid_info warning: Unknown CPU vendor' >&2
                echo 'покажи меню'
                """);
        script.toFile().setExecutable(true);

        ExternalCommandSpeechToTextService service = new ExternalCommandSpeechToTextService();
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "command", script + " {file}");
        ReflectionTestUtils.setField(service, "timeoutSeconds", 5L);

        SpeechToTextResult result = service.transcribe(tempDir.resolve("voice.ogg"), Map.of());

        assertThat(result.transcribed()).isTrue();
        assertThat(result.text()).isEqualTo("покажи меню");
        assertThat(result.text()).doesNotContain("onnxruntime");
        assertThat(result.metadata()).containsEntry("provider", "external-command");
        assertThat(result.metadata().get("stderr").toString()).contains("onnxruntime");
    }
}
