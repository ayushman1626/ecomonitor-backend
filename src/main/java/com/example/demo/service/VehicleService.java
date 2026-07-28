package com.example.demo.service;

import com.example.demo.model.Dtos.vehicle.VehicleRequestDto;
import com.example.demo.model.Dtos.vehicle.VehicleResponseDto;
import com.example.demo.model.Interface;
import com.example.demo.model.User;
import com.example.demo.model.Vehicle;
import com.example.demo.repo.InterfaceRepo;
import com.example.demo.repo.UserRepo;
import com.example.demo.repo.VehicleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VehicleService {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private InterfaceRepo interfaceRepository;

    @Autowired
    private UserRepo userRepository;

    @Transactional
    public VehicleResponseDto createVehicle(VehicleRequestDto dto) {
        Interface iface = interfaceRepository.findById(dto.getInterfaceId())
                .orElseThrow(() -> new EntityNotFoundException("Interface not found"));

        Vehicle vehicle = new Vehicle();
        vehicle.setName(dto.getName());
        vehicle.setLicensePlate(dto.getLicensePlate());
        vehicle.setCapacity(dto.getCapacity());
        vehicle.setInterfaceEntity(iface);
        vehicle.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        vehicle.setCreatedAt(LocalDateTime.now());

        if (dto.getDefaultDriverId() != null) {
            User driver = userRepository.findById(dto.getDefaultDriverId())
                    .orElseThrow(() -> new EntityNotFoundException("Default driver not found"));
            vehicle.setDefaultDriver(driver);
        }

        Vehicle saved = vehicleRepository.save(vehicle);
        return mapToDto(saved);
    }

    public List<VehicleResponseDto> getVehiclesByInterface(UUID interfaceId) {
        return vehicleRepository.findByInterfaceEntityId(interfaceId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public VehicleResponseDto assignDriver(UUID vehicleId, UUID driverId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new EntityNotFoundException("Vehicle not found"));

        if (driverId != null) {
            User driver = userRepository.findById(driverId)
                    .orElseThrow(() -> new EntityNotFoundException("Driver not found"));
            vehicle.setDefaultDriver(driver);
        } else {
            vehicle.setDefaultDriver(null);
        }

        Vehicle saved = vehicleRepository.save(vehicle);
        return mapToDto(saved);
    }

    @Transactional
    public VehicleResponseDto toggleActiveStatus(UUID vehicleId, boolean isActive) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new EntityNotFoundException("Vehicle not found"));

        vehicle.setIsActive(isActive);
        Vehicle saved = vehicleRepository.save(vehicle);
        return mapToDto(saved);
    }

    @Transactional
    public void deleteVehicle(UUID vehicleId) {
        if (!vehicleRepository.existsById(vehicleId)) {
            throw new EntityNotFoundException("Vehicle not found");
        }
        vehicleRepository.deleteById(vehicleId);
    }

    public VehicleResponseDto mapToDto(Vehicle vehicle) {
        VehicleResponseDto dto = new VehicleResponseDto();
        dto.setId(vehicle.getId());
        dto.setName(vehicle.getName());
        dto.setLicensePlate(vehicle.getLicensePlate());
        dto.setCapacity(vehicle.getCapacity());
        dto.setInterfaceId(vehicle.getInterfaceEntity().getId());
        dto.setIsActive(vehicle.getIsActive());
        dto.setCreatedAt(vehicle.getCreatedAt() != null ? vehicle.getCreatedAt().toString() : null);

        if (vehicle.getDefaultDriver() != null) {
            dto.setDefaultDriverId(vehicle.getDefaultDriver().getId());
            dto.setDefaultDriverName(vehicle.getDefaultDriver().getFullName());
        }

        return dto;
    }
}
