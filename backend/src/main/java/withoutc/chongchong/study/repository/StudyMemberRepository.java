package withoutc.chongchong.study.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.exception.StudyMemberErrorCode;
import withoutc.chongchong.study.exception.StudyMemberException;
import withoutc.chongchong.study.repository.projection.StudyMemberSummaryProjection;

@Repository
public interface StudyMemberRepository extends JpaRepository<StudyMember, Long> {
    Optional<StudyMember> findByStudyIdAndUserId(Long studyId, Long userId);

    Optional<StudyMember> findByStudyIdAndId(Long studyId, Long memberId);

    List<StudyMember> findAllByStudyId(Long studyId);

    default StudyMember getByStudyIdAndUserIdOrThrow(Long studyId, Long userId) {
        return findByStudyIdAndUserId(studyId, userId)
                .orElseThrow(() -> new StudyMemberException(StudyMemberErrorCode.STUDY_ACCESS_DENIED));
    }

    default StudyMember getByStudyIdAndIdOrThrow(Long studyId, Long memberId) {
        return findByStudyIdAndId(studyId, memberId)
                .orElseThrow(() -> new StudyMemberException(StudyMemberErrorCode.STUDY_MEMBER_NOT_FOUND));
    }

    @EntityGraph(attributePaths = "study")
    List<StudyMember> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    int countByUserId(Long userId);

    int countByStudyId(Long studyId);

    void deleteAllByStudyId(Long studyId);

    @Query("""
            SELECT new withoutc.chongchong.study.repository.projection.StudyMemberSummaryProjection(
                member.id,
                member.name,
                member.profileImageUrl,
                member.role
            )
            FROM StudyMember member
            WHERE member.study.id = :studyId
            ORDER BY
                CASE
                    WHEN member.role = withoutc.chongchong.study.entity.StudyMemberRole.LEADER
                    THEN 0
                    ELSE 1
                END,
                member.createdAt ASC,
                member.id ASC
            """)
    List<StudyMemberSummaryProjection> findAllSummariesByStudyId(@Param("studyId") Long studyId);
}
