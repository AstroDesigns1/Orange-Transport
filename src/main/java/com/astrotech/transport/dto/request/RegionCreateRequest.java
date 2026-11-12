package com.astrotech.transport.dto.request;

import com.astrotech.transport.enums.RegionName;
import jakarta.validation.constraints.*;

import java.util.UUID;

public record RegionCreateRequest(
      @NotNull(message = "Region name is required")
      RegionName name,
      @NotNull(message = "Regional Supervisor is needed")
      UUID regionSupervisorId
) {
}
