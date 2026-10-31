package withoutc.chongchong.study.repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.exception.StudyErrorCode;
import withoutc.chongchong.study.exception.StudyException;

@Repository
public interface StudyRepository extends JpaRepository<Study, Long> {
    default Study getByIdOrThrow(Long studyId) {
        return findById(studyId).orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT study
            FROM Study study
            WHERE study.id = :studyId
            """
    )
    Optional<Study> findByIdForUpdate(@Param("studyId") Long studyId);

    default Study getByIdForUpdateOrThrow(Long studyId) {
        return findByIdForUpdate(studyId).orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));
    }

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT study FROM Study study WHERE study.id = :studyId")
    Optional<Study> findByIdForShare(@Param("studyId") Long studyId);

    default Study getByIdForShareOrThrow(Long studyId) {
        return findByIdForShare(studyId).orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));
    }

    @Query(value = "SELECT * FROM studies WHERE id = :studyId FOR UPDATE", nativeQuery = true)
    Optional<Study> findByIdForDelete(@Param("studyId") Long studyId);

    default Study getByIdForDeleteOrThrow(Long studyId) {
        return findByIdForDelete(studyId).orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));
    }

    @Query("""
            SELECT s
            FROM Study s
            WHERE s.isPublic = true
              AND (SELECT COUNT(member) FROM StudyMember member WHERE member.study.id = s.id) < :maxMemberCount
              AND (:cursor IS NULL
                   OR s.createdAt < :cursorCreatedAt
                   OR (s.createdAt = :cursorCreatedAt AND s.id < :cursor))
            ORDER BY s.createdAt DESC, s.id DESC
            """)
    List<Study> findByCursor(
            @Param("cursor") Long cursor,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("maxMemberCount") int maxMemberCount,
            Pageable pageable
    );

    @Query("""
            SELECT s
            FROM Study s
            WHERE s.isPublic = true
              AND LOCATE(LOWER(:keyword), LOWER(s.name)) > 0
              AND (SELECT COUNT(member) FROM StudyMember member WHERE member.study.id = s.id) < :maxMemberCount
              AND (:cursor IS NULL
                   OR s.createdAt < :cursorCreatedAt
                   OR (s.createdAt = :cursorCreatedAt AND s.id < :cursor))
            ORDER BY s.createdAt DESC, s.id DESC
            """)
    List<Study> searchByNameAndCursor(
            @Param("keyword") String keyword,
            @Param("cursor") Long cursor,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("maxMemberCount") int maxMemberCount,
            Pageable pageable
    );
}
