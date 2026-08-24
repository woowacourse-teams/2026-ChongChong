package withoutc.chongchong.assignment.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findAllByStudyId(Long studyId);

    void deleteAllByStudyId(Long studyId);

    default Assignment getAssignmentById(Long assignmentId) {
        return findById(assignmentId).orElseThrow(
                () -> new AssignmentException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND));
    }
}
