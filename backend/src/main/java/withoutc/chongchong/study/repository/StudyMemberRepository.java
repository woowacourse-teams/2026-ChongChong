package withoutc.chongchong.study.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.exception.StudyMemberErrorCode;
import withoutc.chongchong.study.exception.StudyMemberException;
import withoutc.chongchong.study.repository.projection.StudyMemberIdentityProjection;
import withoutc.chongchong.study.repository.projection.StudyMemberSummaryProjection;

@Repository
public interface StudyMemberRepository extends JpaRepository<StudyMember, Long> {
    boolean existsByStudyIdAndUserId(Long studyId, Long userId);

    default void requireExistsByStudyIdAndUserId(Long studyId, Long userId) {
        if (!existsByStudyIdAndUserId(studyId, userId)) {
            throw new StudyMemberException(StudyMemberErrorCode.STUDY_ACCESS_DENIED);
        }
    }

    Optional<StudyMember> findByStudyIdAndUserId(Long studyId, Long userId);

    Optional<StudyMember> findByStudyIdAndId(Long studyId, Long memberId);

    List<StudyMember> findAllByStudyId(Long studyId);

    List<StudyMember> findAllByStudyIdAndRole(Long studyId, StudyMemberRole role);

    @Query("""
            SELECT new withoutc.chongchong.study.repository.projection.StudyMemberIdentityProjection(
                member.id, member.user.id)
            FROM StudyMember member
            WHERE member.study.id = :studyId
            ORDER BY member.id
            """)
    List<StudyMemberIdentityProjection> findIdentitiesByStudyId(@Param("studyId") Long studyId);

    @Query("""
            SELECT member.id FROM StudyMember member
            WHERE member.study.id = :studyId AND member.user.id = :userId
            """)
    Optional<Long> findIdByStudyIdAndUserId(@Param("studyId") Long studyId, @Param("userId") Long userId);

    default Long getIdByStudyIdAndUserIdOrThrow(Long studyId, Long userId) {
        return findIdByStudyIdAndUserId(studyId, userId)
                .orElseThrow(() -> new StudyMemberException(StudyMemberErrorCode.STUDY_ACCESS_DENIED));
    }

    @Query("""
            SELECT member.user.id FROM StudyMember member
            WHERE member.study.id = :studyId AND member.id = :memberId
            """)
    Optional<Long> findUserIdByStudyIdAndId(@Param("studyId") Long studyId, @Param("memberId") Long memberId);

    default Long getUserIdByStudyIdAndIdOrThrow(Long studyId, Long memberId) {
        return findUserIdByStudyIdAndId(studyId, memberId)
                .orElseThrow(() -> new StudyMemberException(StudyMemberErrorCode.STUDY_MEMBER_NOT_FOUND));
    }

    default StudyMember getByStudyIdAndUserIdOrThrow(Long studyId, Long userId) {
        return findByStudyIdAndUserId(studyId, userId)
                .orElseThrow(() -> new StudyMemberException(StudyMemberErrorCode.STUDY_ACCESS_DENIED));
    }

    default StudyMember getByStudyIdAndIdOrThrow(Long studyId, Long memberId) {
        return findByStudyIdAndId(studyId, memberId)
                .orElseThrow(() -> new StudyMemberException(StudyMemberErrorCode.STUDY_MEMBER_NOT_FOUND));
    }

    @EntityGraph(attributePaths = {"study", "study.imageFile"})
    List<StudyMember> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<StudyMember> findAllByUserId(Long userId);

    boolean existsByUserIdAndRole(Long userId, StudyMemberRole role);

    int countByUserId(Long userId);

    int countByStudyId(Long studyId);

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
