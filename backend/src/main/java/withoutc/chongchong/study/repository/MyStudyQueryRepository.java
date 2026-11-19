package withoutc.chongchong.study.repository;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import withoutc.chongchong.study.entity.StudyMemberRole;

@Repository
@RequiredArgsConstructor
public class MyStudyQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<MyStudyRow> findByUserId(Long userId, LocalDateTime leaderNoticeCreatedAfter) {
        return jdbcTemplate.query("""
                WITH mine AS (
                    SELECT id, study_id, role, created_at
                    FROM study_members
                    WHERE user_id = ?
                ), member_counts AS (
                    SELECT m.study_id, COUNT(*) AS total
                    FROM study_members m
                    JOIN mine x ON x.study_id = m.study_id
                    GROUP BY m.study_id
                ), notice_counts AS (
                    SELECT n.study_id, COUNT(*) AS total
                    FROM notices n
                    JOIN mine x ON x.study_id = n.study_id
                    WHERE (x.role <> 'LEADER' OR n.created_at > ?)
                      AND EXISTS (
                          SELECT 1 FROM notice_recipients nr
                          WHERE nr.notice_id = n.id AND nr.read_at IS NULL
                            AND (x.role = 'LEADER' OR nr.member_id = x.id)
                      )
                    GROUP BY n.study_id
                ), assignment_counts AS (
                    SELECT a.study_id, COUNT(*) AS total
                    FROM assignments a
                    JOIN mine x ON x.study_id = a.study_id
                    WHERE EXISTS (
                        SELECT 1 FROM assignment_submissions sub
                        WHERE sub.assignment_id = a.id AND sub.assigned AND sub.submitted_at IS NULL
                          AND (x.role = 'LEADER' OR sub.member_id = x.id)
                    )
                    GROUP BY a.study_id
                )
                SELECT s.id, x.role, s.name, s.description, s.is_public, f.object_key, f.content_type,
                       COALESCE(mc.total, 0) AS member_count,
                       COALESCE(nc.total, 0) AS notice_count,
                       COALESCE(ac.total, 0) AS assignment_count
                FROM mine x
                JOIN studies s ON s.id = x.study_id
                LEFT JOIN file_metadata f ON f.id = s.image_file_id
                LEFT JOIN member_counts mc ON mc.study_id = s.id
                LEFT JOIN notice_counts nc ON nc.study_id = s.id
                LEFT JOIN assignment_counts ac ON ac.study_id = s.id
                ORDER BY x.created_at DESC
                """, (rs, rowNum) -> new MyStudyRow(
                rs.getLong("id"),
                StudyMemberRole.valueOf(rs.getString("role")),
                rs.getString("name"),
                rs.getString("description"),
                rs.getBoolean("is_public"),
                rs.getInt("member_count"),
                rs.getInt("notice_count"),
                rs.getInt("assignment_count"),
                rs.getString("object_key"),
                rs.getString("content_type")
        ), userId, leaderNoticeCreatedAfter);
    }

    public record MyStudyRow(
            Long id,
            StudyMemberRole role,
            String name,
            String description,
            boolean isPublic,
            int memberCount,
            int noticeCount,
            int assignmentCount,
            String imageObjectKey,
            String imageContentType
    ) {
    }
}
