package com.astrotech.transport.dto.request;

import com.astrotech.transport.enums.RegionName;

import java.util.UUID;

public record UpdateRegionRequest(
        RegionName name,
        Boolean active,
        UUID regionalSupervisorId
) {}
