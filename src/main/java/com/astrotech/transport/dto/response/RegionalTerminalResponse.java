package com.astrotech.transport.dto.response;

import java.util.List;

public record RegionalTerminalResponse(
        RegionResponse region,
        List<SimpleTerminalResponse> terminals
){}
