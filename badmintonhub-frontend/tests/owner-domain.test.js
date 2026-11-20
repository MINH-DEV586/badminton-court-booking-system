import test from "node:test";
import assert from "node:assert/strict";
import { courtDaySchedule, isEffectiveBooking } from "../src/owner-domain.js";

const day = "2030-03-20";
const now = Date.parse("2030-03-19T00:00:00Z");
const range = (start, end, extra = {}) => ({
  startsAt: `${day}T${start}:00Z`,
  endsAt: `${day}T${end}:00Z`,
  ...extra,
});
const hours = (schedule) =>
  schedule.free.map((interval) => [
    new Date(interval.start).toISOString(),
    new Date(interval.end).toISOString(),
  ]);

test("owner availability merges overlapping reservations and closures while releasing expired holds", () => {
  const bookings = [
    range("10:00", "11:00", { status: "CONFIRMED" }),
    range("11:00", "12:00", {
      status: "PENDING_PAYMENT",
      holdExpiresAt: "2030-03-18T00:00:00Z",
    }),
    range("13:00", "14:00", { status: "CANCELLED" }),
    range("15:00", "16:00", {
      status: "PENDING_PAYMENT",
      holdExpiresAt: "2030-03-21T00:00:00Z",
    }),
  ];
  const blocks = [
    range("10:30", "12:00"),
    range("13:00", "14:00", { cancelledAt: "2030-03-18T00:00:00Z" }),
  ];
  const schedule = courtDaySchedule(
    { active: true },
    bookings,
    blocks,
    day,
    "UTC",
    now,
  );
  assert.equal(schedule.bookings.length, 2);
  assert.equal(schedule.blocks.length, 1);
  assert.deepEqual(hours(schedule), [
    ["2030-03-20T00:00:00.000Z", "2030-03-20T10:00:00.000Z"],
    ["2030-03-20T12:00:00.000Z", "2030-03-20T15:00:00.000Z"],
    ["2030-03-20T16:00:00.000Z", "2030-03-21T00:00:00.000Z"],
  ]);
});

test("owner calendars use the venue day and exclude bookings ending at its midnight", () => {
  const bookings = [
    {
      startsAt: "2030-03-19T16:00:00Z",
      endsAt: "2030-03-19T17:00:00Z",
      status: "CONFIRMED",
    },
  ];
  const schedule = courtDaySchedule(
    { active: true },
    bookings,
    [],
    day,
    "Asia/Ho_Chi_Minh",
    now,
  );
  assert.equal(schedule.bookings.length, 0);
  assert.deepEqual(hours(schedule), [
    ["2030-03-19T17:00:00.000Z", "2030-03-20T17:00:00.000Z"],
  ]);
});

test("inactive courts and past days have no available windows", () => {
  assert.deepEqual(
    courtDaySchedule({ active: false }, [], [], day, "UTC", now).free,
    [],
  );
  assert.deepEqual(
    courtDaySchedule(
      { active: true },
      [],
      [],
      day,
      "UTC",
      Date.parse("2030-03-21T00:00:00Z"),
    ).free,
    [],
  );
  assert.equal(isEffectiveBooking({ status: "EXPIRED" }, now), false);
});

test("free windows start in the future and fit complete quarter-hour booking intervals", () => {
  const schedule = courtDaySchedule(
    { active: true },
    [],
    [range("10:07", "10:13")],
    day,
    "UTC",
    Date.parse("2030-03-20T09:07:00Z"),
  );
  assert.deepEqual(hours(schedule), [
    ["2030-03-20T09:15:00.000Z", "2030-03-20T10:00:00.000Z"],
    ["2030-03-20T10:15:00.000Z", "2030-03-21T00:00:00.000Z"],
  ]);
});
