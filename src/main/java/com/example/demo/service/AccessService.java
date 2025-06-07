package com.example.demo.service;

import com.example.demo.model.Dtos.access.InterfaceAccessDTO;
import com.example.demo.model.Interface;
import com.example.demo.model.User;
import com.example.demo.model.UserInterface;
import com.example.demo.model.enums.Role;
import com.example.demo.repo.InterfaceRepo;
import com.example.demo.repo.UserInterfaceRepo;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AccessService {

    @Autowired
    private UserService userService;

    @Autowired
    private InterfaceRepo interfaceRepo;

    @Autowired
    UserInterfaceRepo userInterfaceRepo;

    @Transactional
    public Boolean giveAccess(UUID interfaceId, String username, String role , String currentUserUsername) throws AccessDeniedException{
        User currentUser = userService.getUserProfile2(currentUserUsername);
        User newUser = userService.getUserProfile2(username);
        Interface interfaceEntity = interfaceRepo.getReferenceById(interfaceId);

        //checking if the current user has admin access
        UserInterface userInterface = userInterfaceRepo
                .findByUserAndInterfaceId(currentUser, interfaceEntity)
                .orElseThrow(() -> new AccessDeniedException("Access denied to this interface"));

        if (!userInterface.getRole().equals(Role.ADMIN)) {
            throw new AccessDeniedException("Only ADMIN can give access for this interface");
        }

        //checking if user has access of the Interface already
        Optional<UserInterface> existingAccess = userInterfaceRepo
                .findByUserAndInterfaceId(newUser, interfaceEntity);

        if(existingAccess.isPresent()){
            throw new IllegalStateException("User already has Access");
        }

        //saving
        UserInterface newUserInterface = new UserInterface();
        newUserInterface.setUser(newUser);
        newUserInterface.setInterfaceId(interfaceEntity);
        newUserInterface.setRole(role.equalsIgnoreCase("ADMIN")?Role.ADMIN: Role.VIEWER);
        userInterfaceRepo.save(newUserInterface);
        return true;
    }

    public List<InterfaceAccessDTO> getAllAccess(UUID interfaceId, String currentUsername) throws AccessDeniedException{
        User currentUser = userService.getUserProfile2(currentUsername);
        Interface interfaceEntity = interfaceRepo.getById(interfaceId);

        UserInterface currentAccess = userInterfaceRepo
                .findByUserAndInterfaceId(currentUser,interfaceEntity)
                .orElseThrow(() -> new AccessDeniedException("Access denied to this interface"));

        if (!currentAccess.getRole().equals(Role.ADMIN)) {
            throw new AccessDeniedException("Only ADMIN view access for this interface");
        }

        List<UserInterface> accessList = userInterfaceRepo.findByInterfaceId(interfaceEntity);

        return accessList.stream()
                .map(ui -> new InterfaceAccessDTO(ui.getUser(),ui.getRole()))
                .collect(Collectors.toList());
    }
}
