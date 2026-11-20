import { addDays, zonedInstant } from "./domain.js";

export function isEffectiveBooking(booking, now = Date.now()) {
  if (booking.status === "CONFIRMED" || booking.status === "COMPLETED")
    return true;
  return (
    booking.status === "PENDING_PAYMENT" &&
    (!booking.holdExpiresAt || Date.parse(booking.holdExpiresAt) > now)
  );
}

export function courtDaySchedule(
  court,
  bookings,
  blocks,
  day,
  zone,
  now = Date.now(),
) {
  const dayStart = Date.parse(zonedInstant(day, "00:00", zone));
  const dayEnd = Date.parse(zonedInstant(addDays(day, 1), "00:00", zone));
  const inDay = (item) =>
    Date.parse(item.startsAt) < dayEnd && Date.parse(item.endsAt) > dayStart;
  const reservations = bookings.filter(
    (booking) => isEffectiveBooking(booking, now) && inDay(booking),
  );
  const closures = blocks.filter((block) => !block.cancelledAt && inDay(block));
  const occupied = [...reservations, ...closures]
    .map((item) => ({
      start: Math.max(dayStart, Date.parse(item.startsAt)),
      end: Math.min(dayEnd, Date.parse(item.endsAt)),
    }))
    .sort((left, right) => left.start - right.start);
  const free = [];
  const quarterHour = 15 * 60000;
  let cursor = Math.max(dayStart, now);
  if (court.active) {
    for (const interval of occupied) {
      if (interval.start > cursor)
        free.push({ start: cursor, end: interval.start });
      cursor = Math.max(cursor, interval.end);
    }
    if (cursor < dayEnd) free.push({ start: cursor, end: dayEnd });
  }
  return {
    bookings: reservations.sort(
      (left, right) => Date.parse(left.startsAt) - Date.parse(right.startsAt),
    ),
    blocks: closures,
    free: free
      .map((interval) => ({
        start: Math.ceil(interval.start / quarterHour) * quarterHour,
        end: Math.floor(interval.end / quarterHour) * quarterHour,
      }))
      .filter((interval) => interval.end - interval.start >= quarterHour),
    dayEnd,
  };
}
