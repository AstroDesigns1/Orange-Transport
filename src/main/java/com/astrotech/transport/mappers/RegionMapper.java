package com.astrotech.transport.mappers;

import com.astrotech.transport.dto.request.RegionCreateRequest;
import com.astrotech.transport.dto.response.RegionResponse;
import com.astrotech.transport.dto.response.RegionalTerminalResponse;
import com.astrotech.transport.dto.response.SimpleRegionResponse;
import com.astrotech.transport.entities.Region;
import com.astrotech.transport.entities.Terminal;
import com.astrotech.transport.entities.User;

import java.time.Instant;
import java.util.List;

public class RegionMapper {
    public static Region createRegion(RegionCreateRequest request, User user) {
        return Region.builder()
                .createdAt(Instant.now())
                .name(request.name())
                .regionalSupervisor(user)
                .active(true)
                .build();
    }

    public static SimpleRegionResponse simpleRegionResponse(Region region) {
        return new SimpleRegionResponse(
                region.getId(),
                region.getName(),
                region.isActive(),
                region.getCreatedAt()

        );
    }

    public static RegionResponse regionResponse(Region region) {
        var simpleResponse = simpleRegionResponse(region);
        var userResponse = UserMapper.response(region.getRegionalSupervisor());
        return new RegionResponse(simpleResponse, userResponse);
    }
    public static RegionalTerminalResponse getResponse(Region region, List<Terminal> terminals){
        var regionResponse = regionResponse(region);
        var terminalsResponse = TerminalMapper.simpleResponseList(terminals);
        return new RegionalTerminalResponse(regionResponse, terminalsResponse);
    }


}
