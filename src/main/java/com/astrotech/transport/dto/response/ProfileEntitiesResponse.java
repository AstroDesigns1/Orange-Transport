package com.astrotech.transport.dto.response;

import com.astrotech.transport.entities.IdentityDocument;
import com.astrotech.transport.entities.User;

public record ProfileEntitiesResponse(
        User supervisor,
        User currentUser,
        IdentityDocument identityDocument) {
}
