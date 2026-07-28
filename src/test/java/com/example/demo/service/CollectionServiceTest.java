package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.model.Dtos.collection.CollectStopRequest;
import com.example.demo.model.Dtos.route.RouteStopDto;
import com.example.demo.model.enums.RouteStatus;
import com.example.demo.model.enums.RouteStopStatus;
import com.example.demo.repo.CollectionLogRepository;
import com.example.demo.repo.RouteRepository;
import com.example.demo.repo.RouteStopRepository;
import com.example.demo.repo.UserRepo;
import com.example.demo.repo.UserInterfaceRepo;
import com.example.demo.exceptions.RfidVerificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CollectionServiceTest {

    @Mock
    private RouteRepository routeRepository;
    @Mock
    private RouteStopRepository routeStopRepository;
    @Mock
    private CollectionLogRepository collectionLogRepository;
    @Mock
    private UserRepo userRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private RouteService routeService;
    @Mock
    private UserInterfaceRepo userInterfaceRepo;

    @InjectMocks
    private CollectionService collectionService;

    private UUID routeId;
    private UUID stopId;
    private UUID workerId;
    private Route route;
    private RouteStop routeStop;
    private Device device;

    @BeforeEach
    void setUp() {
        routeId = UUID.randomUUID();
        stopId = UUID.randomUUID();
        workerId = UUID.randomUUID();

        route = new Route();
        route.setId(routeId);
        route.setAssignedWorkerId(workerId);
        route.setStatus(RouteStatus.ACTIVE);

        device = new Device();
        device.setId(UUID.randomUUID());
        device.setHardwareId("RFID-12345");

        routeStop = new RouteStop();
        routeStop.setId(stopId);
        routeStop.setRoute(route);
        routeStop.setDevice(device);
        routeStop.setStatus(RouteStopStatus.PENDING);
    }

    @Test
    void collectStop_Success_WhenRfidMatches() {
        // Arrange
        CollectStopRequest request = new CollectStopRequest();
        request.setRfidTag("RFID-12345");
        request.setLatitude(12.34);
        request.setLongitude(56.78);
        request.setNotes("Clean collection");

        when(routeRepository.findById(routeId)).thenReturn(Optional.of(route));
        when(routeStopRepository.findById(stopId)).thenReturn(Optional.of(routeStop));
        when(routeStopRepository.save(any(RouteStop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RouteStopDto expectedDto = new RouteStopDto();
        expectedDto.setStopId(stopId);
        expectedDto.setStatus(RouteStopStatus.COLLECTED.name());
        expectedDto.setRfidVerified(true);
        when(routeService.mapStopToDto(any(RouteStop.class))).thenReturn(expectedDto);

        // Act
        RouteStopDto result = collectionService.collectStop(routeId, stopId, workerId, request);

        // Assert
        assertNotNull(result);
        assertTrue(result.getRfidVerified());
        assertEquals(RouteStopStatus.COLLECTED.name(), result.getStatus());

        verify(routeStopRepository).save(routeStop);
        verify(collectionLogRepository).save(any(CollectionLog.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/route/" + routeId + "/updates"), eq(expectedDto));
    }

    @Test
    void collectStop_ThrowsException_WhenRfidDoesNotMatch() {
        // Arrange
        CollectStopRequest request = new CollectStopRequest();
        request.setRfidTag("RFID-WRONG");
        request.setLatitude(12.34);
        request.setLongitude(56.78);

        when(routeRepository.findById(routeId)).thenReturn(Optional.of(route));
        when(routeStopRepository.findById(stopId)).thenReturn(Optional.of(routeStop));

        // Act & Assert
        RfidVerificationException exception = assertThrows(RfidVerificationException.class, () -> {
            collectionService.collectStop(routeId, stopId, workerId, request);
        });

        assertTrue(exception.getMessage().contains("RFID verification failed"));
        verify(routeStopRepository, never()).save(any(RouteStop.class));
        verify(collectionLogRepository, never()).save(any(CollectionLog.class));
    }

    @Test
    void collectStop_ThrowsException_WhenRfidIsNull() {
        // Arrange
        CollectStopRequest request = new CollectStopRequest();
        request.setRfidTag(null);
        request.setLatitude(12.34);
        request.setLongitude(56.78);

        when(routeRepository.findById(routeId)).thenReturn(Optional.of(route));
        when(routeStopRepository.findById(stopId)).thenReturn(Optional.of(routeStop));

        // Act & Assert
        RfidVerificationException exception = assertThrows(RfidVerificationException.class, () -> {
            collectionService.collectStop(routeId, stopId, workerId, request);
        });

        assertTrue(exception.getMessage().contains("RFID verification failed"));
        verify(routeStopRepository, never()).save(any(RouteStop.class));
        verify(collectionLogRepository, never()).save(any(CollectionLog.class));
    }
}
