package net.flectone.pulse.backend.repository;

import net.flectone.pulse.backend.model.ServerMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MetricsRepository extends JpaRepository<ServerMetrics, String> {

    List<ServerMetrics> findByCreatedAtAfter(Instant createdAt);

}
