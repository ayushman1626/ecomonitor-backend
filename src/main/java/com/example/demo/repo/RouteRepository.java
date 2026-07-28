package com.example.demo.repo;

import com.example.demo.model.Route;
import com.example.demo.model.enums.RouteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RouteRepository extends JpaRepository<Route, UUID> {
    List<Route> findByInterfaceEntityId(UUID interfaceId);

    List<Route> findByInterfaceEntityIdAndStatusNot(UUID interfaceId, RouteStatus status);

    List<Route> findByInterfaceEntityIdAndStatus(UUID interfaceId, RouteStatus status);

    List<Route> findByAssignedWorkerIdAndStatus(UUID workerId, RouteStatus status);
}
