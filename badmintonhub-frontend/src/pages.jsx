import React, { useEffect, useState } from "react";
import {
  ArrowRight,
  ChevronRight,
  Clock3,
  Eye,
  EyeOff,
  MapPin,
  Search,
  ShieldCheck,
} from "lucide-react";
import { api, auth } from "./api.js";
import {
  addDays,
  bookingRange,
  dateText,
  estimatePrice,
  localDate,
  money,
  overlaps,
  remaining,
  roles,
  timeText,
} from "./domain.js";
import {
  Back,
  Badge,
  Button,
  Confirm,
  DataState,
  Empty,
  ErrorBox,
  Field,
  Loading,
  Modal,
  Pagination,
  useApp,
  useCollection,
  useData,
} from "./ui.jsx";

const PREVIEW =
  import.meta.env.DEV && import.meta.env.VITE_PREVIEW_MODE === "true";
export function AuthPage({ register }) {
  const { navigate, notify } = useApp();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [visible, setVisible] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  useEffect(() => {
    setError(null);
  }, [register]);
  async function submit(event) {
    event.preventDefault();
    setError(null);
    if (new TextEncoder().encode(password).length > 72) {
      setError(new Error("Mật khẩu không được dài quá 72 byte UTF-8."));
      return;
    }
    setBusy(true);
    try {
      if (register)
        await auth.register({
          email: email.trim(),
          password,
          fullName: name.trim(),
          phone: phone.trim() || null,
        });
      else await auth.login({ email: email.trim(), password });
      navigate(auth.getSession().user.role === "OWNER" ? "/owner" : "/venues");
      notify(
        register
          ? "Tạo tài khoản thành công. Hẹn bạn trên sân!"
          : "Chào mừng bạn trở lại!",
      );
    } catch (error) {
      setError(error);
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="auth-layout">
      <section className="brand-panel">
        <span className="lime-label">DÀNH CHO NGƯỜI YÊU CẦU LÔNG</span>
        <h1>
          Trận hay bắt đầu
          <br />
          từ một lịch hẹn.
        </h1>
        <p>
          Tìm sân phù hợp, chọn giờ yêu thích và sẵn sàng cho trận cầu tiếp
          theo.
        </p>
        <div className="play-slogan">LET’S PLAY.</div>
        <span className="slogan-note">CÙNG NHAU RA SÂN</span>
        <div className="brand-steps">
          {[
            ["01", "Tìm cụm sân"],
            ["02", "Chọn lịch"],
            ["03", "Giữ chỗ"],
          ].map(([number, label]) => (
            <div key={number}>
              <strong>{number}</strong>
              <span>{label}</span>
            </div>
          ))}
        </div>
        <small>Dễ tìm sân · Rõ giá tiền · Chủ động lịch chơi</small>
      </section>
      <section className="auth-form-panel">
        <span className="eyebrow">
          {register ? "BẮT ĐẦU MỘT LỊCH HẸN MỚI" : "CHÀO MỪNG TRỞ LẠI"}
        </span>
        <h2>{register ? "Tạo tài khoản" : "Đăng nhập"}</h2>
        <p className="muted">
          {register
            ? "Gia nhập cộng đồng. Hẹn nhau trên sân."
            : "Đăng nhập để xem sân và đặt lịch của bạn."}
        </p>
        <form onSubmit={submit} className="stack form-stack">
          {register && (
            <Field label="Họ và tên" error={error?.fields?.fullName}>
              <input
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Nguyễn Minh Anh"
                autoComplete="name"
                required
                maxLength={120}
              />
            </Field>
          )}
          <Field label="Email" error={error?.fields?.email}>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="minhanh@example.com"
              autoComplete="email"
              required
              maxLength={254}
            />
          </Field>
          <Field
            label="Mật khẩu"
            hint={register ? "Từ 8 ký tự, tối đa 72 byte UTF-8." : null}
            error={error?.fields?.password}
          >
            <span
              className={`password-field ${error?.code === "INVALID_CREDENTIALS" ? "invalid" : ""}`}
            >
              <input
                aria-label="Mật khẩu"
                type={visible ? "text" : "password"}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Nhập mật khẩu của bạn"
                autoComplete={register ? "new-password" : "current-password"}
                required
                minLength={register ? 8 : 1}
                maxLength={72}
                aria-invalid={error?.code === "INVALID_CREDENTIALS"}
              />
              <button
                type="button"
                onClick={() => setVisible((v) => !v)}
                aria-label={visible ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
              >
                {visible ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </span>
          </Field>
          {register && (
            <Field label="Số điện thoại (không bắt buộc)">
              <input
                type="tel"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="0901 234 567"
                autoComplete="tel"
                maxLength={30}
              />
            </Field>
          )}
          {error && (
            <p className="auth-error" role="alert">
              {error.message}
            </p>
          )}
          <Button className="full" loading={busy} type="submit">
            {busy ? "Đang xử lý…" : register ? "Đăng ký" : "Đăng nhập"}
          </Button>
        </form>
        <p className="auth-switch">
          {register ? "Bạn đã có tài khoản?" : "Bạn chưa có tài khoản?"}{" "}
          <button
            className="text-button"
            onClick={() => navigate(register ? "/login" : "/register")}
          >
            {register ? "Đăng nhập" : "Đăng ký ngay"}
          </button>
        </p>
        <div className="login-note">
          <strong>
            <ShieldCheck size={14} />
            Đăng nhập để xem sân
          </strong>
          <span>Thông tin sân và lịch trống hiển thị sau khi đăng nhập.</span>
        </div>
        {PREVIEW && (
          <div className="preview-accounts">
            <small>TÀI KHOẢN TRẢI NGHIỆM</small>
            <div>
              {[
                ["minhanh", "Người chơi"],
                ["chusan", "Chủ sân"],
                ["admin", "Admin"],
              ].map(([id, label]) => (
                <button
                  key={id}
                  type="button"
                  onClick={() => {
                    setEmail(`${id}@badmintonhub.test`);
                    setPassword(import.meta.env.VITE_PREVIEW_PASSWORD || "");
                    setError(null);
                  }}
                >
                  {label}
                  <ArrowRight size={12} />
                </button>
              ))}
            </div>
            <span>
              Chọn vai trò để điền tài khoản mẫu, sau đó nhấn Đăng nhập.
            </span>
          </div>
        )}
      </section>
    </div>
  );
}

function VenueCard({ venue }) {
  const { navigate } = useApp();
  const courts = useCollection(`/venues/${venue.id}/courts?activeOnly=false`);
  const active = courts.value.filter((item) => item.active);
  const minimum = active.length
    ? Math.min(...active.map((item) => item.baseHourlyRateVnd))
    : null;
  return (
    <article className="venue-card">
      <div className="venue-symbol" aria-hidden="true">
        <span />
        <span />
      </div>
      <div>
        <span className="eyebrow">HẸN NHAU TRÊN SÂN</span>
        <h2>{venue.name}</h2>
        <p className="address">
          <MapPin size={15} />
          {venue.address}
        </p>
      </div>
      <div className="venue-card-meta">
        <span>
          {courts.loading
            ? "Đang tải sân…"
            : courts.error
              ? "Chưa tải được thông tin sân"
              : `${active.length} sân đang hoạt động`}
        </span>
        <strong>
          {minimum === null ? "Xem thông tin" : `${money(minimum)} / giờ`}
        </strong>
      </div>
      <Button
        className="secondary full"
        onClick={() => navigate(`/venues/${venue.id}`)}
      >
        Xem cụm sân
        <ArrowRight size={15} />
      </Button>
    </article>
  );
}
export function VenueList() {
  const venues = useCollection("/venues");
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const filtered = venues.value
    .filter((item) =>
      `${item.name} ${item.address}`
        .toLocaleLowerCase("vi")
        .includes(query.toLocaleLowerCase("vi")),
    )
    .sort((a, b) => a.name.localeCompare(b.name, "vi"));
  const count = 9;
  const sliced = filtered.slice(page * count, (page + 1) * count);
  return (
    <>
      <div className="page-heading">
        <div>
          <span className="eyebrow">MỘT LỊCH HẸN. THÊM MỘT TRẬN HAY.</span>
          <h1>Hẹn nhau ra sân.</h1>
          <p className="muted">
            Chọn địa điểm phù hợp, dành thời gian cho trận cầu bạn yêu.
          </p>
        </div>
      </div>
      <div className="search-bar">
        <Search size={19} />
        <input
          aria-label="Tìm cụm sân"
          placeholder="Tìm theo tên cụm sân hoặc địa chỉ…"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setPage(0);
          }}
        />
        <span>{filtered.length} cụm sân</span>
      </div>
      <DataState data={venues}>
        {sliced.length ? (
          <div className="venue-grid">
            {sliced.map((venue) => (
              <VenueCard key={venue.id} venue={venue} />
            ))}
          </div>
        ) : (
          <Empty
            title="Chưa tìm thấy cụm sân"
            text={
              query
                ? "Thử một tên hoặc địa chỉ khác nhé."
                : "Các địa điểm sẽ xuất hiện khi chủ sân tạo cụm sân."
            }
          />
        )}
      </DataState>
      <Pagination
        data={{ page: { totalPages: Math.ceil(filtered.length / count) } }}
        page={page}
        setPage={setPage}
      />
      <div className="list-note">
        <Clock3 size={15} />
        <span>
          Giá có thể thay đổi theo ngày và khung giờ. Bạn sẽ thấy tổng tiền xác
          nhận khi tạo lịch đặt.
        </span>
      </div>
    </>
  );
}

export function VenueDetail({ id }) {
  const { navigate } = useApp();
  const venue = useData(`/venues/${id}`);
  const courts = useCollection(`/venues/${id}/courts?activeOnly=false`);
  if (venue.loading) return <Loading />;
  if (venue.error)
    return (
      <>
        <Back />
        <ErrorBox error={venue.error} retry={venue.reload} />
      </>
    );
  const item = venue.value;
  const orderedCourts = [...courts.value].sort((left, right) =>
    left.name.localeCompare(right.name, "vi", { numeric: true }),
  );
  const active = courts.value.filter((c) => c.active);
  const minimum = courts.value.length
    ? Math.min(...courts.value.map((c) => c.baseHourlyRateVnd))
    : null;
  const offset = new Intl.DateTimeFormat("en", {
    timeZone: item.timeZone,
    timeZoneName: "shortOffset",
  })
    .formatToParts(new Date())
    .find((p) => p.type === "timeZoneName")
    ?.value.replace("GMT", "UTC");
  return (
    <>
      <div className="breadcrumb">
        <button onClick={() => navigate("/venues")}>Khám phá sân</button>
        <span>/</span>
        <span>{item.name}</span>
      </div>
      <div className="page-heading detail-heading">
        <div>
          <h1>{item.name}</h1>
          <p className="muted">{item.address}</p>
        </div>
        <Button
          disabled={!active.length}
          onClick={() => navigate(`/venues/${id}/book`)}
        >
          Chọn lịch đặt sân
        </Button>
      </div>
      <div className="stats-row">
        <div className="stat">
          <small>SÂN CẦU LÔNG</small>
          <strong>
            {courts.loading
              ? "Đang tải…"
              : `${String(courts.value.length).padStart(2, "0")} sân tại cụm`}
          </strong>
        </div>
        <div className="stat">
          <small>GIÁ CƠ BẢN</small>
          <strong>
            {minimum === null ? "Chưa có giá" : `Từ ${money(minimum)} / giờ`}
          </strong>
        </div>
        <div className="stat">
          <small>MÚI GIỜ</small>
          <strong>
            {item.timeZone} · {offset}
          </strong>
        </div>
      </div>
      <div className="detail-grid">
        <section>
          <div className="section-heading">
            <h2>Chọn sân của bạn</h2>
            <span>
              {active.length} sân hoạt động ·{" "}
              {courts.value.length - active.length} sân tạm ngưng
            </span>
          </div>
          <DataState data={courts}>
            {courts.value.length ? (
              <div className="stack">
                {orderedCourts.map((court) => (
                  <article key={court.id} className="court-row">
                    <div className="court-copy">
                      <div className="inline">
                        <h3>{court.name}</h3>
                        <Badge tone={court.active ? "green" : "amber"}>
                          {court.active ? "Đang hoạt động" : "Không hoạt động"}
                        </Badge>
                      </div>
                      <p>
                        {court.active
                          ? "Chọn khung giờ phù hợp với lịch của bạn"
                          : "Sân đang tạm ngưng hoạt động"}
                      </p>
                    </div>
                    <strong className="court-price">
                      {money(court.baseHourlyRateVnd)} / giờ
                    </strong>
                    <Button
                      className={court.active ? "" : "secondary"}
                      disabled={!court.active}
                      onClick={() =>
                        navigate(`/venues/${id}/book?court=${court.id}`)
                      }
                    >
                      {court.active ? "Đặt sân" : "Tạm ngưng"}
                    </Button>
                  </article>
                ))}
              </div>
            ) : (
              <Empty
                title="Cụm sân chưa có sân"
                text="Vui lòng quay lại sau khi chủ sân cập nhật."
              />
            )}
          </DataState>
          <p className="fineprint section-note">
            Giá có thể thay đổi theo ngày và khung giờ. Vui lòng xem giá dự kiến
            ở bước tiếp theo.
          </p>
        </section>
        <aside className="stack">
          <div className="green-note">
            <span className="lime-label">HẸN NHAU TRÊN SÂN</span>
            <h2>
              Chọn giờ phù hợp.
              <br />
              Giữ chỗ trong 15 phút.
            </h2>
            <p>
              Giá hiển thị theo giờ là giá cơ bản. Tổng tiền chính xác được xác
              nhận khi bạn tạo lịch đặt.
            </p>
          </div>
          <div className="info-card">
            <h3>Thông tin cần biết</h3>
            <ol>
              <li>Giờ đặt theo múi giờ của cụm sân.</li>
              <li>Kiểm tra thời gian sân bị khóa.</li>
              <li>Thanh toán sau khi tạo lịch đặt.</li>
            </ol>
          </div>
        </aside>
      </div>
    </>
  );
}

export function BookingCreate({ venueId }) {
  const { navigate, refresh, notify } = useApp();
  const venue = useData(`/venues/${venueId}`);
  const courts = useCollection(`/venues/${venueId}/courts?activeOnly=false`);
  const initialCourt =
    new URLSearchParams(window.location.hash.split("?")[1] || "").get(
      "court",
    ) || "";
  const [courtId, setCourtId] = useState(initialCourt);
  const [day, setDay] = useState(localDate());
  const [start, setStart] = useState("18:00");
  const [end, setEnd] = useState("19:00");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  useEffect(() => {
    if (!courtId && courts.value.some((c) => c.active))
      setCourtId(courts.value.find((c) => c.active).id);
  }, [courts.value, courtId]);
  const rules = useData(
    courtId ? `/courts/${courtId}/pricing-rules` : null,
    [],
  );
  const blocks = useCollection(courtId ? `/courts/${courtId}/blocks` : null);
  const selected = courts.value.find((c) => c.id === courtId);
  const zone = venue.value?.timeZone || "Asia/Ho_Chi_Minh";
  let range = null;
  let rangeError = null;
  try {
    range = bookingRange(day, start, end, zone);
  } catch (error) {
    rangeError = error.message;
  }
  const conflict =
    range &&
    blocks.value.some(
      (b) =>
        !b.cancelledAt &&
        overlaps(range.startsAt, range.endsAt, b.startsAt, b.endsAt),
    );
  const dayBlocks = blocks.value.filter(
    (block) =>
      localDate(new Date(block.startsAt), zone) <= day &&
      localDate(new Date(Date.parse(block.endsAt) - 1), zone) >= day,
  );
  const estimate =
    range && selected && !rules.error
      ? estimatePrice(
          selected,
          rules.value || [],
          range.startsAt,
          range.endsAt,
          zone,
        )
      : null;
  async function submit(event) {
    event.preventDefault();
    setError(null);
    if (rangeError) {
      setError(new Error(rangeError));
      return;
    }
    setBusy(true);
    try {
      const booking = await api.post("/bookings", { courtId, ...range });
      refresh();
      notify("Đã giữ chỗ. Kiểm tra thông tin và thời hạn thanh toán.");
      navigate(`/bookings/${booking.id}`);
    } catch (error) {
      setError(error);
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <Back to={`/venues/${venueId}`}>Thông tin cụm sân</Back>
      <div className="page-heading">
        <div>
          <span className="eyebrow">
            01 CHỌN LỊCH · 02 GIỮ CHỖ · 03 THEO DÕI
          </span>
          <h1>Chọn một lịch hẹn.</h1>
          <p className="muted">{venue.value?.name || "Đang tải cụm sân…"}</p>
        </div>
      </div>
      <DataState data={venue}>
        <div className="detail-grid">
          <form className="panel stack" onSubmit={submit}>
            <h2>Thông tin đặt sân</h2>
            <DataState data={courts}>
              <Field label="Sân cầu lông">
                <select
                  required
                  value={courtId}
                  onChange={(e) => {
                    setCourtId(e.target.value);
                    setError(null);
                  }}
                >
                  <option value="" disabled>
                    Chọn sân
                  </option>
                  {courts.value
                    .filter((c) => c.active)
                    .map((c) => (
                      <option value={c.id} key={c.id}>
                        {c.name} · {money(c.baseHourlyRateVnd)} / giờ
                      </option>
                    ))}
                </select>
              </Field>
            </DataState>
            <Field label="Ngày chơi">
              <input
                type="date"
                required
                min={localDate(new Date(), zone)}
                value={day}
                onChange={(e) => setDay(e.target.value)}
              />
            </Field>
            <div className="form-grid">
              <Field label="Giờ bắt đầu">
                <input
                  type="time"
                  required
                  step={900}
                  value={start}
                  onChange={(e) => setStart(e.target.value)}
                />
              </Field>
              <Field label="Giờ kết thúc">
                <input
                  type="time"
                  required
                  step={900}
                  value={end}
                  onChange={(e) => setEnd(e.target.value)}
                />
              </Field>
            </div>
            <p className="fineprint">
              Múi giờ: {zone}. Đặt và kết thúc trong cùng ngày.
            </p>
            {rangeError && <p className="field-error">{rangeError}</p>}
            {conflict && (
              <ErrorBox
                error={
                  new Error(
                    "Sân bị khóa trong khung giờ này. Vui lòng chọn thời gian khác.",
                  )
                }
              />
            )}
            <ErrorBox error={rules.error} retry={rules.reload} />
            <ErrorBox error={blocks.error} retry={blocks.reload} />
            <ErrorBox error={error} />
            <Button
              type="submit"
              className="full"
              loading={busy}
              disabled={
                !selected ||
                !!rangeError ||
                conflict ||
                rules.loading ||
                blocks.loading ||
                !!rules.error ||
                !!blocks.error
              }
            >
              Xác nhận giữ chỗ
              <ArrowRight size={16} />
            </Button>
            <p className="fineprint">
              Hệ thống sẽ kiểm tra lại lịch trống trước khi xác nhận.
            </p>
          </form>
          <aside className="stack">
            <div className="green-note">
              <span className="lime-label">LỊCH HẸN CỦA BẠN</span>
              <h2>{selected?.name || "Chọn sân để bắt đầu"}</h2>
              <p>{venue.value?.address}</p>
              <dl className="summary dark">
                <div>
                  <dt>Ngày chơi</dt>
                  <dd>{day.split("-").reverse().join("/")}</dd>
                </div>
                <div>
                  <dt>Khung giờ</dt>
                  <dd>
                    {start} – {end}
                  </dd>
                </div>
                <div>
                  <dt>Giá dự kiến</dt>
                  <dd>{estimate === null ? "—" : money(estimate)}</dd>
                </div>
              </dl>
              <small>
                Giá cuối cùng do backend xác nhận sau khi tạo lịch đặt.
              </small>
            </div>
            <div className="info-card">
              <h3>Lịch khóa sân</h3>
              {blocks.loading ? (
                <Loading />
              ) : dayBlocks.length ? (
                dayBlocks.map((b) => (
                  <p key={b.id} className="block-note">
                    <strong>
                      {dateText(b.startsAt, zone)} ·{" "}
                      {timeText(b.startsAt, zone)} – {dateText(b.endsAt, zone)}{" "}
                      · {timeText(b.endsAt, zone)}
                    </strong>
                    <span>{b.reason}</span>
                  </p>
                ))
              ) : (
                <p className="muted small">
                  Chưa có lịch khóa được ghi nhận trong ngày này. Đây chưa phải
                  xác nhận sân còn trống.
                </p>
              )}
            </div>
          </aside>
        </div>
      </DataState>
    </>
  );
}

export function BookingList() {
  const { navigate } = useApp();
  const [page, setPage] = useState(0);
  const [filter, setFilter] = useState("ALL");
  const data = useData(`/bookings/me?page=${page}&size=10`);
  const list = (data.value?.content || []).filter(
    (b) => filter === "ALL" || b.status === filter,
  );
  return (
    <>
      <div className="page-heading">
        <div>
          <span className="eyebrow">CHỦ ĐỘNG LỊCH CHƠI</span>
          <h1>Lịch đặt của tôi</h1>
          <p className="muted">Mỗi lịch hẹn là một trận cầu đáng mong đợi.</p>
        </div>
        <Button onClick={() => navigate("/venues")}>
          Đặt lịch mới
          <ArrowRight size={15} />
        </Button>
      </div>
      <div className="toolbar">
        <div className="tabs">
          {[
            ["ALL", "Tất cả"],
            ["PENDING_PAYMENT", "Chờ thanh toán"],
            ["CONFIRMED", "Đã xác nhận"],
            ["CANCELLED", "Đã hủy"],
          ].map(([value, label]) => (
            <button
              key={value}
              className={filter === value ? "active" : ""}
              onClick={() => setFilter(value)}
            >
              {label}
            </button>
          ))}
        </div>
      </div>
      {filter !== "ALL" && (
        <p className="fineprint">
          Bộ lọc áp dụng trên trang hiện tại. Dùng phân trang để xem các lịch
          khác.
        </p>
      )}
      <DataState data={data}>
        {list.length ? (
          <div className="stack">
            {list.map((b) => (
              <BookingRow
                key={b.id}
                booking={b}
                onClick={() => navigate(`/bookings/${b.id}`)}
              />
            ))}
          </div>
        ) : (
          <Empty
            title="Chưa có lịch đặt trong mục này"
            text="Chọn một sân yêu thích và lên lịch cho trận cầu tiếp theo."
            action={
              <Button onClick={() => navigate("/venues")}>Khám phá sân</Button>
            }
          />
        )}
      </DataState>
      <Pagination data={data.value} page={page} setPage={setPage} />
    </>
  );
}
function BookingRow({ booking: b, onClick }) {
  const court = useData(`/courts/${b.courtId}`);
  const venue = useData(court.value ? `/venues/${court.value.venueId}` : null);
  const zone = venue.value?.timeZone || "Asia/Ho_Chi_Minh";
  return (
    <article className="booking-row">
      <div className="calendar-tile">
        <span>
          {new Date(b.startsAt).toLocaleDateString("vi-VN", {
            month: "short",
            timeZone: zone,
          })}
        </span>
        <strong>
          {new Date(b.startsAt).toLocaleDateString("vi-VN", {
            day: "2-digit",
            timeZone: zone,
          })}
        </strong>
      </div>
      <div className="booking-row-copy">
        <h3>{court.value?.name || "Lịch đặt sân"}</h3>
        <p>
          {dateText(b.startsAt, zone)} · {timeText(b.startsAt, zone)} –{" "}
          {timeText(b.endsAt, zone)}
        </p>
        <span className="fineprint">
          Mã đặt: {b.id.slice(0, 8).toUpperCase()}
        </span>
      </div>
      <Badge status={b.status} />
      <strong>{money(b.totalAmountVnd)}</strong>
      <Button className="secondary compact" onClick={onClick}>
        Chi tiết
        <ChevronRight size={14} />
      </Button>
    </article>
  );
}

export function BookingDetail({ id }) {
  const { refresh, notify, navigate, user } = useApp();
  const data = useData(`/bookings/${id}`);
  const payments = useData(`/bookings/${id}/payments?size=100`);
  const court = useData(data.value ? `/courts/${data.value.courtId}` : null);
  const venue = useData(court.value ? `/venues/${court.value.venueId}` : null);
  const [confirm, setConfirm] = useState(false);
  const [pay, setPay] = useState(false);
  const [refund, setRefund] = useState(null);
  const [now, setNow] = useState(Date.now());
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);
  const b = data.value;
  const zone = venue.value?.timeZone || "Asia/Ho_Chi_Minh";
  const expired = b?.holdExpiresAt && Date.parse(b.holdExpiresAt) <= now;
  const canPay =
    b?.customerId === user.id && b?.status === "PENDING_PAYMENT" && !expired;
  const canManagePayment =
    user.role === "ADMIN" ||
    (user.role === "OWNER" && venue.value?.ownerId === user.id);
  return (
    <>
      <Back to={user.role === "OWNER" ? "/owner" : "/bookings"}>
        {user.role === "OWNER" ? "Quản lý sân" : "Lịch đặt của tôi"}
      </Back>
      <DataState data={data}>
        {b && (
          <>
            <div className="page-heading">
              <div>
                <span className="eyebrow">
                  MÃ ĐẶT · {id.slice(0, 8).toUpperCase()}
                </span>
                <h1>Chi tiết lịch hẹn</h1>
                <p className="muted">
                  {venue.value?.name || "Thông tin cụm sân"}
                </p>
              </div>
              <Badge
                status={
                  expired && b.status === "PENDING_PAYMENT"
                    ? "EXPIRED"
                    : b.status
                }
              />
            </div>
            <div className="detail-grid">
              <section className="panel">
                <h2>{court.value?.name || "Sân cầu lông"}</h2>
                <p className="muted">{venue.value?.address}</p>
                <dl className="summary">
                  <div>
                    <dt>Ngày chơi</dt>
                    <dd>{dateText(b.startsAt, zone)}</dd>
                  </div>
                  <div>
                    <dt>Giờ chơi</dt>
                    <dd>
                      {timeText(b.startsAt, zone)} – {timeText(b.endsAt, zone)}
                    </dd>
                  </div>
                  <div>
                    <dt>Múi giờ</dt>
                    <dd>{zone}</dd>
                  </div>
                  <div>
                    <dt>Tổng tiền xác nhận</dt>
                    <dd className="price">{money(b.totalAmountVnd)}</dd>
                  </div>
                  <div>
                    <dt>Mã đặt đầy đủ</dt>
                    <dd className="mono small">{b.id}</dd>
                  </div>
                </dl>
                <div className="inline wrap-actions">
                  {canPay && (
                    <Button onClick={() => setPay(true)}>
                      Tạo yêu cầu thanh toán
                    </Button>
                  )}
                  {["PENDING_PAYMENT", "CONFIRMED"].includes(b.status) && (
                    <Button
                      className="secondary danger-text"
                      onClick={() => setConfirm(true)}
                    >
                      Hủy lịch đặt
                    </Button>
                  )}
                  {user.role !== "OWNER" && (
                    <Button
                      className="secondary"
                      onClick={() => navigate("/venues")}
                    >
                      Khám phá sân khác
                    </Button>
                  )}
                </div>
              </section>
              <aside className="stack">
                {b.status === "PENDING_PAYMENT" && (
                  <div className="green-note">
                    <span className="lime-label">THỜI GIAN GIỮ CHỖ</span>
                    <div className="countdown">
                      {remaining(b.holdExpiresAt, now)}
                    </div>
                    <p>
                      {expired
                        ? "Thời gian giữ chỗ đã hết. Bạn có thể tạo một lịch đặt mới."
                        : "Hoàn tất các bước theo hướng dẫn của chủ sân trước khi hết thời gian giữ chỗ."}
                    </p>
                  </div>
                )}
                <div className="info-card">
                  <h3>Yêu cầu thanh toán</h3>
                  <DataState data={payments}>
                    {payments.value?.content?.length ? (
                      payments.value.content.map((p) => (
                        <div className="payment-item" key={p.id}>
                          <div className="inline between">
                            <strong>{p.provider}</strong>
                            <Badge status={p.status} />
                          </div>
                          <p>{money(p.amountVnd)}</p>
                          <small className="mono muted">
                            {p.id.slice(0, 8).toUpperCase()}
                          </small>
                          {canManagePayment && p.status === "SUCCEEDED" && (
                            <Button
                              className="secondary compact"
                              onClick={() => setRefund(p)}
                            >
                              Ghi nhận hoàn tiền
                            </Button>
                          )}
                        </div>
                      ))
                    ) : (
                      <p className="muted small">Chưa có yêu cầu thanh toán.</p>
                    )}
                  </DataState>
                  <p className="fineprint">
                    Hệ thống hiện ghi nhận yêu cầu và trạng thái nội bộ; chưa
                    chuyển tiền trực tuyến.
                  </p>
                </div>
              </aside>
            </div>
          </>
        )}
      </DataState>
      {confirm && (
        <Confirm
          title="Hủy lịch hẹn này?"
          text="Lịch đặt sẽ được chuyển sang trạng thái đã hủy nếu đáp ứng chính sách của hệ thống. Giao dịch đã thanh toán cần được chủ sân đối soát riêng."
          danger
          close={() => setConfirm(false)}
          accept={async () => {
            await api.post(`/bookings/${id}/cancel`);
            refresh();
            notify("Đã hủy lịch đặt.");
          }}
        />
      )}
      {pay && <PaymentForm booking={b} close={() => setPay(false)} />}
      {refund && (
        <Confirm
          title="Ghi nhận hoàn tiền?"
          text="Xác nhận sau khi đã hoàn tiền và đối soát với nhà cung cấp. Hệ thống sẽ cập nhật giao dịch thành đã hoàn tiền và hủy lịch đặt."
          close={() => setRefund(null)}
          accept={async () => {
            await api.post(`/owner/payments/${refund.id}/refund`);
            refresh();
            notify("Đã ghi nhận hoàn tiền và hủy lịch đặt.");
          }}
        />
      )}
    </>
  );
}
function PaymentForm({ booking, close }) {
  const { refresh, notify } = useApp();
  const [provider, setProvider] = useState("VNPAY");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  return (
    <Modal title="Tạo yêu cầu thanh toán" close={close}>
      <form
        className="stack"
        onSubmit={async (e) => {
          e.preventDefault();
          setBusy(true);
          setError(null);
          try {
            await api.post("/payments", { bookingId: booking.id, provider });
            refresh();
            notify(
              "Đã ghi nhận yêu cầu thanh toán. Vui lòng chờ chủ sân đối soát.",
            );
            close();
          } catch (error) {
            setError(error);
            setBusy(false);
          }
        }}
      >
        <p className="muted">
          Tổng tiền do hệ thống xác nhận:{" "}
          <strong>{money(booking.totalAmountVnd)}</strong>.
        </p>
        <Field label="Nhà cung cấp dự kiến">
          <select
            value={provider}
            onChange={(e) => setProvider(e.target.value)}
          >
            <option value="VNPAY">VNPAY</option>
            <option value="MOMO">MoMo</option>
            <option value="BANK_TRANSFER">Chuyển khoản ngân hàng</option>
          </select>
        </Field>
        <div className="login-note">
          <strong>Lưu ý về thanh toán</strong>
          <span>
            Thao tác này chỉ tạo bản ghi yêu cầu, không mở cổng thanh toán và
            không trừ tiền. Liên hệ chủ sân để được hướng dẫn đối soát.
          </span>
        </div>
        <ErrorBox error={error} />
        <Button type="submit" loading={busy}>
          Ghi nhận yêu cầu
        </Button>
      </form>
    </Modal>
  );
}

export function Profile() {
  const { user, logout, refresh, notify } = useApp();
  const [name, setName] = useState(user.fullName);
  const [phone, setPhone] = useState(user.phone || "");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  return (
    <>
      <div className="page-heading">
        <div>
          <span className="eyebrow">THÔNG TIN CỦA BẠN</span>
          <h1>Hồ sơ cá nhân</h1>
          <p className="muted">
            Giữ thông tin liên hệ luôn sẵn sàng cho lịch hẹn tiếp theo.
          </p>
        </div>
      </div>
      <div className="detail-grid">
        <form
          className="panel stack"
          onSubmit={async (e) => {
            e.preventDefault();
            setBusy(true);
            setError(null);
            try {
              const value = await api.put("/users/me", {
                fullName: name.trim(),
                phone: phone.trim() || null,
              });
              auth.updateUser(value);
              refresh();
              notify("Đã lưu thông tin cá nhân.");
            } catch (error) {
              setError(error);
            } finally {
              setBusy(false);
            }
          }}
        >
          <Field label="Họ và tên">
            <input
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              maxLength={120}
            />
          </Field>
          <Field label="Email">
            <input value={user.email} disabled />
          </Field>
          <Field label="Số điện thoại">
            <input
              type="tel"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              maxLength={30}
              placeholder="Chưa cung cấp"
            />
          </Field>
          <ErrorBox error={error} />
          <Button type="submit" loading={busy}>
            Lưu thay đổi
          </Button>
        </form>
        <aside className="stack">
          <div className="green-note">
            <span className="lime-label">HẸN NHAU TRÊN SÂN</span>
            <h2>{roles[user.role]}</h2>
            <p>
              Tài khoản của bạn được sử dụng để quản lý hồ sơ và các lịch đặt
              trên BadmintonHub.
            </p>
          </div>
          <Button className="secondary danger-text" onClick={logout}>
            Đăng xuất
          </Button>
        </aside>
      </div>
    </>
  );
}
