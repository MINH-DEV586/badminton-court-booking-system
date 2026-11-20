import test from "node:test";
import assert from "node:assert/strict";
import { api, auth } from "../src/api.js";

const account = {
  id: "00000000-0000-4000-a000-000000000001",
  fullName: "Test",
  role: "CUSTOMER",
};
const tokens = (suffix = "old") => ({
  accessToken: `access-${suffix}`,
  refreshToken: `refresh-${suffix}`,
  user: account,
});
const json = (value, status = 200) =>
  new Response(JSON.stringify(value), {
    status,
    headers: { "Content-Type": "application/json" },
  });
function install(t, handler) {
  const original = globalThis.fetch;
  globalThis.fetch = handler;
  t.after(async () => {
    globalThis.fetch = async () => new Response(null, { status: 204 });
    await auth.logout();
    globalThis.fetch = original;
  });
}

test("login uses JSON, omits cookies and maps invalid credentials into Vietnamese", async (t) => {
  install(t, async (url, options) => {
    assert.equal(url, "/api/auth/login");
    assert.equal(options.credentials, "omit");
    assert.equal(options.headers.Authorization, undefined);
    assert.equal(JSON.parse(options.body).email, "test@example.com");
    return json({ code: "INVALID_CREDENTIALS" }, 401);
  });
  await assert.rejects(
    auth.login({ email: "test@example.com", password: "incorrect" }),
    (error) =>
      error.status === 401 && error.message.includes("Email hoặc mật khẩu"),
  );
  assert.equal(auth.getSession(), null);
});
test("concurrent expired access tokens perform a single refresh and retry with the new bearer token", async (t) => {
  let refreshCount = 0;
  install(t, async (url, options) => {
    if (url.endsWith("/auth/login")) return json(tokens());
    if (url.endsWith("/auth/refresh")) {
      refreshCount++;
      await new Promise((resolve) => setTimeout(resolve, 20));
      return json(tokens("new"));
    }
    if (options.headers.Authorization === "Bearer access-old")
      return json({ code: "UNAUTHORIZED" }, 401);
    assert.equal(options.headers.Authorization, "Bearer access-new");
    return json({ ok: true });
  });
  await auth.login({});
  const result = await Promise.all([
    api.get("/venues"),
    api.get("/bookings/me"),
  ]);
  assert.equal(refreshCount, 1);
  assert.equal(result.length, 2);
  assert.equal(auth.getSession().accessToken, "access-new");
});
test("an in-flight refresh cannot restore a session after the user logs out", async (t) => {
  let release;
  let begin;
  const started = new Promise((resolve) => {
    begin = resolve;
  });
  install(t, async (url) => {
    if (url.endsWith("/auth/login")) return json(tokens());
    if (url.endsWith("/auth/refresh")) {
      begin();
      return new Promise((resolve) => {
        release = () => resolve(json(tokens("new")));
      });
    }
    if (url.endsWith("/auth/logout"))
      return new Response(null, { status: 204 });
    return json({ code: "UNAUTHORIZED" }, 401);
  });
  await auth.login({});
  const pending = api.get("/venues");
  await started;
  await auth.logout();
  release();
  await assert.rejects(pending, (error) => error.status === 401);
  assert.equal(auth.getSession(), null);
});
test("forbidden actions do not cause a refresh or destroy the current session", async (t) => {
  let calls = 0;
  install(t, async (url) => {
    calls++;
    return url.endsWith("/auth/login")
      ? json(tokens())
      : json({ code: "FORBIDDEN" }, 403);
  });
  await auth.login({});
  await assert.rejects(
    api.get("/admin/users"),
    (error) => error.status === 403,
  );
  assert.equal(calls, 2);
  assert.equal(auth.getSession().user.id, account.id);
});
test("collection loading follows the backend PagedModel and rejects malformed success responses", async (t) => {
  install(t, async (url) =>
    url.includes("page=0")
      ? json({ content: [{ id: 1 }], page: { totalPages: 2 } })
      : json({ content: [{ id: 2 }], page: { totalPages: 2 } }),
  );
  assert.deepEqual(await api.all("/venues"), [{ id: 1 }, { id: 2 }]);
  globalThis.fetch = async () => json({ unexpected: true });
  await assert.rejects(
    api.all("/venues"),
    (error) => error.code === "INVALID_RESPONSE",
  );
});
test("a success response containing HTML or an incomplete login object is not accepted as authentication", async (t) => {
  install(
    t,
    async () => new Response("<html>not an API</html>", { status: 200 }),
  );
  await assert.rejects(
    auth.login({}),
    (error) => error.code === "INVALID_RESPONSE",
  );
  assert.equal(auth.getSession(), null);
});
