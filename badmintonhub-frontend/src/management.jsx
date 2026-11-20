import React, { useEffect, useState } from "react";
import OwnerOverview from "./OwnerOverview.jsx";
import { ArrowRight, Pencil, Plus, Search, Trash2 } from "lucide-react";
import { api, auth } from "./api.js";
import {
  dateText,
  localDate,
  money,
  roles,
  timeText,
  zonedInstant,
} from "./domain.js";
import {
  Badge,
  Button,
  Confirm,
  DataState,
  Empty,
  ErrorBox,
  Field,
  Modal,
  Pagination,
  useApp,
  useCollection,
  useData,
} from "./ui.jsx";

const days = [
  "Thứ Hai",
  "Thứ Ba",
  "Thứ Tư",
  "Thứ Năm",
  "Thứ Sáu",
  "Thứ Bảy",
  "Chủ nhật",
];
const titles = {
  venue: "cụm sân",
  court: "sân",
  rule: "quy tắc giá",
  block: "lịch khóa sân",
};

function ResourceForm({
  kind,
  initial,
  defaults = {},
  venues = [],
  courts = [],
  zone = "Asia/Ho_Chi_Minh",
  save,
  close,
}) {
  const [values, setValues] = useState(() => ({
    name: "",
    address: "",
    timeZone: "Asia/Ho_Chi_Minh",
    venueId: venues[0]?.id || "",
    courtId: courts[0]?.id || "",
    baseHourlyRateVnd: 80000,
    active: true,
    dayOfWeek: 1,
    startLocalTime: "18:00",
    endLocalTime: "22:00",
    hourlyRateVnd: 100000,
    validFrom: localDate(),
    validUntil: "",
    startsAt: `${localDate()}T12:00`,
    endsAt: `${localDate()}T14:00`,
    reason: "",
    ...defaults,
    ...initial,
  }));
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const update = (key, value) =>
    setValues((previous) => ({ ...previous, [key]: value }));
  const field = (key, label, type = "text", options = {}) => (
    <Field label={label} key={key}>
      <input
        type={type}
        value={values[key] ?? ""}
        onChange={(e) => update(key, e.target.value)}
        required
        {...options}
      />
    </Field>
  );
  const integer = (key) => {
    const result = Number(values[key]);
    if (!Number.isSafeInteger(result) || result < 0)
      throw new Error(
        "Giá phải là số nguyên không âm và nằm trong phạm vi hợp lệ.",
      );
    return result;
  };
  async function submit(event) {
    event.preventDefault();
    setError(null);
    setBusy(true);
    try {
      let body;
      if (kind === "venue")
        body = {
          name: values.name.trim(),
          address: values.address.trim(),
          timeZone: values.timeZone.trim(),
        };
      if (kind === "court")
        body = initial
          ? {
              name: values.name.trim(),
              baseHourlyRateVnd: integer("baseHourlyRateVnd"),
              active: values.active === true || values.active === "true",
            }
          : {
              venueId: values.venueId,
              name: values.name.trim(),
              baseHourlyRateVnd: integer("baseHourlyRateVnd"),
            };
      if (kind === "rule")
        body = {
          courtId: values.courtId,
          dayOfWeek: Number(values.dayOfWeek),
          startLocalTime: values.startLocalTime,
          endLocalTime: values.endLocalTime,
          hourlyRateVnd: integer("hourlyRateVnd"),
          validFrom: values.validFrom,
          validUntil: values.validUntil || null,
        };
      if (kind === "block")
        body = {
          courtId: values.courtId,
          startsAt: zonedInstant(
            values.startsAt.slice(0, 10),
            values.startsAt.slice(11, 16),
            zone,
          ),
          endsAt: zonedInstant(
            values.endsAt.slice(0, 10),
            values.endsAt.slice(11, 16),
            zone,
          ),
          reason: values.reason.trim(),
        };
      await save(body);
      close();
    } catch (error) {
      setError(error);
      setBusy(false);
    }
  }
  return (
    <Modal
      title={`${initial ? "Cập nhật" : "Thêm"} ${titles[kind]}`}
      close={() => {
        if (!busy) close();
      }}
    >
      <form onSubmit={submit} className="stack">
        {kind === "venue" && (
          <>
            {field("name", "Tên cụm sân", "text", { maxLength: 150 })}
            {field("address", "Địa chỉ", "text", { maxLength: 255 })}
            {field("timeZone", "Múi giờ", "text", { maxLength: 64 })}
          </>
        )}
        {kind === "court" && (
          <>
            {!initial && (
              <Field label="Cụm sân">
                <select
                  required
                  value={values.venueId}
                  onChange={(e) => update("venueId", e.target.value)}
                >
                  {venues.map((v) => (
                    <option key={v.id} value={v.id}>
                      {v.name}
                    </option>
                  ))}
                </select>
              </Field>
            )}
            {field("name", "Tên sân", "text", { maxLength: 100 })}
            {field("baseHourlyRateVnd", "Giá cơ bản / giờ (VND)", "number", {
              min: 0,
              step: 1,
            })}
            {initial && (
              <Field label="Trạng thái">
                <select
                  value={String(values.active)}
                  onChange={(e) => update("active", e.target.value)}
                >
                  <option value="true">Đang hoạt động</option>
                  <option value="false">Tạm ngưng</option>
                </select>
              </Field>
            )}
          </>
        )}
        {["rule", "block"].includes(kind) && (
          <Field label="Sân">
            <select
              required
              value={values.courtId}
              onChange={(e) => update("courtId", e.target.value)}
            >
              {courts.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </Field>
        )}
        {kind === "rule" && (
          <>
            <Field label="Ngày trong tuần">
              <select
                value={values.dayOfWeek}
                onChange={(e) => update("dayOfWeek", e.target.value)}
              >
                {days.map((day, index) => (
                  <option value={index + 1} key={day}>
                    {day}
                  </option>
                ))}
              </select>
            </Field>
            <div className="form-grid">
              {field("startLocalTime", "Từ giờ", "time")}
              {field("endLocalTime", "Đến giờ", "time")}
            </div>
            {field("hourlyRateVnd", "Giá giờ (VND)", "number", {
              min: 0,
              step: 1,
            })}
            <div className="form-grid">
              {field("validFrom", "Ngày có hiệu lực", "date")}
              {field("validUntil", "Hết hiệu lực (tùy chọn)", "date", {
                required: false,
              })}
            </div>
            <p className="fineprint">
              Ngày hết hiệu lực là mốc loại trừ; để trống nếu không giới hạn.
            </p>
          </>
        )}
        {kind === "block" && (
          <>
            {field("startsAt", "Bắt đầu khóa", "datetime-local")}
            {field("endsAt", "Kết thúc khóa", "datetime-local")}
            {field("reason", "Lý do", "text", { maxLength: 255 })}
          </>
        )}
        {["rule", "block"].includes(kind) && (
          <p className="fineprint">Giờ nhập theo múi giờ {zone}.</p>
        )}
        <ErrorBox error={error} />
        <div className="form-actions">
          <Button
            type="button"
            className="secondary"
            disabled={busy}
            onClick={close}
          >
            Quay lại
          </Button>
          <Button type="submit" loading={busy}>
            Lưu thông tin
          </Button>
        </div>
      </form>
    </Modal>
  );
}

export function OwnerPage() {
  const { navigate, refresh, notify } = useApp();
  const [tab, setTab] = useState("overview");
  const [venueId, setVenueId] = useState("");
  const [courtId, setCourtId] = useState("");
  const [page, setPage] = useState(0);
  const [form, setForm] = useState(null);
  const [confirm, setConfirm] = useState(null);
  const venues = useCollection("/owner/venues");
  const courts = useCollection(
    venueId ? `/venues/${venueId}/courts?activeOnly=false` : null,
  );
  useEffect(() => {
    if (venues.value.length && !venues.value.some((v) => v.id === venueId))
      setVenueId(venues.value[0].id);
    if (!venues.value.length && !venues.loading) setVenueId("");
  }, [venues.value, venues.loading, venueId]);
  useEffect(() => {
    if (courts.value.length && !courts.value.some((c) => c.id === courtId))
      setCourtId(courts.value[0].id);
    if (!courts.value.length && !courts.loading) setCourtId("");
    setPage(0);
  }, [courts.value, courts.loading, courtId]);
  const selectedVenue = venues.value.find((v) => v.id === venueId);
  const zone = selectedVenue?.timeZone || "Asia/Ho_Chi_Minh";
  const rules = useData(
    tab === "pricing" && courtId ? `/courts/${courtId}/pricing-rules` : null,
    [],
  );
  const blocks = useCollection(
    tab === "blocks" && courtId ? `/courts/${courtId}/blocks` : null,
  );
  const bookings = useData(
    tab === "bookings" && courtId
      ? `/owner/courts/${courtId}/bookings?page=${page}&size=10`
      : null,
  );
  const base = {
    venue: "/owner/venues",
    court: "/owner/courts",
    rule: "/owner/pricing-rules",
    block: "/owner/court-blocks",
  };
  const remove = (kind, item) =>
    setConfirm({
      title: `Xóa ${titles[kind]}?`,
      text: "Thao tác chỉ hoàn tất nếu dữ liệu không còn bị tham chiếu bởi nghiệp vụ khác.",
      danger: true,
      accept: async () => {
        await api.delete(`${base[kind]}/${item.id}`);
        refresh();
        notify("Đã xóa thông tin.");
      },
    });
  const create = () =>
    setForm({
      kind:
        tab === "venues"
          ? "venue"
          : tab === "courts"
            ? "court"
            : tab === "pricing"
              ? "rule"
              : "block",
    });
  const tabs = [
    ["overview", "Tổng quan & lịch trống"],
    ["venues", "Cụm sân"],
    ["courts", "Sân cầu lông"],
    ["bookings", "Lịch đặt sân"],
    ["pricing", "Quy tắc giá"],
    ["blocks", "Khóa lịch sân"],
  ];
  return (
    <>
      <div className="page-heading">
        <div>
          <span className="eyebrow">KHÔNG GIAN DÀNH CHO CHỦ SÂN</span>
          <h1>Quản lý sân của bạn</h1>
          <p className="muted">Thông tin rõ ràng, lịch chơi chủ động.</p>
        </div>
        {["venues", "courts", "pricing", "blocks"].includes(tab) && (
          <Button
            onClick={create}
            disabled={
              (tab !== "venues" && !venueId) ||
              (["pricing", "blocks"].includes(tab) && !courtId)
            }
          >
            <Plus size={16} />
            Thêm{" "}
            {
              titles[
                tab === "venues"
                  ? "venue"
                  : tab === "courts"
                    ? "court"
                    : tab === "pricing"
                      ? "rule"
                      : "block"
              ]
            }
          </Button>
        )}
      </div>
      <div className="tabs section-tabs">
        {tabs.map(([value, label]) => (
          <button
            key={value}
            className={tab === value ? "active" : ""}
            onClick={() => setTab(value)}
          >
            {label}
          </button>
        ))}
      </div>
      {!["venues", "overview"].includes(tab) && (
        <div className="toolbar form-grid">
          <Field label="Cụm sân">
            <select
              value={venueId}
              onChange={(e) => {
                setVenueId(e.target.value);
                setCourtId("");
              }}
            >
              {venues.value.length ? (
                venues.value.map((v) => (
                  <option key={v.id} value={v.id}>
                    {v.name}
                  </option>
                ))
              ) : (
                <option value="">Chưa có cụm sân</option>
              )}
            </select>
          </Field>
          {["bookings", "pricing", "blocks"].includes(tab) && (
            <Field label="Sân">
              <select
                value={courtId}
                onChange={(e) => {
                  setCourtId(e.target.value);
                  setPage(0);
                }}
              >
                {courts.value.length ? (
                  courts.value.map((c) => (
                    <option value={c.id} key={c.id}>
                      {c.name}
                    </option>
                  ))
                ) : (
                  <option value="">Chưa có sân</option>
                )}
              </select>
            </Field>
          )}
        </div>
      )}
      {tab === "overview" && <OwnerOverview venues={venues} />}
      {tab === "venues" && (
        <DataState data={venues}>
          {venues.value.length ? (
            <div className="stack">
              {venues.value.map((v) => (
                <article className="management-row" key={v.id}>
                  <div className="grow">
                    <h3>{v.name}</h3>
                    <p className="muted">{v.address}</p>
                    <small>{v.timeZone}</small>
                  </div>
                  <Button
                    className="secondary compact"
                    onClick={() => {
                      setVenueId(v.id);
                      setCourtId("");
                      setTab("courts");
                    }}
                  >
                    Quản lý sân
                    <ArrowRight size={14} />
                  </Button>
                  <button
                    className="icon-button"
                    aria-label={`Sửa ${v.name}`}
                    onClick={() => setForm({ kind: "venue", initial: v })}
                  >
                    <Pencil size={16} />
                  </button>
                  <button
                    className="icon-button danger-text"
                    aria-label={`Xóa ${v.name}`}
                    onClick={() => remove("venue", v)}
                  >
                    <Trash2 size={16} />
                  </button>
                </article>
              ))}
            </div>
          ) : (
            <Empty
              title="Bắt đầu với một cụm sân"
              text="Thêm tên, địa chỉ và múi giờ để tạo địa điểm của bạn."
              action={<Button onClick={create}>Thêm cụm sân</Button>}
            />
          )}
        </DataState>
      )}
      {tab === "courts" && (
        <DataState data={courts}>
          {courts.value.length ? (
            <div className="stack">
              {courts.value.map((c) => (
                <article className="management-row" key={c.id}>
                  <div className="grow">
                    <div className="inline">
                      <h3>{c.name}</h3>
                      <Badge tone={c.active ? "green" : "amber"}>
                        {c.active ? "Hoạt động" : "Tạm ngưng"}
                      </Badge>
                    </div>
                    <p className="muted">{money(c.baseHourlyRateVnd)} / giờ</p>
                  </div>
                  <button
                    className="icon-button"
                    aria-label={`Sửa ${c.name}`}
                    onClick={() => setForm({ kind: "court", initial: c })}
                  >
                    <Pencil size={16} />
                  </button>
                  <button
                    className="icon-button danger-text"
                    aria-label={`Xóa ${c.name}`}
                    onClick={() => remove("court", c)}
                  >
                    <Trash2 size={16} />
                  </button>
                </article>
              ))}
            </div>
          ) : (
            <Empty
              title="Chưa có sân trong cụm này"
              text="Tạo cụm sân trước, sau đó thêm các sân bên trong."
            />
          )}
        </DataState>
      )}
      {tab === "pricing" && (
        <DataState data={rules}>
          {rules.value.length ? (
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Ngày</th>
                    <th>Khung giờ</th>
                    <th>Giá giờ</th>
                    <th>Hiệu lực</th>
                    <th>Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {rules.value.map((r) => (
                    <tr key={r.id}>
                      <td>{days[r.dayOfWeek - 1]}</td>
                      <td>
                        {r.startLocalTime.slice(0, 5)} –{" "}
                        {r.endLocalTime.slice(0, 5)}
                      </td>
                      <td>{money(r.hourlyRateVnd)}</td>
                      <td>
                        {r.validFrom} → {r.validUntil || "Không giới hạn"}
                      </td>
                      <td>
                        <button
                          className="text-button danger-text"
                          onClick={() => remove("rule", r)}
                        >
                          Xóa
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <Empty
              title="Chưa có quy tắc giá"
              text="Khi không có quy tắc phù hợp, hệ thống dùng giá cơ bản của sân."
            />
          )}
        </DataState>
      )}
      {tab === "blocks" && (
        <DataState data={blocks}>
          {blocks.value.length ? (
            <div className="stack">
              {blocks.value.map((b) => (
                <article className="management-row" key={b.id}>
                  <div className="grow">
                    <h3>{b.reason}</h3>
                    <p className="muted">
                      {dateText(b.startsAt, zone)} ·{" "}
                      {timeText(b.startsAt, zone)} → {dateText(b.endsAt, zone)}{" "}
                      · {timeText(b.endsAt, zone)}
                    </p>
                  </div>
                  <Button
                    className="secondary compact"
                    onClick={() =>
                      setConfirm({
                        title: "Hủy lịch khóa sân?",
                        text: "Khoảng thời gian này sẽ không còn bị khóa bởi bản ghi này.",
                        accept: async () => {
                          await api.post(`/owner/court-blocks/${b.id}/cancel`);
                          refresh();
                          notify("Đã hủy lịch khóa sân.");
                        },
                      })
                    }
                  >
                    Hủy lịch khóa
                  </Button>
                </article>
              ))}
            </div>
          ) : (
            <Empty
              title="Chưa có lịch khóa sân"
              text="Tạo lịch khóa khi bảo trì hoặc tạm dành sân cho hoạt động riêng."
            />
          )}
        </DataState>
      )}
      {tab === "bookings" && (
        <DataState data={bookings}>
          {bookings.value?.content?.length ? (
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Mã đặt</th>
                    <th>Ngày chơi</th>
                    <th>Khung giờ</th>
                    <th>Tổng tiền</th>
                    <th>Trạng thái</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {bookings.value.content.map((b) => (
                    <tr key={b.id}>
                      <td className="mono">{b.id.slice(0, 8).toUpperCase()}</td>
                      <td>{dateText(b.startsAt, zone)}</td>
                      <td>
                        {timeText(b.startsAt, zone)} –{" "}
                        {timeText(b.endsAt, zone)}
                      </td>
                      <td>{money(b.totalAmountVnd)}</td>
                      <td>
                        <Badge status={b.status} />
                      </td>
                      <td>
                        <button
                          className="text-button"
                          onClick={() => navigate(`/bookings/${b.id}`)}
                        >
                          Xem đơn
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <Empty
              title="Chưa có lịch đặt cho sân này"
              text="Các đơn đặt của người chơi sẽ hiển thị tại đây."
            />
          )}
          <Pagination data={bookings.value} page={page} setPage={setPage} />
        </DataState>
      )}
      {form && (
        <ResourceForm
          {...form}
          defaults={{ venueId, courtId }}
          venues={venues.value}
          courts={courts.value}
          zone={zone}
          close={() => setForm(null)}
          save={async (body) => {
            if (form.initial)
              await api.put(`${base[form.kind]}/${form.initial.id}`, body);
            else await api.post(base[form.kind], body);
            refresh();
            notify("Đã lưu thông tin.");
          }}
        />
      )}
      {confirm && <Confirm {...confirm} close={() => setConfirm(null)} />}
    </>
  );
}

export function AdminPage() {
  const { user, refresh, notify, navigate } = useApp();
  const [tab, setTab] = useState("users");
  const [page, setPage] = useState(0);
  const [confirm, setConfirm] = useState(null);
  const [query, setQuery] = useState("");
  const [lookup, setLookup] = useState("");
  const [lookupId, setLookupId] = useState("");
  const [error, setError] = useState(null);
  const [complete, setComplete] = useState(null);
  const users = useData(
    tab === "users" ? `/admin/users?page=${page}&size=10` : null,
  );
  const payments = useCollection(
    tab === "payments" && lookupId ? `/bookings/${lookupId}/payments` : null,
  );
  const list = (users.value?.content || []).filter((value) =>
    `${value.fullName} ${value.email}`
      .toLocaleLowerCase("vi")
      .includes(query.toLocaleLowerCase("vi")),
  );
  const update = async (id, kind, value) => {
    const result = await api.patch(`/admin/users/${id}/${kind}`, {
      [kind]: value,
    });
    if (id === user.id) auth.updateUser(result);
    refresh();
    notify("Đã cập nhật tài khoản.");
  };
  return (
    <>
      <div className="page-heading">
        <div>
          <span className="eyebrow">KHÔNG GIAN QUẢN TRỊ</span>
          <h1>Quản trị hệ thống</h1>
          <p className="muted">
            Quản lý tài khoản và đối soát thông tin một cách rõ ràng.
          </p>
        </div>
      </div>
      <div className="tabs section-tabs">
        {[
          ["users", "Người dùng"],
          ["payments", "Đối soát thanh toán"],
          ["operations", "Vận hành"],
        ].map(([value, label]) => (
          <button
            key={value}
            className={tab === value ? "active" : ""}
            onClick={() => setTab(value)}
          >
            {label}
          </button>
        ))}
      </div>
      {tab === "users" && (
        <>
          <div className="search-bar">
            <Search size={18} />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Lọc tên hoặc email trong trang hiện tại…"
              aria-label="Lọc người dùng"
            />
          </div>
          <DataState data={users}>
            {list.length ? (
              <div className="table-scroll">
                <table>
                  <thead>
                    <tr>
                      <th>Người dùng</th>
                      <th>Vai trò</th>
                      <th>Lịch đặt</th>
                      <th>Truy cập tài khoản</th>
                    </tr>
                  </thead>
                  <tbody>
                    {list.map((value) => (
                      <tr key={value.id}>
                        <td>
                          <strong>
                            {value.fullName}
                            {value.id === user.id ? " (bạn)" : ""}
                          </strong>
                          <span className="table-subtitle">{value.email}</span>
                        </td>
                        <td>
                          <select
                            aria-label={`Vai trò của ${value.fullName}`}
                            value={value.role}
                            onChange={(e) => {
                              const role = e.target.value;
                              setConfirm({
                                title: "Thay đổi vai trò?",
                                text: `Cấp quyền ${roles[role]} cho ${value.fullName}. Quyền mới sẽ áp dụng cho các yêu cầu tiếp theo.`,
                                accept: () => update(value.id, "role", role),
                              });
                            }}
                          >
                            {Object.entries(roles).map(([key, label]) => (
                              <option key={key} value={key}>
                                {label}
                              </option>
                            ))}
                          </select>
                        </td>
                        <td>
                          <button
                            className="text-button"
                            onClick={() =>
                              setConfirm({
                                title: `Lịch đặt của ${value.fullName}`,
                                text: "Mở danh sách lịch đặt của tài khoản này trong cửa sổ tra cứu.",
                                accept: async () => {
                                  const data = await api.get(
                                    `/admin/users/${value.id}/bookings?size=100`,
                                  );
                                  setComplete({
                                    type: "bookings",
                                    data: data.content || [],
                                    name: value.fullName,
                                  });
                                },
                              })
                            }
                          >
                            Tra cứu
                          </button>
                        </td>
                        <td>
                          <div className="inline">
                            <button
                              className="text-button danger-text"
                              onClick={() =>
                                setConfirm({
                                  title: "Khóa tài khoản?",
                                  text: `Tài khoản ${value.fullName} sẽ không tiếp tục truy cập bằng các token đã cấp.`,
                                  danger: true,
                                  accept: () =>
                                    update(value.id, "active", false),
                                })
                              }
                            >
                              Khóa
                            </button>
                            <button
                              className="text-button"
                              onClick={() =>
                                setConfirm({
                                  title: "Mở tài khoản?",
                                  text: `Cho phép tài khoản ${value.fullName} đăng nhập trở lại.`,
                                  accept: () =>
                                    update(value.id, "active", true),
                                })
                              }
                            >
                              Mở
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <Empty
                title="Không có người dùng phù hợp"
                text="Thử từ khóa khác hoặc chuyển trang."
              />
            )}
            <Pagination data={users.value} page={page} setPage={setPage} />
          </DataState>
          <p className="fineprint section-note">
            Các thao tác Khóa/Mở là yêu cầu thay đổi quyền truy cập; danh sách
            hiện không cung cấp trạng thái hoạt động của tài khoản.
          </p>
        </>
      )}
      {tab === "payments" && (
        <>
          <form
            className="search-bar"
            onSubmit={(e) => {
              e.preventDefault();
              setError(null);
              if (
                !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(
                  lookup.trim(),
                )
              ) {
                setError(new Error("Nhập mã đặt sân đầy đủ ở định dạng UUID."));
                return;
              }
              setLookupId(lookup.trim());
            }}
          >
            <Search size={18} />
            <input
              required
              value={lookup}
              onChange={(e) => setLookup(e.target.value)}
              aria-label="Mã đặt sân để đối soát"
              placeholder="Nhập mã đặt sân đầy đủ để tra cứu thanh toán…"
            />
            <Button type="submit">Tra cứu</Button>
          </form>
          <ErrorBox error={error} />
          <div className="login-note section-note">
            <strong>Đối soát nội bộ</strong>
            <span>
              Các thao tác cập nhật trạng thái trong hệ thống, không thực hiện
              chuyển hoặc hoàn tiền qua ngân hàng.
            </span>
          </div>
          {lookupId ? (
            <DataState data={payments}>
              {payments.value.length ? (
                <div className="stack">
                  {payments.value.map((p) => (
                    <article className="management-row" key={p.id}>
                      <div className="grow">
                        <div className="inline">
                          <h3>{p.provider}</h3>
                          <Badge status={p.status} />
                        </div>
                        <p className="muted">
                          {money(p.amountVnd)} ·{" "}
                          <span className="mono">
                            {p.id.slice(0, 8).toUpperCase()}
                          </span>
                        </p>
                        <small>
                          {p.providerPaymentId ||
                            "Chưa có mã giao dịch nhà cung cấp"}
                        </small>
                      </div>
                      {p.status === "PENDING" && (
                        <>
                          <Button
                            className="compact"
                            onClick={() =>
                              setComplete({ type: "payment", payment: p })
                            }
                          >
                            Xác nhận
                          </Button>
                          <Button
                            className="secondary compact danger-text"
                            onClick={() =>
                              setConfirm({
                                title: "Đánh dấu thanh toán thất bại?",
                                text: "Chỉ xác nhận sau khi đã kiểm tra kết quả giao dịch với nhà cung cấp.",
                                danger: true,
                                accept: async () => {
                                  await api.post(
                                    `/admin/payments/${p.id}/fail`,
                                  );
                                  refresh();
                                  notify("Đã cập nhật trạng thái thất bại.");
                                },
                              })
                            }
                          >
                            Thất bại
                          </Button>
                        </>
                      )}
                      {p.status === "SUCCEEDED" && (
                        <Button
                          className="secondary compact"
                          onClick={() =>
                            setConfirm({
                              title: "Ghi nhận trạng thái hoàn tiền?",
                              text: "Chỉ thực hiện khi tiền đã được đối soát và hoàn bằng quy trình bên ngoài hệ thống.",
                              accept: async () => {
                                await api.post(
                                  `/owner/payments/${p.id}/refund`,
                                );
                                refresh();
                                notify("Đã ghi nhận trạng thái hoàn tiền.");
                              },
                            })
                          }
                        >
                          Hoàn tiền
                        </Button>
                      )}
                    </article>
                  ))}
                </div>
              ) : (
                <Empty
                  title="Chưa có yêu cầu thanh toán"
                  text="Đơn đặt này chưa tạo yêu cầu thanh toán."
                />
              )}
            </DataState>
          ) : (
            <Empty
              title="Tra cứu theo mã đặt sân"
              text="Mở chi tiết lịch đặt để sao chép mã đầy đủ, sau đó dán vào ô phía trên."
            />
          )}
        </>
      )}
      {tab === "operations" && (
        <div className="panel operation-card">
          <span className="eyebrow">GIỮ LỊCH ĐẶT LUÔN CẬP NHẬT</span>
          <h2>Xử lý giữ chỗ hết hạn</h2>
          <p className="muted">
            Chuyển các lịch đặt đang chờ thanh toán và đã hết hạn giữ chỗ sang
            trạng thái hết hạn.
          </p>
          <Button
            onClick={() =>
              setConfirm({
                title: "Cập nhật các giữ chỗ hết hạn?",
                text: "Hệ thống sẽ kiểm tra các đơn chờ thanh toán và cập nhật những đơn đã quá thời hạn.",
                accept: async () => {
                  const result = await api.post(
                    "/admin/bookings/expire-pending",
                  );
                  refresh();
                  notify(
                    `Đã cập nhật ${result.expiredCount} lịch đặt hết hạn.`,
                  );
                },
              })
            }
          >
            Kiểm tra và cập nhật
          </Button>
        </div>
      )}
      {confirm && <Confirm {...confirm} close={() => setConfirm(null)} />}
      {complete?.type === "payment" && (
        <CompletePayment
          payment={complete.payment}
          close={() => setComplete(null)}
        />
      )}
      {complete?.type === "bookings" && (
        <Modal
          wide
          title={`Lịch đặt · ${complete.name}`}
          close={() => setComplete(null)}
        >
          {complete.data.length ? (
            <div className="stack">
              {complete.data.map((b) => (
                <article className="management-row" key={b.id}>
                  <div className="grow">
                    <strong>
                      {dateText(b.startsAt)} · {timeText(b.startsAt)} –{" "}
                      {timeText(b.endsAt)}
                    </strong>
                    <p className="mono small muted">{b.id}</p>
                  </div>
                  <Badge status={b.status} />
                  <Button
                    className="secondary compact"
                    onClick={() => {
                      setComplete(null);
                      navigate(`/bookings/${b.id}`);
                    }}
                  >
                    Chi tiết
                  </Button>
                </article>
              ))}
            </div>
          ) : (
            <Empty
              title="Tài khoản chưa có lịch đặt"
              text="Các lịch đặt sẽ hiển thị tại đây."
            />
          )}
        </Modal>
      )}
    </>
  );
}

function CompletePayment({ payment, close }) {
  const { notify, refresh } = useApp();
  const [reference, setReference] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  return (
    <Modal title="Xác nhận kết quả đối soát" close={close}>
      <form
        className="stack"
        onSubmit={async (e) => {
          e.preventDefault();
          if (!reference.trim()) {
            setError(new Error("Mã giao dịch không được để trống."));
            return;
          }
          setBusy(true);
          setError(null);
          try {
            await api.post(`/admin/payments/${payment.id}/complete`, {
              providerPaymentId: reference.trim(),
            });
            refresh();
            notify("Đã xác nhận trạng thái thanh toán.");
            close();
          } catch (error) {
            setError(error);
            setBusy(false);
          }
        }}
      >
        <p className="muted">
          Xác nhận giao dịch {money(payment.amountVnd)} sau khi đã kiểm tra với
          nhà cung cấp.
        </p>
        <Field label="Mã giao dịch nhà cung cấp">
          <input
            value={reference}
            onChange={(e) => setReference(e.target.value)}
            required
            maxLength={128}
            placeholder="Nhập mã đối soát chính xác"
          />
        </Field>
        <ErrorBox error={error} />
        <Button type="submit" loading={busy}>
          Xác nhận thành công
        </Button>
      </form>
    </Modal>
  );
}
