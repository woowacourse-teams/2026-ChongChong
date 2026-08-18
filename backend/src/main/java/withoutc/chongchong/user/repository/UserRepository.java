package withoutc.chongchong.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import withoutc.chongchong.user.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}
