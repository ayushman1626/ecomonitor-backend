package com.example.demo.repo;

import com.example.demo.model.CollectionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CollectionLogRepository extends JpaRepository<CollectionLog, Long> {
    List<CollectionLog> findByRouteId(UUID routeId);
}
