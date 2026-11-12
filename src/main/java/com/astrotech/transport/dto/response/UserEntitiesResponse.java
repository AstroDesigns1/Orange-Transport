package com.astrotech.transport.dto.response;

import com.astrotech.transport.entities.User;

public record UserEntitiesResponse(User supervisor, User currentUser) {
}
