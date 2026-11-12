package com.badmintonhub.backend.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class DtoValidationSupport {
    private static ValidatorFactory factory;
    protected static Validator validator;

    @BeforeAll
    static void initializeValidator() {
        factory = Validation.byDefaultProvider().configure()
            .clockProvider(() -> Clock.fixed(Instant.parse("2026-11-12T00:00:00Z"), ZoneOffset.UTC))
            .buildValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    protected static Set<String> invalidFields(Object request) {
        return validator.validate(request).stream().map(error -> error.getPropertyPath().toString())
            .collect(Collectors.toSet());
    }
}
