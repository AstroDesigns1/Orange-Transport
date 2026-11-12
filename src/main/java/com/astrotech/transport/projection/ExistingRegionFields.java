package com.astrotech.transport.projection;

import com.astrotech.transport.enums.RegionName;

import java.util.UUID;

public interface ExistingRegionFields {
    RegionName getName();
    UUID getSupervisorId();
}
