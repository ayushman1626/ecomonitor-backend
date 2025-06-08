package com.example.demo.repo;

import com.example.demo.model.Device;
import com.example.demo.model.Interface;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeviceRepo extends JpaRepository<Device, UUID> {

    List<Device> findByInterfaceEntityId(UUID interfaceId);

    List<Device> findByInterfaceEntityIn(List<Interface> interfaces);


    void deleteAllByInterfaceEntity(Interface iface);
}
