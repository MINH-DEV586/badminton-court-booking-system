package com.badmintonhub.backend.controller;

import com.badmintonhub.backend.exception.GlobalExceptionHandler;
import com.badmintonhub.backend.repository.UserAccountRepository;
import com.badmintonhub.backend.security.*;
import com.badmintonhub.backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = ControllerTestSupport.TestConfig.class)
@TestPropertySource(properties = "app.security.jwt-secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
public abstract class ControllerTestSupport {
    @Autowired protected WebApplicationContext context;
    @Autowired protected VenueService venues;
    @Autowired protected CourtService courts;
    @Autowired protected BookingService bookings;
    @Autowired protected CourtBlockService blocks;
    @Autowired protected PricingRuleService pricing;
    @Autowired protected PaymentService payments;
    @Autowired protected UserService users;
    protected MockMvc mvc;
    protected final UUID callerId = UUID.randomUUID();

    @BeforeEach
    void setUpMvc() {
        // Keep service authorization active for HTTP requests, but stub/verify the underlying mocks.
        venues = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(venues);
        courts = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(courts);
        bookings = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(bookings);
        blocks = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(blocks);
        pricing = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(pricing);
        payments = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(payments);
        users = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(users);
        reset(venues, courts, bookings, blocks, pricing, payments, users);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    protected RequestPostProcessor identity(String role) {
        return jwt().jwt(token -> token.subject(callerId.toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, JwtConfig.class, DatabaseUserDetailsService.class,
        AccountJwtAuthenticationConverter.class, GlobalExceptionHandler.class})
    static class TestConfig {
        @Bean UserAccountRepository accounts() { return mock(UserAccountRepository.class); }
        @Bean VenueService venues() { return mock(VenueService.class); }
        @Bean CourtService courts() { return mock(CourtService.class); }
        @Bean BookingService bookings() { return mock(BookingService.class); }
        @Bean CourtBlockService blocks() { return mock(CourtBlockService.class); }
        @Bean PricingRuleService pricing() { return mock(PricingRuleService.class); }
        @Bean PaymentService payments() { return mock(PaymentService.class); }
        @Bean UserService users() { return mock(UserService.class); }
    }
}
