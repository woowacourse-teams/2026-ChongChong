package withoutc.chongchong.assignment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import withoutc.chongchong.assignment.entity.Assignment;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
}
