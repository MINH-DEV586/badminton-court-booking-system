import React, {
  useEffect,
  useRef,
  useState,
  useSyncExternalStore,
} from "react";
import { Check, LogOut, UserRound, X } from "lucide-react";
import { api, auth } from "./api.js";
import { roles } from "./domain.js";
import { AppContext, Modal } from "./ui.jsx";
import {
  AuthPage,
  VenueList,
  VenueDetail,
  BookingCreate,
  BookingList,
  BookingDetail,
  Profile,
} from "./pages.jsx";
import { OwnerPage, AdminPage } from "./management.jsx";

const currentPath = () =>
  (window.location.hash.slice(1) || "/login").split("?")[0];
export const isPreview =
  import.meta.env.DEV && import.meta.env.VITE_PREVIEW_MODE === "true";

export default function App() {
  const session = useSyncExternalStore(
    auth.subscribe,
    auth.getSession,
    () => null,
  );
  const [path, setPath] = useState(currentPath);
  const [revision, setRevision] = useState(0);
  const [toast, setToast] = useState(null);
  const [info, setInfo] = useState(null);
  const [loggingOut, setLoggingOut] = useState(false);
  const previousUser = useRef(null);
  const user = session?.user;
  const role = user?.role;
  const home = role === "OWNER" ? "/owner" : "/venues";
  function navigate(next) {
    window.location.hash = next;
    setPath(next.split("?")[0]);
    window.scrollTo({ top: 0, behavior: "instant" });
  }
  function notify(message) {
    setToast({ message, id: Date.now() });
  }
  useEffect(() => {
    const listener = () => {
      setPath(currentPath());
      window.scrollTo(0, 0);
    };
    window.addEventListener("hashchange", listener);
    return () => window.removeEventListener("hashchange", listener);
  }, []);
  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => setToast(null), 5500);
    return () => clearTimeout(timer);
  }, [toast]);
  useEffect(() => {
    if (user && (path === "/login" || path === "/register")) navigate(home);
    if (!user && !["/login", "/register"].includes(path)) navigate("/login");
    if (user && path === "/owner" && !["OWNER", "ADMIN"].includes(role))
      navigate("/venues");
    if (user && path === "/admin" && role !== "ADMIN") navigate("/venues");
    if (
      role === "OWNER" &&
      (path.startsWith("/venues") || path === "/bookings")
    )
      navigate("/owner");
    if (previousUser.current && !user && !loggingOut)
      notify("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
    previousUser.current = user?.id || null;
  }, [user?.id, role, path]);
  async function logout() {
    setLoggingOut(true);
    try {
      await auth.logout();
    } catch {
      notify(
        "Đã đăng xuất trên thiết bị. Yêu cầu thu hồi phiên chưa gửi được tới máy chủ.",
      );
    } finally {
      navigate("/login");
      setLoggingOut(false);
    }
  }
  let page;
  const parts = path.split("/").filter(Boolean);
  if (!user) page = <AuthPage register={path === "/register"} />;
  else if (role === "OWNER" && (parts[0] === "venues" || path === "/bookings"))
    page = <OwnerPage />;
  else if (parts[0] === "venues" && parts[2] === "book")
    page = <BookingCreate venueId={parts[1]} />;
  else if (parts[0] === "venues" && parts[1])
    page = <VenueDetail id={parts[1]} />;
  else if (parts[0] === "bookings" && parts[1])
    page = <BookingDetail id={parts[1]} />;
  else if (parts[0] === "bookings") page = <BookingList />;
  else if (parts[0] === "profile") page = <Profile />;
  else if (parts[0] === "owner" && ["OWNER", "ADMIN"].includes(role))
    page = <OwnerPage />;
  else if (parts[0] === "admin" && role === "ADMIN") page = <AdminPage />;
  else page = role === "OWNER" ? <OwnerPage /> : <VenueList />;
  return (
    <AppContext.Provider
      value={{
        user,
        role,
        path,
        navigate,
        notify,
        revision,
        refresh: () => setRevision((value) => value + 1),
        logout,
        api,
      }}
    >
      <div className="app-shell">
        <header className="site-header">
          <div className="header-inner">
            <button
              className="wordmark"
              onClick={() => navigate(user ? home : "/login")}
              aria-label="BadmintonHub — trang chủ"
            >
              badmintonhub<span>.</span>
            </button>
            {!user ? (
              <>
                <span className="tagline">
                  Kết nối đam mê. Sẵn sàng ra sân.
                </span>
                <div className="header-auth">
                  <span>
                    {path === "/register"
                      ? "Đã có tài khoản?"
                      : "Chưa có tài khoản?"}
                  </span>
                  <button
                    className="button secondary"
                    onClick={() =>
                      navigate(path === "/register" ? "/login" : "/register")
                    }
                  >
                    {path === "/register" ? "Đăng nhập" : "Đăng ký"}
                  </button>
                </div>
              </>
            ) : (
              <>
                <nav className="main-nav" aria-label="Điều hướng chính">
                  {role !== "OWNER" && (
                    <>
                      <button
                        className={parts[0] === "venues" ? "active" : ""}
                        onClick={() => navigate("/venues")}
                      >
                        Khám phá sân
                      </button>
                      <button
                        className={parts[0] === "bookings" ? "active" : ""}
                        onClick={() => navigate("/bookings")}
                      >
                        Lịch đặt của tôi
                      </button>
                    </>
                  )}
                  {["OWNER", "ADMIN"].includes(role) && (
                    <button
                      className={path === "/owner" ? "active" : ""}
                      onClick={() => navigate("/owner")}
                    >
                      Quản lý sân
                    </button>
                  )}
                  {role === "ADMIN" && (
                    <button
                      className={path === "/admin" ? "active" : ""}
                      onClick={() => navigate("/admin")}
                    >
                      Quản trị
                    </button>
                  )}
                </nav>
                <div className="header-account">
                  <button
                    className="profile-link"
                    onClick={() => navigate("/profile")}
                  >
                    <span>Hồ sơ</span>
                    <span className="slash">/</span>
                    <strong>{user.fullName}</strong>
                    <UserRound size={17} />
                  </button>
                  <span className="role-label">{roles[role]}</span>
                </div>
              </>
            )}
          </div>
        </header>
        {user && isPreview && (
          <div className="preview-strip">
            <span className="status-dot" />
            Máy chủ trải nghiệm · Dữ liệu mẫu được lưu trong bộ nhớ, không phải
            đặt sân thật.
            <button onClick={logout}>
              <LogOut size={13} />
              Đổi tài khoản
            </button>
          </div>
        )}
        <main className={user ? "main-content wrap" : "auth-main wrap"}>
          {page}
        </main>
        <footer className="site-footer wrap">
          <div>
            <span>© {new Date().getFullYear()} BadmintonHub</span>
            <span>Một lịch hẹn. Thêm một trận hay.</span>
          </div>
          <div>
            <button onClick={() => setInfo("terms")}>Điều khoản</button>
            <button onClick={() => setInfo("help")}>Hỗ trợ</button>
          </div>
        </footer>
        {toast && (
          <div className="toast" role="status">
            <Check size={18} />
            <span>{toast.message}</span>
            <button aria-label="Đóng thông báo" onClick={() => setToast(null)}>
              <X size={16} />
            </button>
          </div>
        )}
        {info && (
          <Modal
            title={info === "terms" ? "Thông tin sử dụng" : "Bạn cần hỗ trợ?"}
            close={() => setInfo(null)}
          >
            {info === "terms" ? (
              <div className="prose">
                <p>
                  Kiểm tra địa điểm, múi giờ và tổng tiền trước khi tạo lịch
                  đặt. Giá hiển thị ban đầu là giá cơ bản; giá xác nhận nằm
                  trong chi tiết đơn đặt.
                </p>
                <p>
                  Chính sách hủy và thanh toán được áp dụng theo trạng thái của
                  lịch đặt. Không chia sẻ tài khoản hoặc thông tin đăng nhập.
                </p>
                {isPreview && (
                  <p>
                    Bản chạy hiện tại dùng dữ liệu trải nghiệm; không có giao
                    dịch chuyển tiền thật.
                  </p>
                )}
              </div>
            ) : (
              <div className="prose">
                <p>
                  Nếu không đăng nhập được, kiểm tra email và mật khẩu rồi thử
                  lại. Nếu mất kết nối, chờ máy chủ sẵn sàng và dùng nút “Thử
                  lại”.
                </p>
                <p>
                  Với vấn đề lịch sân hoặc thanh toán, lưu mã đặt sân và liên hệ
                  người quản lý cụm sân để đối chiếu. Hiện chưa có kênh thanh
                  toán trực tuyến tự động.
                </p>
              </div>
            )}
          </Modal>
        )}
      </div>
    </AppContext.Provider>
  );
}
