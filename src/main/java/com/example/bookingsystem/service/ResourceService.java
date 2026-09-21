package com.example.bookingsystem.service;

import com.example.bookingsystem.dto.ResourceRequest;
import com.example.bookingsystem.dto.ResourceResponse;
import com.example.bookingsystem.entity.Resource;
import com.example.bookingsystem.exception.ResourceNotFoundException;
import com.example.bookingsystem.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;

    @Transactional(readOnly = true)
    public Page<ResourceResponse> getAll(Pageable pageable) {
        return resourceRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ResourceResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Transactional
    public ResourceResponse create(ResourceRequest request) {
        Resource resource = Resource.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .available(request.getAvailable() == null || request.getAvailable())
                .build();
        return toResponse(resourceRepository.save(resource));
    }

    @Transactional
    public ResourceResponse update(Long id, ResourceRequest request) {
        Resource resource = findEntity(id);
        resource.setName(request.getName());
        resource.setDescription(request.getDescription());
        resource.setType(request.getType());
        if (request.getAvailable() != null) {
            resource.setAvailable(request.getAvailable());
        }
        return toResponse(resourceRepository.save(resource));
    }

    @Transactional
    public void delete(Long id) {
        Resource resource = findEntity(id);
        resourceRepository.delete(resource);
    }

    Resource findEntity(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
    }

    private ResourceResponse toResponse(Resource resource) {
        return ResourceResponse.builder()
                .id(resource.getId())
                .name(resource.getName())
                .description(resource.getDescription())
                .type(resource.getType())
                .available(resource.isAvailable())
                .createdAt(resource.getCreatedAt())
                .build();
    }
}
