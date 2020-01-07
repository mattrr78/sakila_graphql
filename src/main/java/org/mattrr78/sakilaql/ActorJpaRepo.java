package org.mattrr78.sakilaql;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActorJpaRepo extends JpaRepository<Actor, Long> {
}
