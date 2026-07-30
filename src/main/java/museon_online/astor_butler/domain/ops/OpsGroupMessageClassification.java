package museon_online.astor_butler.domain.ops;

import java.util.List;

public record OpsGroupMessageClassification(
        String scenario,
        String intent,
        String projectCode,
        String resultStatus,
        String summary,
        List<String> actions
) {
    public static OpsGroupMessageClassification of(
            String scenario,
            String intent,
            String projectCode,
            String resultStatus,
            String summary,
            List<String> actions
    ) {
        return new OpsGroupMessageClassification(
                blankDefault(scenario, "OPS_RESULT_MEMORY"),
                blankDefault(intent, "RESULT_MEMORY"),
                blankToNull(projectCode),
                blankToNull(resultStatus),
                blankDefault(summary, "Командное сообщение сохранено в память проекта."),
                actions == null ? List.of() : List.copyOf(actions)
        );
    }

    private static String blankDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
