export const money = (value) =>
  new Intl.NumberFormat("vi-VN").format(value || 0) + "đ";
export const roles = {
  CUSTOMER: "Người chơi",
  OWNER: "Chủ sân",
  ADMIN: "Quản trị viên",
};
export const statuses = {
  PENDING_PAYMENT: ["Chờ thanh toán", "amber"],
  CONFIRMED: ["Đã xác nhận", "green"],
  EXPIRED: ["Hết hạn giữ chỗ", "muted"],
  CANCELLED: ["Đã hủy", "muted"],
  COMPLETED: ["Đã hoàn thành", "green"],
  PENDING: ["Đang chờ", "amber"],
  SUCCEEDED: ["Thành công", "green"],
  FAILED: ["Thất bại", "red"],
  REFUNDED: ["Đã hoàn tiền", "muted"],
};
export const dateText = (value, zone = "Asia/Ho_Chi_Minh") =>
  new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    timeZone: zone,
  }).format(new Date(value));
export const timeText = (value, zone = "Asia/Ho_Chi_Minh") =>
  new Intl.DateTimeFormat("vi-VN", {
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
    timeZone: zone,
  }).format(new Date(value));
export function localDate(value = new Date(), zone = "Asia/Ho_Chi_Minh") {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: zone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(value);
  const get = (key) => parts.find((part) => part.type === key).value;
  return `${get("year")}-${get("month")}-${get("day")}`;
}
export function addDays(date, days) {
  const value = new Date(`${date}T12:00:00Z`);
  value.setUTCDate(value.getUTCDate() + days);
  return value.toISOString().slice(0, 10);
}

// Convert the selected venue-local wall clock to UTC; never assume the browser's time zone.
export function zonedInstant(date, time, zone = "Asia/Ho_Chi_Minh") {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date) || !/^\d{2}:\d{2}$/.test(time))
    throw new Error("Ngày hoặc giờ chưa hợp lệ.");
  const [year, month, day] = date.split("-").map(Number);
  const [hour, minute] = time.split(":").map(Number);
  if (hour > 23 || minute > 59) throw new Error("Giờ chưa hợp lệ.");
  const desired = Date.UTC(year, month - 1, day, hour, minute);
  if (new Date(desired).toISOString().slice(0, 10) !== date)
    throw new Error("Ngày chưa hợp lệ.");
  const formatter = new Intl.DateTimeFormat("en-CA", {
    timeZone: zone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  });
  let instant = desired;
  for (let pass = 0; pass < 3; pass++) {
    const pieces = Object.fromEntries(
      formatter
        .formatToParts(new Date(instant))
        .map((part) => [part.type, part.value]),
    );
    const observed = Date.UTC(
      +pieces.year,
      +pieces.month - 1,
      +pieces.day,
      +pieces.hour,
      +pieces.minute,
    );
    const delta = desired - observed;
    if (delta === 0) return new Date(instant).toISOString();
    instant += delta;
  }
  throw new Error("Khung giờ này không tồn tại trong múi giờ của cụm sân.");
}
export function bookingRange(date, start, end, zone, now = Date.now()) {
  const startsAt = zonedInstant(date, start, zone);
  const endsAt = zonedInstant(date, end, zone);
  if (Date.parse(startsAt) <= now)
    throw new Error("Giờ bắt đầu phải ở trong tương lai.");
  if (Date.parse(endsAt) <= Date.parse(startsAt))
    throw new Error("Giờ kết thúc phải sau giờ bắt đầu trong cùng ngày.");
  return { startsAt, endsAt };
}
export const overlaps = (start, end, otherStart, otherEnd) =>
  Date.parse(start) < Date.parse(otherEnd) &&
  Date.parse(end) > Date.parse(otherStart);
export function remaining(until, now = Date.now()) {
  if (!until || !Number.isFinite(Date.parse(until))) return "00:00";
  const seconds = Math.max(0, Math.ceil((Date.parse(until) - now) / 1000));
  return `${String(Math.floor(seconds / 60)).padStart(2, "0")}:${String(seconds % 60).padStart(2, "0")}`;
}
export function estimatePrice(court, rules, startsAt, endsAt, zone) {
  let amount = 0;
  for (
    let current = Date.parse(startsAt);
    current < Date.parse(endsAt);
    current += 15 * 60000
  ) {
    const end = Math.min(current + 15 * 60000, Date.parse(endsAt));
    const date = localDate(new Date(current), zone);
    const time = timeText(current, zone);
    const weekday = ((new Date(`${date}T12:00:00Z`).getUTCDay() + 6) % 7) + 1;
    const rule = [...rules]
      .sort((a, b) => a.startLocalTime.localeCompare(b.startLocalTime))
      .find(
        (item) =>
          item.dayOfWeek === weekday &&
          date >= item.validFrom &&
          (!item.validUntil || date < item.validUntil) &&
          time >= item.startLocalTime.slice(0, 5) &&
          time < item.endLocalTime.slice(0, 5),
      );
    amount += Math.floor(
      ((rule?.hourlyRateVnd ?? court.baseHourlyRateVnd) *
        ((end - current) / 1000)) /
        3600,
    );
  }
  return amount;
}
