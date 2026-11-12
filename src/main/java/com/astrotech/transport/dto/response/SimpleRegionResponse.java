package com.astrotech.transport.dto.response;

import com.astrotech.transport.enums.RegionName;

import java.time.Instant;
import java.util.UUID;

public record SimpleRegionResponse(
        UUID id,
        RegionName name,
        boolean active,
        Instant createdAt
){
    
}
