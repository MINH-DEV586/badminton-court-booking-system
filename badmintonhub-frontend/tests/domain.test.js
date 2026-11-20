import test from "node:test";
import assert from "node:assert/strict";
import {
  addDays,
  bookingRange,
  estimatePrice,
  localDate,
  overlaps,
  remaining,
  zonedInstant,
} from "../src/domain.js";

test("booking wall clocks use the venue time zone rather than the browser time zone", () => {
  assert.equal(
    zonedInstant("2026-11-18", "18:00", "Asia/Ho_Chi_Minh"),
    "2026-11-18T11:00:00.000Z",
  );
  assert.equal(
    zonedInstant("2026-11-18", "18:00", "Asia/Bangkok"),
    "2026-11-18T11:00:00.000Z",
  );
  assert.equal(
    zonedInstant("2026-11-18", "18:00", "UTC"),
    "2026-11-18T18:00:00.000Z",
  );
  assert.equal(
    localDate(new Date("2026-11-17T20:00:00Z"), "Asia/Ho_Chi_Minh"),
    "2026-11-18",
  );
});
test("invalid ranges and impossible calendar dates are rejected before an API call", () => {
  const now = Date.parse("2026-11-17T00:00:00Z");
  assert.throws(
    () => bookingRange("2026-11-16", "18:00", "19:00", "Asia/Ho_Chi_Minh", now),
    /tương lai/,
  );
  assert.throws(
    () => bookingRange("2026-11-18", "19:00", "18:00", "Asia/Ho_Chi_Minh", now),
    /sau giờ/,
  );
  assert.throws(() => zonedInstant("2026-02-30", "18:00", "UTC"), /hợp lệ/);
  assert.throws(() => zonedInstant("2026-11-18", "25:00", "UTC"), /hợp lệ/);
});
test("nonexistent DST wall clocks are rejected instead of silently shifting a booking", () => {
  assert.throws(
    () => zonedInstant("2026-03-08", "02:30", "America/New_York"),
    /không tồn tại/,
  );
});
test("adjacent bookings do not overlap, genuinely intersecting ones do", () => {
  assert.equal(
    overlaps(
      "2026-11-18T10:00Z",
      "2026-11-18T11:00Z",
      "2026-11-18T11:00Z",
      "2026-11-18T12:00Z",
    ),
    false,
  );
  assert.equal(
    overlaps(
      "2026-11-18T10:00Z",
      "2026-11-18T11:00Z",
      "2026-11-18T10:30Z",
      "2026-11-18T12:00Z",
    ),
    true,
  );
});
test("estimate follows 15 minute segments and exclusive rule validity", () => {
  const court = { baseHourlyRateVnd: 80000 };
  const rules = [
    {
      dayOfWeek: 3,
      startLocalTime: "18:00:00",
      endLocalTime: "19:00:00",
      hourlyRateVnd: 120000,
      validFrom: "2026-11-01",
      validUntil: "2026-11-19",
    },
  ];
  assert.equal(
    estimatePrice(
      court,
      rules,
      "2026-11-18T10:30:00Z",
      "2026-11-18T11:30:00Z",
      "Asia/Ho_Chi_Minh",
    ),
    100000,
  );
  assert.equal(
    estimatePrice(
      court,
      rules,
      "2026-11-25T11:00:00Z",
      "2026-11-25T12:00:00Z",
      "Asia/Ho_Chi_Minh",
    ),
    80000,
  );
});
test("hold countdown cannot go negative and calendar arithmetic crosses months", () => {
  assert.equal(
    remaining("2026-11-18T10:15:00Z", Date.parse("2026-11-18T10:00:00Z")),
    "15:00",
  );
  assert.equal(
    remaining("2026-11-18T10:00:00Z", Date.parse("2026-11-18T10:01:00Z")),
    "00:00",
  );
  assert.equal(remaining(null), "00:00");
  assert.equal(addDays("2026-11-30", 1), "2026-12-01");
});
