package com.astrotech.transport.dto.request;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record TerminalRouteRequest(
        @NotNull(message = "Terminal Id is required")
        UUID terminalId,
        @NotNull(message = "Route Id is required")
        UUID routeId,
        @Min(1)
        @Max(10)
        Integer stopOrder
) {
}
