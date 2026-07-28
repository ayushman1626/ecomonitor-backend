package com.example.demo.repo;

import com.example.demo.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    List<Vehicle> findByInterfaceEntityId(UUID interfaceId);
    List<Vehicle> findByInterfaceEntityIdAndIsActiveTrue(UUID interfaceId);
}
