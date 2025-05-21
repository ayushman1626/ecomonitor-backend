package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.model.Dtos.DeviceDTO;
import com.example.demo.model.Dtos.InterfaceDTO;
import com.example.demo.model.enums.Role;
import com.example.demo.repo.DeviceRepo;
import com.example.demo.repo.InterfaceRepo;
import com.example.demo.repo.UserInterfaceRepo;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DeviceService {

    @Autowired
    DeviceRepo deviceRepo;

    @Autowired
    InterfaceRepo interfaceRepo;

    @Autowired
    UserInterfaceRepo userInterfaceRepo;

    @Autowired
    UserService userService;

    public DeviceDTO createDevice(Device device, UUID interfaceId, String username)
            throws EntityNotFoundException,AccessDeniedException{

        User currentUser = userService.getUserProfile2(username);
        Interface interfaceEntity = interfaceRepo.getReferenceById(interfaceId);
        // Check if user has ADMIN role for the interface
        UserInterface userInterface = userInterfaceRepo
                .findByUserAndInterfaceId(currentUser, interfaceEntity)
                .orElseThrow(() -> new EntityNotFoundException("No interface found for this User!!"));

        if (!userInterface.getRole().equals(Role.ADMIN)) {
            throw new AccessDeniedException("Only ADMIN can create device for this interface");
        }

        //saving
        device.setInterfaceEntity(interfaceEntity);
        device.setCreatedAt(LocalDateTime.now());
        device.setPlacementDate(LocalDate.now());
        return new DeviceDTO(deviceRepo.save(device));
    }

    public List<DeviceDTO> getAllDevices(UserPrinciple userPrinciple) {
        String username = userPrinciple.getUsername();
        User user = userService.getUserProfile2(username);

        List<UserInterface> userInterfaces = userInterfaceRepo.findByUser(user)
                .orElse(Collections.emptyList());

        if (userInterfaces.isEmpty()) {
            throw new EntityNotFoundException("No interfaces found for the user.");
        }

        List<Interface> interfaces = userInterfaces.stream()
                .map(UserInterface::getInterfaceId)
                .toList();

        List<Device> devices = deviceRepo.findByInterfaceEntityIn(interfaces);

        return devices.stream()
                .map(DeviceDTO::new)
                .toList();
    }

}
