package museon_online.astor_butler.speech;

import java.nio.file.Path;
import java.util.Map;

public interface SpeechToTextService {

    SpeechToTextResult transcribe(Path audioFile, Map<String, Object> context);
}
