package withoutc.chongchong.user.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import withoutc.chongchong.user.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT targetUser FROM User targetUser WHERE targetUser.id = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") Long userId);
}
