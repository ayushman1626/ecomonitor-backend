package com.example.demo.service;

import com.example.demo.exceptions.UserAlreadyExistsException;
import com.example.demo.model.*;
import com.example.demo.model.Dtos.SensorReadingDTO;
import com.example.demo.model.Dtos.device.DeviceDTO;
import com.example.demo.model.Dtos.device.DeviceRequestDTO;
import com.example.demo.model.enums.DeviceType;
import com.example.demo.model.enums.Role;
import com.example.demo.repo.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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

    @Autowired
    SensorReadingRepo sensorReadingRepo;
    @Autowired
    private UserRepo userRepo;

    @Autowired
    private RedisService redisService;


    public DeviceDTO createDevice(DeviceRequestDTO deviceRequest, UUID interfaceId, String username)
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
        Device newDevice = new Device();
        newDevice.setName(deviceRequest.getName());
        newDevice.setLocation(deviceRequest.getLocation());
        newDevice.setType(deviceRequest.getType());
        newDevice.setInterfaceEntity(interfaceEntity);
        newDevice.setCreatedAt(LocalDateTime.now());
        newDevice.setPlacementDate(LocalDate.now());
        System.out.println("Device type being saved: [" + deviceRequest.getType().name() + "]");
        return new DeviceDTO(deviceRepo.save(newDevice));
    }

    public void linkHardwareDevice(
            UUID deviceId, String hardwareId, String username) throws AccessDeniedException{
            if(deviceRepo.existsByHardwareId(hardwareId)){
                throw new UserAlreadyExistsException("HardwareId already in use");
            }
            Device device = deviceRepo.findById(deviceId).orElseThrow(() -> new EntityNotFoundException("Device not found"));
            System.out.println("Device found: " + device.getName() + " with IDDDDDDDDDDDDDDD: " + device.getId());
            User user = userRepo.findByUsername(username);

            boolean hasAdminAccess = userHasAdminAccessToDevice(user, deviceId);
            if (!hasAdminAccess) {
                throw new AccessDeniedException("You do not have access modify this device");
            }

            device.setHardwareId(hardwareId);
            deviceRepo.save(device);
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

    public DeviceDTO getDeviceById(UUID deviceId, String username)
    throws Exception {
        User user = userService.getUserProfile2(username);
        boolean hasAccess = userHasAccessToDevice(user, deviceId);
        if(!hasAccess) {
            throw new AccessDeniedException("You do not have access to this device.");
        }

        Device device = deviceRepo.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("Device not found"));
        return new DeviceDTO(device);
    }

    // Get device readings for the last 'days' days or 'hours' hours
    public List<SensorReadingDTO> getDeviceReadings(UUID deviceId, String username, Integer days, Integer hours) throws AccessDeniedException {
        User user = userService.getUserProfile2(username);
        Device sensor = deviceRepo.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("Device not found"));

        boolean hasAccess = userHasAccessToDevice(user, deviceId);

        if(!hasAccess) {
            throw new AccessDeniedException("You do not have access to this device.");
        }
        LocalDateTime fromDate;
        if (hours != null) {
            fromDate = LocalDateTime.now().minusHours(hours);
        } else if (days != null) {
            fromDate = LocalDateTime.now().minusDays(days);
        } else {
            // default to 1 day if neither is provided (optional)
            fromDate = LocalDateTime.now().minusDays(1);
        }
        // Fetch sensor readings from the repository
        List<SensorReading> readings = sensorReadingRepo.findBySensorAndFromDate(sensor, fromDate);
        if(readings == null || readings.isEmpty()) {
            return Collections.emptyList();
        }
        return readings.stream()
                .map(reading -> new SensorReadingDTO(
                        reading.getSensor().getId().toString(),
                        reading.getValue1().toString(),
                        reading.getValue2() != null ? reading.getValue2().toString() : null,
                        reading.getValue3().toString(),
                        reading.getRecordedAt().toString()))
                .toList();
    }

    // Method to delete a device
    @Transactional
    public void deleteDevice(UUID deviceId, String username) throws AccessDeniedException {
        User user = userService.getUserProfile2(username);
        Device device = deviceRepo.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("Device not found"));

        // Check if the user has access to the device
        boolean hasAdminAccess = userHasAdminAccessToDevice(user, deviceId);

        if (!hasAdminAccess) {
            throw new AccessDeniedException("You do not have access to delete this device.");
        }
        // Delete the device
        sensorReadingRepo.deleteBySensor(device);
        deviceRepo.delete(device);
        redisService.evictDevice(device.getHardwareId());
    }

    //Check if user(checked) has access to a device
    public boolean userHasAccessToDevice(User user, UUID deviceId) {
        List<UserInterface> userInterfaces = userInterfaceRepo.findByUser(user)
                .orElse(Collections.emptyList());
        if (userInterfaces.isEmpty()) {
            return false;
        }
        List<Interface> interfaces = userInterfaces.stream()
                .map(UserInterface::getInterfaceId)
                .toList();
        return deviceRepo.existsByIdAndInterfaceEntityIn(deviceId, interfaces);
    }

    //check if user(checked) has ADMIN access to a device
    public boolean userHasAdminAccessToDevice(User user, UUID deviceId) throws AccessDeniedException {
        Device device = deviceRepo.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("Device not found"));
        Interface interfaceEntity = device.getInterfaceEntity();
        UserInterface userInterface = userInterfaceRepo
                .findByUserAndInterfaceId(user, interfaceEntity)
                .orElseThrow(() -> new AccessDeniedException("No access to this interface"));

        return userInterface.getRole().equals(Role.ADMIN);
    }



}
