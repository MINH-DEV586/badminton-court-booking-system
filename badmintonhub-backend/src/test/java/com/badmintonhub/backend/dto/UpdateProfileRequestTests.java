package com.badmintonhub.backend.dto;

import com.badmintonhub.backend.dto.request.UpdateProfileRequest;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UpdateProfileRequestTests extends DtoValidationSupport {
    @Test
    void allowsProfileWithoutPhone() {
        assertTrue(invalidFields(new UpdateProfileRequest("Nguyen Van A", null)).isEmpty());
    }

    @Test
    void rejectsBlankNameAndFieldsOverDatabaseLimits() {
        assertEquals(Set.of("fullName"), invalidFields(new UpdateProfileRequest(" ", null)));
        assertEquals(Set.of("fullName", "phone"),
            invalidFields(new UpdateProfileRequest("a".repeat(121), "1".repeat(31))));
    }
}
