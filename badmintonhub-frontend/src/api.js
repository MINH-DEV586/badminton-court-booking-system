const BASE = (import.meta.env?.VITE_API_BASE_URL || "/api").replace(/\/$/, "");
let session = null;
let refreshing = null;
const listeners = new Set();

export class ApiError extends Error {
  constructor(message, status = 0, code = "", fields = {}) {
    super(message);
    this.status = status;
    this.code = code;
    this.fields = fields;
  }
}

const messages = {
  INVALID_CREDENTIALS: "Email hoặc mật khẩu chưa đúng. Vui lòng thử lại.",
  EMAIL_ALREADY_EXISTS:
    "Email này đã được đăng ký. Bạn có thể đăng nhập để tiếp tục.",
  UNAUTHORIZED: "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.",
  FORBIDDEN: "Bạn không có quyền thực hiện thao tác này.",
  BOOKING_CONFLICT:
    "Khung giờ này vừa có người đặt. Bạn hãy chọn giờ khác nhé.",
  COURT_BLOCKED: "Sân đang bị khóa trong khoảng thời gian bạn chọn.",
  COURT_INACTIVE: "Sân đang tạm ngưng hoạt động.",
  BOOKING_HOLD_EXPIRED: "Thời gian giữ chỗ đã hết. Vui lòng tạo lịch đặt mới.",
  CANNOT_CANCEL: "Lịch đặt này hiện không thể hủy.",
  BLOCK_CONFLICT: "Khoảng thời gian này đã có lịch khóa sân.",
  CANNOT_REFUND:
    "Chỉ giao dịch thành công mới có thể chuyển trạng thái hoàn tiền.",
  CONFLICT:
    "Dữ liệu này đang được sử dụng hoặc bị trùng. Vui lòng kiểm tra lại.",
  VALIDATION_ERROR: "Vui lòng kiểm tra các thông tin đã nhập.",
  INVALID_REQUEST: "Thông tin gửi lên chưa đúng định dạng.",
  INTERNAL_ERROR: "Máy chủ chưa xử lý được yêu cầu. Vui lòng thử lại sau.",
};

function updateSession(value) {
  session = value;
  listeners.forEach((listener) => listener());
}
function tokenResponse(value) {
  if (
    !value ||
    typeof value.accessToken !== "string" ||
    typeof value.refreshToken !== "string" ||
    !value.user?.id ||
    !["CUSTOMER", "OWNER", "ADMIN"].includes(value.user.role)
  ) {
    throw new ApiError(
      "Phản hồi đăng nhập chưa hợp lệ. Vui lòng kiểm tra địa chỉ API.",
      502,
      "INVALID_RESPONSE",
    );
  }
  return value;
}

async function parse(response) {
  if (response.status === 204) return null;
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

async function send(path, options = {}, retry = true, authenticated = true) {
  const token = session?.accessToken;
  let response;
  try {
    response = await fetch(`${BASE}${path}`, {
      method: options.method || "GET",
      credentials: "omit",
      signal: options.signal,
      headers: {
        Accept: "application/json",
        ...(options.body !== undefined
          ? { "Content-Type": "application/json" }
          : {}),
        ...(authenticated && token ? { Authorization: `Bearer ${token}` } : {}),
      },
      ...(options.body !== undefined
        ? { body: JSON.stringify(options.body) }
        : {}),
    });
  } catch (error) {
    if (error.name === "AbortError") throw error;
    throw new ApiError(
      "Chưa kết nối được máy chủ. Kiểm tra backend đang chạy rồi thử lại.",
    );
  }
  if (
    response.status === 401 &&
    authenticated &&
    retry &&
    session?.refreshToken
  ) {
    if (session.accessToken === token) {
      if (!refreshing) {
        const refreshToken = session.refreshToken;
        refreshing = send(
          "/auth/refresh",
          { method: "POST", body: { refreshToken } },
          false,
          false,
        )
          .then((value) => {
            // A response that arrives after logout must never restore a session.
            if (session?.refreshToken === refreshToken)
              updateSession(tokenResponse(value));
          })
          .catch((error) => {
            if (session?.refreshToken === refreshToken) updateSession(null);
            throw error;
          })
          .finally(() => {
            refreshing = null;
          });
      }
      await refreshing;
    }
    if (!session)
      throw new ApiError(messages.UNAUTHORIZED, 401, "UNAUTHORIZED");
    return send(path, options, false, authenticated);
  }
  const value = await parse(response);
  if (!response.ok) {
    const code = value?.code || "";
    if (response.status === 401 && authenticated) updateSession(null);
    const message =
      messages[code] ||
      (response.status === 404
        ? "Không tìm thấy dữ liệu bạn yêu cầu."
        : response.status === 403
          ? messages.FORBIDDEN
          : response.status >= 500
            ? "Máy chủ chưa sẵn sàng. Vui lòng thử lại sau."
            : "Không thể thực hiện yêu cầu này.");
    throw new ApiError(message, response.status, code, value?.errors || {});
  }
  return value;
}

export const auth = {
  getSession: () => session,
  subscribe: (listener) => {
    listeners.add(listener);
    return () => listeners.delete(listener);
  },
  async login(body) {
    const value = tokenResponse(
      await send("/auth/login", { method: "POST", body }, false, false),
    );
    updateSession(value);
    return value;
  },
  async register(body) {
    const value = tokenResponse(
      await send("/auth/register", { method: "POST", body }, false, false),
    );
    updateSession(value);
    return value;
  },
  async logout() {
    const refreshToken = session?.refreshToken;
    updateSession(null);
    if (refreshToken)
      await send(
        "/auth/logout",
        { method: "POST", body: { refreshToken } },
        false,
        false,
      );
  },
  updateUser(user) {
    if (session) updateSession({ ...session, user });
  },
};

export const api = {
  get: (path, signal) => send(path, { signal }),
  post: (path, body) =>
    send(path, { method: "POST", ...(body !== undefined ? { body } : {}) }),
  put: (path, body) => send(path, { method: "PUT", body }),
  patch: (path, body) => send(path, { method: "PATCH", body }),
  delete: (path) => send(path, { method: "DELETE" }),
  async all(path, signal) {
    const separator = path.includes("?") ? "&" : "?";
    const output = [];
    for (let page = 0; page < 100; page++) {
      const value = await send(`${path}${separator}page=${page}&size=100`, {
        signal,
      });
      if (Array.isArray(value)) return value;
      if (!value || !Array.isArray(value.content))
        throw new ApiError(
          "Máy chủ trả về danh sách chưa hợp lệ.",
          502,
          "INVALID_RESPONSE",
        );
      output.push(...(value?.content || []));
      if (page + 1 >= (value?.page?.totalPages || 1)) return output;
    }
    throw new ApiError("Danh sách quá lớn. Hãy thu hẹp phạm vi tra cứu.");
  },
};
