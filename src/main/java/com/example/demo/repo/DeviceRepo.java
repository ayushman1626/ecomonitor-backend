package com.example.demo.repo;

import com.example.demo.model.Device;
import com.example.demo.model.Interface;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRepo extends JpaRepository<Device, UUID> {

    List<Device> findByInterfaceEntityId(UUID interfaceId);

    List<Device> findByInterfaceEntityIn(List<Interface> interfaces);


    void deleteAllByInterfaceEntity(Interface iface);

    void deleteById(UUID deviceId);

    boolean existsByHardwareId(String hardwareId);

    boolean existsByIdAndInterfaceEntityIn(UUID deviceId, List<Interface> interfaces);

    List<Device> findByIsActiveTrueAndLastUpdatedBefore(LocalDateTime cutoff);

    Optional<Device> findByHardwareId(String hardwareId);
}
