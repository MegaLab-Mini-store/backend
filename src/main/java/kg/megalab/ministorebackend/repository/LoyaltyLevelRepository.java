package kg.megalab.ministorebackend.repository;

import kg.megalab.ministorebackend.entity.LoyaltyLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoyaltyLevelRepository extends JpaRepository<LoyaltyLevel, Long> {

}
