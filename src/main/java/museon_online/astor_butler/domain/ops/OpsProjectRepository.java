package museon_online.astor_butler.domain.ops;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OpsProjectRepository {

    private final JdbcTemplate jdbcTemplate;

    public OpsProject createProject(OpsProjectCommand command) {
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO ops_projects (
                    code, name, vertical, stage, status, owner_name, team_chat_id, progress_percent,
                    deadline_at, next_call_at, launch_status, result_definition, description,
                    metadata_json, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, COALESCE(?::jsonb, '{}'::jsonb),
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id
                """,
                Long.class,
                normalizeCode(command.code()),
                command.name().trim(),
                command.vertical().name(),
                command.stage().name(),
                command.status().name(),
                blankToNull(command.owner()),
                blankToNull(command.teamChatId()),
                clampProgress(command.progressPercent()),
                timestamp(command.deadlineAt()),
                timestamp(command.nextCallAt()),
                blankToNull(command.launchStatus()),
                blankToNull(command.resultDefinition()),
                blankToNull(command.description()),
                blankToNull(command.metadataJson())
        );
        return findProjectById(id).orElseThrow();
    }

    public Optional<OpsProject> findProjectById(Long id) {
        List<OpsProject> result = jdbcTemplate.query("""
                SELECT *
                FROM ops_projects
                WHERE id = ?
                """, projectMapper(), id);
        return result.stream().findFirst();
    }

    public Optional<OpsProject> findProjectByCode(String code) {
        List<OpsProject> result = jdbcTemplate.query("""
                SELECT *
                FROM ops_projects
                WHERE code = ?
                """, projectMapper(), normalizeCode(code));
        return result.stream().findFirst();
    }

    public List<OpsProject> listProjects(OpsProjectStatus status, OpsProjectVertical vertical, int limit) {
        if (status != null && vertical != null) {
            return jdbcTemplate.query("""
                    SELECT *
                    FROM ops_projects
                    WHERE status = ? AND vertical = ?
                    ORDER BY updated_at DESC, id DESC
                    LIMIT ?
                    """, projectMapper(), status.name(), vertical.name(), limit);
        }
        if (status != null) {
            return jdbcTemplate.query("""
                    SELECT *
                    FROM ops_projects
                    WHERE status = ?
                    ORDER BY updated_at DESC, id DESC
                    LIMIT ?
                    """, projectMapper(), status.name(), limit);
        }
        if (vertical != null) {
            return jdbcTemplate.query("""
                    SELECT *
                    FROM ops_projects
                    WHERE vertical = ?
                    ORDER BY updated_at DESC, id DESC
                    LIMIT ?
                    """, projectMapper(), vertical.name(), limit);
        }
        return jdbcTemplate.query("""
                SELECT *
                FROM ops_projects
                ORDER BY updated_at DESC, id DESC
                LIMIT ?
                """, projectMapper(), limit);
    }

    public OpsProject updateProjectStatus(Long id, OpsProjectStatus status, OpsProjectStage stage, Integer progressPercent, String launchStatus) {
        jdbcTemplate.update("""
                UPDATE ops_projects
                SET status = ?,
                    stage = COALESCE(?, stage),
                    progress_percent = COALESCE(?, progress_percent),
                    launch_status = COALESCE(?, launch_status),
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                status.name(),
                stage == null ? null : stage.name(),
                progressPercent == null ? null : clampProgress(progressPercent),
                blankToNull(launchStatus),
                id
        );
        return findProjectById(id).orElseThrow();
    }

    public OpsProject updateProjectNextCallAt(Long id, Instant nextCallAt) {
        jdbcTemplate.update("""
                UPDATE ops_projects
                SET next_call_at = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                timestamp(nextCallAt),
                id
        );
        return findProjectById(id).orElseThrow();
    }

    public OpsTask createTask(OpsTaskCommand command) {
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO ops_tasks (
                    project_id, title, owner_name, status, priority, pipeline_stage, due_at,
                    deliverable_url, notes, metadata_json, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, COALESCE(?::jsonb, '{}'::jsonb),
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id
                """,
                Long.class,
                command.projectId(),
                command.title().trim(),
                blankToNull(command.owner()),
                command.status().name(),
                command.priority().name(),
                command.pipelineStage().name(),
                timestamp(command.dueAt()),
                blankToNull(command.deliverableUrl()),
                blankToNull(command.notes()),
                blankToNull(command.metadataJson())
        );
        return findTaskById(id).orElseThrow();
    }

    public Optional<OpsTask> findTaskById(Long id) {
        List<OpsTask> result = jdbcTemplate.query("""
                SELECT *
                FROM ops_tasks
                WHERE id = ?
                """, taskMapper(), id);
        return result.stream().findFirst();
    }

    public List<OpsTask> listOpenTasksByProject(Long projectId, int limit) {
        return jdbcTemplate.query("""
                SELECT *
                FROM ops_tasks
                WHERE project_id = ?
                  AND status NOT IN ('DONE', 'CANCELLED')
                ORDER BY due_at NULLS LAST, priority DESC, id DESC
                LIMIT ?
                """, taskMapper(), projectId, limit);
    }

    public OpsTask updateTaskStatus(Long id, OpsTaskStatus status) {
        jdbcTemplate.update("""
                UPDATE ops_tasks
                SET status = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, status.name(), id);
        return findTaskById(id).orElseThrow();
    }

    public OpsCall createCall(OpsCallCommand command) {
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO ops_calls (
                    project_id, title, starts_at, owner_name, status, notes, metadata_json,
                    created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, COALESCE(?::jsonb, '{}'::jsonb),
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id
                """,
                Long.class,
                command.projectId(),
                command.title().trim(),
                timestamp(command.startsAt()),
                blankToNull(command.owner()),
                command.status().name(),
                blankToNull(command.notes()),
                blankToNull(command.metadataJson())
        );
        return findCallById(id).orElseThrow();
    }

    public Optional<OpsCall> findCallById(Long id) {
        List<OpsCall> result = jdbcTemplate.query("""
                SELECT *
                FROM ops_calls
                WHERE id = ?
                """, callMapper(), id);
        return result.stream().findFirst();
    }

    public List<OpsCall> listUpcomingCalls(Long projectId, int limit) {
        if (projectId == null) {
            return jdbcTemplate.query("""
                    SELECT *
                    FROM ops_calls
                    WHERE status = 'SCHEDULED'
                    ORDER BY starts_at NULLS LAST, id DESC
                    LIMIT ?
                    """, callMapper(), limit);
        }
        return jdbcTemplate.query("""
                SELECT *
                FROM ops_calls
                WHERE project_id = ?
                  AND status = 'SCHEDULED'
                ORDER BY starts_at NULLS LAST, id DESC
                LIMIT ?
                """, callMapper(), projectId, limit);
    }

    public OpsArtifact createArtifact(OpsArtifactCommand command) {
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO ops_artifacts (
                    project_id, title, artifact_type, status, owner_name, artifact_url, notes,
                    metadata_json, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, COALESCE(?::jsonb, '{}'::jsonb),
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id
                """,
                Long.class,
                command.projectId(),
                command.title().trim(),
                command.type().name(),
                command.status().name(),
                blankToNull(command.owner()),
                command.url().trim(),
                blankToNull(command.notes()),
                blankToNull(command.metadataJson())
        );
        return findArtifactById(id).orElseThrow();
    }

    public Optional<OpsArtifact> findArtifactById(Long id) {
        List<OpsArtifact> result = jdbcTemplate.query("""
                SELECT *
                FROM ops_artifacts
                WHERE id = ?
                """, artifactMapper(), id);
        return result.stream().findFirst();
    }

    public List<OpsArtifact> listArtifacts(Long projectId, int limit) {
        return jdbcTemplate.query("""
                SELECT *
                FROM ops_artifacts
                WHERE project_id = ?
                  AND status <> 'ARCHIVED'
                ORDER BY updated_at DESC, id DESC
                LIMIT ?
                """, artifactMapper(), projectId, limit);
    }

    private RowMapper<OpsProject> projectMapper() {
        return (rs, rowNum) -> new OpsProject(
                rs.getLong("id"),
                rs.getString("code"),
                rs.getString("name"),
                OpsProjectVertical.valueOf(rs.getString("vertical")),
                OpsProjectStage.valueOf(rs.getString("stage")),
                OpsProjectStatus.valueOf(rs.getString("status")),
                rs.getString("owner_name"),
                rs.getString("team_chat_id"),
                rs.getInt("progress_percent"),
                instant(rs, "deadline_at"),
                instant(rs, "next_call_at"),
                rs.getString("launch_status"),
                rs.getString("result_definition"),
                rs.getString("description"),
                rs.getString("metadata_json"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    private RowMapper<OpsTask> taskMapper() {
        return (rs, rowNum) -> new OpsTask(
                rs.getLong("id"),
                rs.getLong("project_id"),
                rs.getString("title"),
                rs.getString("owner_name"),
                OpsTaskStatus.valueOf(rs.getString("status")),
                OpsTaskPriority.valueOf(rs.getString("priority")),
                OpsProjectStage.valueOf(rs.getString("pipeline_stage")),
                instant(rs, "due_at"),
                rs.getString("deliverable_url"),
                rs.getString("notes"),
                rs.getString("metadata_json"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    private RowMapper<OpsCall> callMapper() {
        return (rs, rowNum) -> new OpsCall(
                rs.getLong("id"),
                rs.getLong("project_id"),
                rs.getString("title"),
                instant(rs, "starts_at"),
                rs.getString("owner_name"),
                OpsCallStatus.valueOf(rs.getString("status")),
                rs.getString("notes"),
                rs.getString("metadata_json"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    private RowMapper<OpsArtifact> artifactMapper() {
        return (rs, rowNum) -> new OpsArtifact(
                rs.getLong("id"),
                rs.getLong("project_id"),
                rs.getString("title"),
                OpsArtifactType.valueOf(rs.getString("artifact_type")),
                OpsArtifactStatus.valueOf(rs.getString("status")),
                rs.getString("owner_name"),
                rs.getString("artifact_url"),
                rs.getString("notes"),
                rs.getString("metadata_json"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase().replace(' ', '_');
    }

    private Integer clampProgress(Integer value) {
        if (value == null) {
            return 0;
        }
        return Math.max(0, Math.min(value, 100));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private Instant instant(ResultSet rs, String column) throws java.sql.SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
