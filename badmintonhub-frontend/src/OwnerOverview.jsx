import React, { useEffect, useState } from "react";
import { api } from "./api.js";
import { dateText, localDate, money, timeText } from "./domain.js";
import { courtDaySchedule } from "./owner-domain.js";
import {
  Badge,
  Button,
  DataState,
  Empty,
  ErrorBox,
  Field,
  useApp,
} from "./ui.jsx";

export default function OwnerOverview({ venues }) {
  const { navigate, revision } = useApp();
  const [data, setData] = useState({ value: [], loading: true, error: null });
  const [version, setVersion] = useState(0);
  const [venueId, setVenueId] = useState("");
  const [day, setDay] = useState(localDate());
  const [now, setNow] = useState(Date.now());
  const [updatedAt, setUpdatedAt] = useState(null);
  const reload = () => setVersion((value) => value + 1);
  useEffect(() => {
    const clock = setInterval(() => setNow(Date.now()), 1000);
    const polling = setInterval(reload, 60000);
    const onFocus = () => reload();
    window.addEventListener("focus", onFocus);
    return () => {
      clearInterval(clock);
      clearInterval(polling);
      window.removeEventListener("focus", onFocus);
    };
  }, []);
  useEffect(() => {
    if (venues.loading || venues.error) return;
    const controller = new AbortController();
    let active = true;
    setData((previous) => ({ ...previous, loading: true, error: null }));
    async function load() {
      const groups = await Promise.all(
        venues.value.map(async (venue) => {
          const courts = await api.all(
            `/venues/${venue.id}/courts?activeOnly=false`,
            controller.signal,
          );
          return courts.map((court) => ({ court, venue }));
        }),
      );
      const rows = await Promise.all(
        groups.flat().map(async (row) => {
          const [bookings, blocks] = await Promise.all([
            api.all(
              `/owner/courts/${row.court.id}/bookings`,
              controller.signal,
            ),
            api.all(`/courts/${row.court.id}/blocks`, controller.signal),
          ]);
          return { ...row, bookings, blocks };
        }),
      );
      if (active) {
        setData({ value: rows, loading: false, error: null });
        setUpdatedAt(Date.now());
      }
    }
    load().catch((error) => {
      if (active && error.name !== "AbortError")
        setData({ value: [], loading: false, error });
    });
    return () => {
      active = false;
      controller.abort();
    };
  }, [venues.value, venues.loading, venues.error, revision, version]);

  const visible = data.value.filter(
    (row) => !venueId || row.venue.id === venueId,
  );
  let schedules = [];
  let dateError = null;
  try {
    if (!day) throw new Error("Chọn ngày để xem lịch sân.");
    schedules = visible
      .map((row) => ({
        ...row,
        schedule: courtDaySchedule(
          row.court,
          row.bookings,
          row.blocks,
          day,
          row.venue.timeZone,
          now,
        ),
      }))
      .sort(
        (left, right) =>
          left.venue.name.localeCompare(right.venue.name, "vi") ||
          left.court.name.localeCompare(right.court.name, "vi", {
            numeric: true,
          }),
      );
  } catch (error) {
    dateError = error;
  }
  const dailyBookings = schedules.flatMap((row) => row.schedule.bookings);
  const customers = new Set(dailyBookings.map((booking) => booking.customerId))
    .size;

  return (
    <DataState data={venues}>
      <DataState
        data={{ ...data, loading: data.loading && !updatedAt, reload }}
      >
        <div className="owner-metrics">
          <div className="stat">
            <small>CỤM SÂN ĐÃ ĐĂNG</small>
            <strong>{venues.value.length}</strong>
          </div>
          <div className="stat">
            <small>TỔNG SỐ SÂN</small>
            <strong>{data.value.length}</strong>
            <span>
              {data.value.filter((row) => row.court.active).length} sân đang
              hoạt động
            </span>
          </div>
          <div className="stat">
            <small>LƯỢT ĐẶT TRONG NGÀY ĐÃ CHỌN</small>
            <strong>{dateError ? "—" : dailyBookings.length}</strong>
            <span>Theo bộ lọc cụm sân</span>
          </div>
          <div className="stat">
            <small>KHÁCH ĐẶT TRONG NGÀY ĐÃ CHỌN</small>
            <strong>{dateError ? "—" : customers}</strong>
            <span>Mỗi khách được tính một lần</span>
          </div>
        </div>
        <div className="owner-calendar-heading">
          <div>
            <h2>Lịch sân và khung giờ trống</h2>
            <p className="muted small">
              Giờ theo múi giờ từng cụm sân. Lịch đã hủy và giữ chỗ hết hạn được
              giải phóng.
            </p>
          </div>
          <Button
            className="secondary compact"
            loading={data.loading}
            onClick={reload}
          >
            Cập nhật lịch
          </Button>
        </div>
        <div className="owner-calendar-filters">
          <Field label="Cụm sân xem lịch">
            <select
              value={venueId}
              onChange={(event) => setVenueId(event.target.value)}
            >
              <option value="">Tất cả cụm sân của bạn</option>
              {venues.value.map((venue) => (
                <option key={venue.id} value={venue.id}>
                  {venue.name}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Ngày xem lịch">
            <input
              type="date"
              value={day}
              onChange={(event) => setDay(event.target.value)}
              required
            />
          </Field>
        </div>
        <ErrorBox error={dateError} />
        {!dateError &&
          (schedules.length ? (
            <div className="stack">
              {schedules.map(({ court, venue, schedule }) => (
                <article className="owner-schedule panel" key={court.id}>
                  <div className="owner-schedule-heading">
                    <div>
                      <p className="eyebrow">{venue.name}</p>
                      <h3>{court.name}</h3>
                      <p className="fineprint">
                        {venue.timeZone} · {money(court.baseHourlyRateVnd)} /
                        giờ
                      </p>
                    </div>
                    <Badge tone={court.active ? "green" : "amber"}>
                      {court.active ? "Đang hoạt động" : "Tạm ngưng"}
                    </Badge>
                  </div>
                  <h4>Khung giờ trống</h4>
                  {schedule.free.length ? (
                    <div className="free-windows">
                      {schedule.free.map((interval) => (
                        <span key={interval.start}>
                          {timeText(interval.start, venue.timeZone)} –{" "}
                          {interval.end === schedule.dayEnd
                            ? "24:00"
                            : timeText(interval.end, venue.timeZone)}
                        </span>
                      ))}
                    </div>
                  ) : (
                    <p className="muted small">
                      {!court.active
                        ? "Sân đang tạm ngưng hoạt động."
                        : schedule.dayEnd <= now
                          ? "Ngày này đã qua."
                          : "Đã kín lịch trong thời gian còn lại của ngày."}
                    </p>
                  )}
                  {schedule.blocks.map((block) => (
                    <p className="owner-closure" key={block.id}>
                      Khóa sân · {dateText(block.startsAt, venue.timeZone)}{" "}
                      {timeText(block.startsAt, venue.timeZone)} –{" "}
                      {dateText(block.endsAt, venue.timeZone)}{" "}
                      {timeText(block.endsAt, venue.timeZone)} · {block.reason}
                    </p>
                  ))}
                  <h4>Lịch khách đã đặt · {schedule.bookings.length} lượt</h4>
                  {schedule.bookings.length ? (
                    <div className="owner-reservations">
                      {schedule.bookings.map((booking) => (
                        <div className="owner-reservation" key={booking.id}>
                          <div>
                            <strong>
                              {dateText(booking.startsAt, venue.timeZone)} ·{" "}
                              {timeText(booking.startsAt, venue.timeZone)} –{" "}
                              {timeText(booking.endsAt, venue.timeZone)}
                            </strong>
                            <p className="fineprint">
                              Mã khách{" "}
                              {booking.customerId.slice(0, 8).toUpperCase()} ·
                              Đơn {booking.id.slice(0, 8).toUpperCase()}
                            </p>
                          </div>
                          <Badge status={booking.status} />
                          <Button
                            className="secondary compact"
                            onClick={() => navigate(`/bookings/${booking.id}`)}
                          >
                            Xem lịch đặt
                          </Button>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="muted small">
                      Chưa có lịch đặt hiệu lực trong ngày này.
                    </p>
                  )}
                </article>
              ))}
            </div>
          ) : (
            <Empty
              title="Chưa có sân để xem lịch"
              text="Thêm cụm sân và sân cầu lông trong các mục quản lý."
            />
          ))}
        <p className="fineprint section-note">
          Khung giờ hiển thị theo ngày 00:00–24:00, chia theo bước 15 phút. Thời
          gian đã qua được loại khỏi khung giờ trống. Lịch tự cập nhật mỗi phút
          {updatedAt ? ` · Cập nhật lúc ${timeText(updatedAt)}` : ""}.
        </p>
      </DataState>
    </DataState>
  );
}
