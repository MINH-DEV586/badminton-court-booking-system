import React, {
  createContext,
  useContext,
  useEffect,
  useRef,
  useState,
} from "react";
import {
  AlertCircle,
  ArrowLeft,
  ChevronLeft,
  ChevronRight,
  LoaderCircle,
  X,
} from "lucide-react";
import { statuses } from "./domain.js";
import { api } from "./api.js";

export const AppContext = createContext(null);
export const useApp = () => useContext(AppContext);

export function useData(path, initial = null, all = false) {
  const { revision, user } = useApp();
  const [state, setState] = useState({
    value: initial,
    loading: Boolean(path),
    error: null,
  });
  const [version, setVersion] = useState(0);
  useEffect(() => {
    if (!path) {
      setState({ value: initial, loading: false, error: null });
      return;
    }
    const controller = new AbortController();
    let active = true;
    setState((previous) => ({ ...previous, loading: true, error: null }));
    (all ? api.all(path, controller.signal) : api.get(path, controller.signal))
      .then((value) => {
        if (active) setState({ value, loading: false, error: null });
      })
      .catch((error) => {
        if (active && error.name !== "AbortError")
          setState({ value: initial, loading: false, error });
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [path, revision, user?.id, version, all]);
  return { ...state, reload: () => setVersion((value) => value + 1) };
}
export const useCollection = (path) => useData(path, [], true);
export function Loading({ text = "Đang tải dữ liệu…" }) {
  return (
    <div className="loading" role="status">
      <LoaderCircle className="spin" size={21} />
      <span>{text}</span>
    </div>
  );
}
export function ErrorBox({ error, retry }) {
  return (
    error && (
      <div className="error-box" role="alert">
        <AlertCircle size={18} />
        <div>
          <p>{error.message || String(error)}</p>
          {retry && (
            <button className="text-button" onClick={retry}>
              Thử lại
            </button>
          )}
        </div>
      </div>
    )
  );
}
export function DataState({ data, children }) {
  return data.loading ? (
    <Loading />
  ) : data.error ? (
    <ErrorBox error={data.error} retry={data.reload} />
  ) : (
    children
  );
}
export function Empty({
  title = "Chưa có dữ liệu",
  text = "Thông tin sẽ xuất hiện tại đây khi có cập nhật.",
  action,
}) {
  return (
    <div className="empty">
      <span className="empty-court" aria-hidden="true" />
      <h3>{title}</h3>
      <p>{text}</p>
      {action}
    </div>
  );
}
export function Badge({ status, children, tone }) {
  const spec = statuses[status] || [children, tone || "green"];
  return (
    <span className={`badge ${tone || spec[1]}`}>{children || spec[0]}</span>
  );
}
export function Field({ label, children, hint, error, className = "" }) {
  const control =
    React.isValidElement(children) &&
    ["input", "select", "textarea"].includes(children.type)
      ? React.cloneElement(children, { "aria-label": label })
      : children;
  return (
    <label className={`field ${className}`}>
      <span>{label}</span>
      {control}
      {hint && <small>{hint}</small>}
      {error && <small className="field-error">{error}</small>}
    </label>
  );
}
export function Button({ children, loading, className = "", ...props }) {
  return (
    <button
      className={`button ${className}`}
      {...props}
      disabled={props.disabled || loading}
    >
      {loading && <LoaderCircle size={15} className="spin" />}
      {children}
    </button>
  );
}
export function Back({ to = "/venues", children = "Khám phá sân" }) {
  const { navigate } = useApp();
  return (
    <button className="back-link" onClick={() => navigate(to)}>
      <ArrowLeft size={14} />
      {children}
    </button>
  );
}
export function Pagination({ data, page, setPage }) {
  const total = data?.page?.totalPages || 0;
  return (
    total > 1 && (
      <div className="pagination">
        <span>
          Trang {page + 1} / {total}
        </span>
        <Button
          className="secondary compact"
          disabled={!page}
          onClick={() => setPage(page - 1)}
          aria-label="Trang trước"
        >
          <ChevronLeft size={16} />
        </Button>
        <Button
          className="secondary compact"
          disabled={page + 1 >= total}
          onClick={() => setPage(page + 1)}
          aria-label="Trang sau"
        >
          <ChevronRight size={16} />
        </Button>
      </div>
    )
  );
}
export function Modal({ title, children, close, wide = false }) {
  const ref = useRef(null);
  useEffect(() => {
    const dialog = ref.current;
    const previous = document.activeElement;
    dialog.showModal();
    return () => {
      dialog.close();
      previous?.focus?.();
    };
  }, []);
  return (
    <dialog
      ref={ref}
      className={`modal ${wide ? "wide" : ""}`}
      onCancel={(event) => {
        event.preventDefault();
        close();
      }}
      onClick={(event) => {
        if (event.target === event.currentTarget) close();
      }}
      aria-labelledby="modal-title"
    >
      <header className="modal-heading">
        <h2 id="modal-title">{title}</h2>
        <button className="icon-button" onClick={close} aria-label="Đóng">
          <X size={21} />
        </button>
      </header>
      <div className="modal-body">{children}</div>
    </dialog>
  );
}
export function Confirm({ title, text, accept, close, danger = false }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  return (
    <Modal
      title={title}
      close={() => {
        if (!busy) close();
      }}
    >
      <p className="muted">{text}</p>
      <ErrorBox error={error} />
      <div className="form-actions">
        <Button className="secondary" onClick={close} disabled={busy}>
          Quay lại
        </Button>
        <Button
          className={danger ? "danger" : ""}
          loading={busy}
          onClick={async () => {
            setBusy(true);
            try {
              await accept();
              close();
            } catch (error) {
              setError(error);
              setBusy(false);
            }
          }}
        >
          Xác nhận
        </Button>
      </div>
    </Modal>
  );
}
