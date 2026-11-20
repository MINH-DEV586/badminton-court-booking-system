package com.badmintonhub.backend.dev;

import com.badmintonhub.backend.entity.*;
import com.badmintonhub.backend.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;

/** Sample records exist only in the explicitly selected in-memory frontend-preview environment. */
@Component
@Profile("frontend-preview")
public class FrontendPreviewData implements ApplicationRunner {
    private final UserAccountRepository users;
    private final VenueRepository venues;
    private final CourtRepository courts;
    private final BookingRepository bookings;
    private final PricingRuleRepository pricing;
    private final CourtBlockRepository blocks;
    private final PaymentRepository payments;
    private final PasswordEncoder encoder;
    private final String password;

    public FrontendPreviewData(UserAccountRepository users, VenueRepository venues, CourtRepository courts,
        BookingRepository bookings, PricingRuleRepository pricing, CourtBlockRepository blocks,
        PaymentRepository payments, PasswordEncoder encoder, @Value("${app.preview.password}") String password) {
        this.users = users;
        this.venues = venues;
        this.courts = courts;
        this.bookings = bookings;
        this.pricing = pricing;
        this.blocks = blocks;
        this.payments = payments;
        this.encoder = encoder;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        if (users.count() != 0 || venues.count() != 0) return;
        var customer = user("minhanh@badmintonhub.test", "Minh Anh", UserAccount.Role.CUSTOMER);
        var owner = user("chusan@badmintonhub.test", "Nguyễn Hoàng Nam", UserAccount.Role.OWNER);
        user("admin@badmintonhub.test", "Quản trị viên", UserAccount.Role.ADMIN);
        var venue = venue(owner, "Cụm sân Bình Thạnh", "123 Đinh Bộ Lĩnh, phường 26, Bình Thạnh, TP.HCM");
        var first = court(venue, "Sân 01", 80000, true);
        var second = court(venue, "Sân 02", 80000, true);
        court(venue, "Sân 03", 80000, false);
        var tanBinh = venue(owner, "Cụm sân Tân Bình", "88 Hoàng Hoa Thám, phường 12, Tân Bình, TP.HCM");
        court(tanBinh, "Sân 01", 120000, true);
        court(tanBinh, "Sân 02", 100000, true);
        var phuNhuan = venue(owner, "Cụm sân Phú Nhuận", "45 Phan Xích Long, phường 2, Phú Nhuận, TP.HCM");
        court(phuNhuan, "Sân 01", 100000, true);
        court(phuNhuan, "Sân 02", 100000, true);
        var tomorrow = LocalDate.now(ZoneId.of(venue.getTimeZone())).plusDays(1);
        var start = tomorrow.atTime(18, 0).atZone(ZoneId.of(venue.getTimeZone())).toInstant();
        var booked = booking(customer, first, start, Booking.Status.CONFIRMED, 80000);
        booking(customer, second, start.plus(Duration.ofDays(1)), Booking.Status.PENDING_PAYMENT, 80000);
        var paid = new Payment();
        paid.setBooking(booked);
        paid.setProvider("PREVIEW");
        paid.setProviderPaymentId("PREVIEW-001");
        paid.setAmountVnd(80000);
        paid.setStatus(Payment.Status.SUCCEEDED);
        paid.setPaidAt(Instant.now());
        payments.save(paid);
        var rule = new PricingRule();
        rule.setCourt(first);
        rule.setDayOfWeek(tomorrow.getDayOfWeek().getValue());
        rule.setStartLocalTime(LocalTime.of(18, 0));
        rule.setEndLocalTime(LocalTime.of(22, 0));
        rule.setHourlyRateVnd(100000);
        rule.setValidFrom(tomorrow);
        pricing.save(rule);
        var block = new CourtBlock();
        block.setCourt(second);
        block.setCreatedBy(owner);
        block.setStartsAt(tomorrow.atTime(12, 0).atZone(ZoneId.of(venue.getTimeZone())).toInstant());
        block.setEndsAt(block.getStartsAt().plusSeconds(7200));
        block.setReason("Vệ sinh và bảo trì định kỳ");
        blocks.save(block);
    }

    private UserAccount user(String email, String name, UserAccount.Role role) {
        var value = new UserAccount();
        value.setEmail(email);
        value.setFullName(name);
        value.setPasswordHash(encoder.encode(password));
        value.setPhone("0901234567");
        value.setRole(role);
        return users.save(value);
    }
    private Venue venue(UserAccount owner, String name, String address) {
        var value = new Venue();
        value.setOwner(owner);
        value.setName(name);
        value.setAddress(address);
        value.setTimeZone("Asia/Ho_Chi_Minh");
        return venues.save(value);
    }
    private Court court(Venue venue, String name, long price, boolean active) {
        var value = new Court();
        value.setVenue(venue);
        value.setName(name);
        value.setBaseHourlyRateVnd(price);
        value.setActive(active);
        return courts.save(value);
    }
    private Booking booking(UserAccount customer, Court court, Instant start, Booking.Status status, long price) {
        var value = new Booking();
        value.setCustomer(customer);
        value.setCourt(court);
        value.setStartsAt(start);
        value.setEndsAt(start.plusSeconds(3600));
        value.setTotalAmountVnd(price);
        value.setStatus(status);
        if (status == Booking.Status.PENDING_PAYMENT) value.setHoldExpiresAt(Instant.now().plusSeconds(900));
        return bookings.save(value);
    }
}
