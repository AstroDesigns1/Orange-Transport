package com.astrotech.transport.controllers;

import com.astrotech.transport.core.ApiCacheControl;
import com.astrotech.transport.core.GetCalculatedPagination;
import com.astrotech.transport.dto.request.RegionCreateRequest;
import com.astrotech.transport.dto.request.UpdateRegionRequest;
import com.astrotech.transport.dto.response.*;
import com.astrotech.transport.ratelimit.redisRatelimit.Ratelimit;
import com.astrotech.transport.responseBuilder.ApiResponse;
import com.astrotech.transport.responseBuilder.ApiResponseBuilder;
import com.astrotech.transport.service.RegionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/region")
@RequiredArgsConstructor
@Tag(name = "Region", description = "For creating, updating region....Also for getting a region or regions")
public class RegionController {
    private final RegionService regionService;
    private final ApiCacheControl apiCacheControl;

    @PostMapping("/create")
    @Ratelimit
    public ResponseEntity<ApiResponse<RegionResponse>> createTerminal(@Valid @RequestBody RegionCreateRequest request) {

        var response = regionService.createRegion(request);
        return ApiResponseBuilder.success("Region Created Successfully", response, apiCacheControl.noStore());
    }

    @PatchMapping("/{regionId}/update")
    @Ratelimit
    public ResponseEntity<ApiResponse<RegionResponse>> updateTerminal(@Valid @RequestBody UpdateRegionRequest request, @PathVariable UUID regionId) {
        var response = regionService.updateRegion(regionId, request);
        return ApiResponseBuilder.success("Region Updated Successfully", response, apiCacheControl.noStore());
    }

    @GetMapping("/all")
    @Ratelimit
    public ResponseEntity<ApiResponse<SliceResponse<SimpleRegionResponse>>> getAllRegions(
            @RequestParam(required = false, defaultValue = GetCalculatedPagination.DEFAULT_PAGE, name = "page") int page,
            @RequestParam(required = false, defaultValue = GetCalculatedPagination.DEFAULT_SIZE, name = "size") int size) {
        var response = regionService.getAllRegions(page, size, true);
        return ApiResponseBuilder.success("Regions fetched Successfully", response, apiCacheControl.publicMaxAgeMinutes(15));


    }

    @GetMapping("/list")
    @Ratelimit
    public ResponseEntity<ApiResponse<SliceResponse<SimpleRegionResponse>>> getListTerminals(
            @RequestParam(required = false, defaultValue = GetCalculatedPagination.DEFAULT_PAGE, name = "page") int page,
            @RequestParam(required = false, defaultValue = GetCalculatedPagination.DEFAULT_SIZE, name = "size") int size) {
        var response = regionService.getAllRegions(page, size, false);
        return ApiResponseBuilder.success("Regions fetched Successfully", response, apiCacheControl.publicMaxAgeMinutes(15));
    }

    @GetMapping("/{regionId}/get-region")
    @Ratelimit
    public ResponseEntity<ApiResponse<RegionalTerminalResponse>> getTerminal(@PathVariable UUID regionId) {
        var response = regionService.getRegionWithTerminals(regionId);
        return ApiResponseBuilder.success("Region with Terminals fetched Successfully", response, apiCacheControl.publicMaxAgeHours(1));


    }
}
