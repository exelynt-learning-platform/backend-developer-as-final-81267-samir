package com.system.booking.service;

import com.system.booking.dto.request.ResourceRequest;
import com.system.booking.dto.response.ResourceResponse;
import com.system.booking.exception.ResourceNotFoundException;
import com.system.booking.model.entity.Resource;
import com.system.booking.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @InjectMocks
    private ResourceService resourceService;

    private Resource resource;

    @BeforeEach
    void setUp() {
        resource = Resource.builder()
                .id(1L)
                .name("Conference Room A")
                .description("Spacious 10-person room")
                .type("ROOM")
                .build();
    }

    @Test
    @DisplayName("Should create a resource successfully")
    void createResource_Success() {
        ResourceRequest request = ResourceRequest.builder()
                .name("Conference Room A")
                .description("Spacious 10-person room")
                .type("ROOM")
                .build();

        when(resourceRepository.save(any(Resource.class))).thenReturn(resource);

        ResourceResponse response = resourceService.createResource(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Conference Room A", response.getName());
        assertEquals("ROOM", response.getType());
        verify(resourceRepository, times(1)).save(any(Resource.class));
    }

    @Test
    @DisplayName("Should retrieve all resources as list")
    void getAllResources_Success() {
        when(resourceRepository.findAll()).thenReturn(List.of(resource));

        List<ResourceResponse> responses = resourceService.getAllResources();

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("Conference Room A", responses.get(0).getName());
    }

    @Test
    @DisplayName("Should retrieve resource by ID")
    void getResourceById_Success() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));

        ResourceResponse response = resourceService.getResourceById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Conference Room A", response.getName());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when resource does not exist")
    void getResourceById_NotFound_ThrowsException() {
        when(resourceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> resourceService.getResourceById(99L)
        );
    }

    @Test
    @DisplayName("Should update resource successfully")
    void updateResource_Success() {
        ResourceRequest updateRequest = ResourceRequest.builder()
                .name("Updated Room A")
                .description("Upgraded 4K TV")
                .type("ROOM")
                .build();

        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        when(resourceRepository.save(any(Resource.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResourceResponse response = resourceService.updateResource(1L, updateRequest);

        assertNotNull(response);
        assertEquals("Updated Room A", response.getName());
        assertEquals("Upgraded 4K TV", response.getDescription());
    }

    @Test
    @DisplayName("Should delete resource successfully")
    void deleteResource_Success() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        doNothing().when(resourceRepository).delete(resource);

        resourceService.deleteResource(1L);

        verify(resourceRepository, times(1)).delete(resource);
    }
}
