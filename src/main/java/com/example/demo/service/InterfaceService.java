package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.model.Dtos.InterfaceDTO;
import com.example.demo.model.Dtos.InterfaceWithDevicesDTO;
import com.example.demo.model.enums.Role;
import com.example.demo.repo.DeviceRepo;
import com.example.demo.repo.InterfaceRepo;
import com.example.demo.repo.UserInterfaceRepo;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class InterfaceService {
    @Autowired
    UserService userService;

    @Autowired
    UserInterfaceRepo userInterfaceRepo;

    @Autowired
    InterfaceRepo interfaceRepo;

    @Autowired
    DeviceRepo deviceRepo;


    public List<InterfaceDTO> getInterfaceForUser(String username) throws UsernameNotFoundException {

        User user = userService.getUserProfile2(username);
        List<UserInterface> userInterfaces = userInterfaceRepo.findByUser(user).orElse(Collections.emptyList());

        return userInterfaces.stream()
                .map(ui -> new InterfaceDTO(ui.getInterfaceId(),ui.getRole().name()))
                .collect(Collectors.toList());
    }

    @Transactional
    public InterfaceDTO saveInterface(Interface iface, String username) throws Exception{
        User user = userService.getUserProfile2(username);
        iface.setCreatedBy(user);
        iface.setCreatedAt(LocalDateTime.now());
        Interface savedInterface = interfaceRepo.save(iface);
        UserInterface userInterfaceAccess = new UserInterface();
        userInterfaceAccess.setUser(user);
        userInterfaceAccess.setInterfaceId(savedInterface);
        userInterfaceAccess.setRole(Role.ADMIN);
        userInterfaceRepo.save(userInterfaceAccess);
        return new InterfaceDTO(savedInterface,Role.ADMIN.name());
    }

    public InterfaceWithDevicesDTO getInterfaceWithDevices(UUID interfaceId, String username) throws EntityNotFoundException{
        User user = userService.getUserProfile2(username);

        // Fetch the Interface
        Interface interfaceEntity = interfaceRepo.findById(interfaceId)
                .orElseThrow(() -> new EntityNotFoundException("Interface not found"));

        // Fetch all Devices linked to this Interface
        List<Device> devices = deviceRepo.findByInterfaceEntityId(interfaceId);

        //fetch role of current user for this Interface
        UserInterface userInterface = userInterfaceRepo.findByUserAndInterfaceId(user,interfaceEntity)
                .orElseThrow(() -> new EntityNotFoundException("Interface not found"));

        return new InterfaceWithDevicesDTO(interfaceEntity, devices, userInterface.getRole().name());
    }




    private InterfaceDTO mapToDto(UserInterface ui) {
        Interface iface = ui.getInterfaceId();
        return new InterfaceDTO(
                iface.getId().toString(),
                iface.getName(),
                iface.getDescription(),
                ui.getUser().getId().toString(),
                ui.getUser().getUsername(),
                ui.getRole().name(),
                iface.getCreatedAt().toString()
        );
    }

}
