package com.system.booking.service;

import com.system.booking.dto.request.ResourceRequest;
import com.system.booking.dto.response.ResourceResponse;
import com.system.booking.exception.ResourceNotFoundException;
import com.system.booking.model.entity.Resource;
import com.system.booking.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;

    @Transactional
    public ResourceResponse createResource(ResourceRequest request) {
        Resource resource = Resource.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .build();

        Resource saved = resourceRepository.save(resource);
        log.info("Created new resource with ID: {}", saved.getId());
        return ResourceResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> getAllResources() {
        return resourceRepository.findAll().stream()
                .map(ResourceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Paginated variant — used by GET /api/resources?page=&size=&sort=
     */
    @Transactional(readOnly = true)
    public Page<ResourceResponse> getAllResourcesPaged(Pageable pageable) {
        return resourceRepository.findAll(pageable)
                .map(ResourceResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public ResourceResponse getResourceById(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", id));
        return ResourceResponse.fromEntity(resource);
    }

    @Transactional
    public ResourceResponse updateResource(Long id, ResourceRequest request) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", id));

        resource.setName(request.getName());
        resource.setDescription(request.getDescription());
        resource.setType(request.getType());

        Resource updated = resourceRepository.save(resource);
        log.info("Updated resource with ID: {}", updated.getId());
        return ResourceResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteResource(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", id));

        resourceRepository.delete(resource);
        log.info("Deleted resource with ID: {}", id);
    }
}
