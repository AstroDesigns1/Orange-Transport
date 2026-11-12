package com.astrotech.transport.dto.response;


import java.util.List;

public record RegionResponse(
        SimpleRegionResponse region,
        UserResponse regionalSupervisor
) {
}

