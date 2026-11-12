package com.astrotech.transport.service;


import com.astrotech.transport.core.GetCurrentUser;
import com.astrotech.transport.core.GetPageRequest;
import com.astrotech.transport.dto.request.RegionCreateRequest;
import com.astrotech.transport.dto.request.UpdateRegionRequest;
import com.astrotech.transport.dto.response.*;
import com.astrotech.transport.entities.Region;
import com.astrotech.transport.enums.AssignedServiceType;
import com.astrotech.transport.enums.UserRole;
import com.astrotech.transport.exceptions.BadRequestException;
import com.astrotech.transport.exceptions.ResourceNotFoundException;
import com.astrotech.transport.mappers.RegionMapper;
import com.astrotech.transport.repositories.RegionRepository;
import com.astrotech.transport.validators.role.RoleRequired;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegionService {
    private final ProfileService profileService;
    private final RegionRepository regionRepository;
    private final AssignWorkerService assignWorkerService;
    private final GetCurrentUser getCurrentUser;

    @RoleRequired(UserRole.ADMIN)
    @Transactional
    public RegionResponse createRegion(RegionCreateRequest request) {
        Objects.requireNonNull(request, "Request cannot be null");
        var currentUserId = getCurrentUser.getCurrentUserIdAndRole().userId();
        var existingRegions = regionRepository.findExistingFields(request.name(), request.regionSupervisorId());
        if (!existingRegions.isEmpty()) {
            if (existingRegions.stream().anyMatch(u -> request.name().equals(u.getName()))) {
                throw new BadRequestException("Region with this name already exists");
            }
            if (existingRegions.stream().anyMatch(u -> request.regionSupervisorId().equals(u.getSupervisorId()))) {
                throw new BadRequestException("Supervisor has already been assigned to a region");
            }
        }
        var profileResponse = profileService.findAllByUserIds(request.regionSupervisorId(), currentUserId, UserRole.REGIONAL_SUPERVISOR);
        var regionalSupervisor = profileResponse.supervisor();


        var regionMapper = RegionMapper.createRegion(request, regionalSupervisor);

        var savedRegion = regionRepository.save(regionMapper);

        assignWorkerService.assignWorker(regionalSupervisor.getId(), currentUserId, AssignedServiceType.REGIONAL, savedRegion.getId());

        return RegionMapper.regionResponse(savedRegion);


    }

    @Transactional
    @RoleRequired(UserRole.ADMIN)
    public RegionResponse updateRegion(UUID regionId, UpdateRegionRequest request) {
        var currentUserId = getCurrentUser.getCurrentUserIdAndRole().userId();
        var region = regionRepository.findById(regionId)
                .orElseThrow(() -> new ResourceNotFoundException("Region not found with ID: " + regionId));


        if (request.name() != null && request.name() != region.getName()) {
            boolean nameExists = regionRepository.existsByNameAndIdNot(request.name(), regionId);
            if (nameExists) {
                throw new IllegalArgumentException("Region with name '" + request.name() + "' already exists.");
            }
            region.setName(request.name());
        }


        if (request.active() != null) {
            region.setActive(request.active());
        }


        if (request.regionalSupervisorId() != null && !request.regionalSupervisorId().equals(region.getRegionalSupervisor().getId())) {
            UUID supervisorId = request.regionalSupervisorId();
            boolean supervisorAssigned = regionRepository
                    .existsByRegionalSupervisorIdAndIdNot(supervisorId, regionId);
            if (supervisorAssigned) {
                throw new IllegalArgumentException("User ID " + supervisorId + " is already a regional supervisor for another region.");
            }


            var profileResponse = profileService.findAllByUserIds(supervisorId, currentUserId, UserRole.REGIONAL_SUPERVISOR);
            var regionalSupervisor = profileResponse.supervisor();
            region.setRegionalSupervisor(regionalSupervisor);
            assignWorkerService.assignWorker(regionalSupervisor.getId(), currentUserId, AssignedServiceType.REGIONAL, region.getId());
        }

        var updatedRegion = regionRepository.save(region);

        return RegionMapper.regionResponse(updatedRegion);
    }

    @Transactional(readOnly = true)
    public SliceResponse<SimpleRegionResponse> getAllRegions(int page, int size, Boolean usePageable) {
        if (!Boolean.TRUE.equals(usePageable)) {

            var content = regionRepository.findAll()
                    .stream()
                    .map(RegionMapper::simpleRegionResponse)
                    .toList();

            return getSliceResponse(content,
                    0,
                    content.size(),
                    false,
                    false);
        }
        var pageable = GetPageRequest.getPageableWithSorting(page,
                size,
                "name",
                true,
                Region.class,
                true);
        var result = regionRepository.findAllBy(pageable);
        var content = result.getContent()
                .stream()
                .map(RegionMapper::simpleRegionResponse)
                .toList();
        return getSliceResponse(content, result.getNumber(),
                result.getSize(),
                result.hasNext(),
                result.hasPrevious());
    }

    @Transactional(readOnly = true)
    public RegionalTerminalResponse getRegionWithTerminals(UUID regionId) {
        var region = regionRepository.findWithTerminalsById(regionId)
                .orElseThrow(() -> new ResourceNotFoundException("Region not found with ID: " + regionId));

        var terminals = region.getTerminals();

        return RegionMapper.getResponse(region, terminals);

    }

    private static @NonNull SliceResponse<SimpleRegionResponse> getSliceResponse(List<SimpleRegionResponse> content, int page, int size, boolean hasNext, boolean hasPrevious) {
        return new SliceResponse<>(
                content,
                page,
                size,
                hasNext,
                hasPrevious
        );
    }

}
