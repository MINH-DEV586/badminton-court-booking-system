import { test, expect } from "@playwright/test";

test("registration offers customer and owner roles without generating accounts", async ({
  page,
}) => {
  const writes = [];
  page.on("request", (request) => {
    if (
      ["POST", "PUT", "PATCH", "DELETE"].includes(request.method()) &&
      new URL(request.url()).pathname.startsWith("/api/")
    )
      writes.push(request.url());
  });
  await page.goto("/#/register");
  const role = page.getByLabel("Loại tài khoản", { exact: true });
  await expect(role.locator("option")).toHaveText([
    "Khách hàng",
    "Chủ sân",
  ]);
  await expect(role).toHaveValue("CUSTOMER");
  await role.selectOption("OWNER");
  await expect(role).toHaveValue("OWNER");
  await page.setViewportSize({ width: 390, height: 844 });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBeTruthy();
  await page.screenshot({
    path: "test-results/register-role-mobile.png",
    fullPage: true,
  });
  await expect(page.getByLabel("Email", { exact: true })).toHaveValue("");
  expect(writes).toEqual([]);
});

test("login starts empty and authenticates only against the backend", async ({
  page,
}) => {
  const errors = [];
  page.on("pageerror", (error) => errors.push(error.message));
  await page.goto("/#/login");
  await expect(page.getByLabel("Email", { exact: true })).toHaveValue("");
  await expect(page.getByLabel("Mật khẩu", { exact: true })).toHaveValue("");
  await expect(page.getByText("TÀI KHOẢN TRẢI NGHIỆM")).toHaveCount(0);
  await page
    .getByLabel("Email", { exact: true })
    .fill("read-only-check@invalid.example");
  await page.getByLabel("Mật khẩu", { exact: true }).fill("invalid-password");
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await expect(page.getByRole("alert")).toContainText(
    "Email hoặc mật khẩu chưa đúng",
  );
  await page.setViewportSize({ width: 390, height: 844 });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBeTruthy();
  expect(errors).toEqual([]);
});

test("protected pages require a database account", async ({ page }) => {
  await page.goto("/#/owner");
  await expect(page).toHaveURL(/#\/login$/);
  await expect(
    page.getByRole("heading", { name: "Đăng nhập", exact: true }),
  ).toBeVisible();
});

test("an existing account can load its data without generating business records", async ({
  page,
}) => {
  test.skip(
    !process.env.E2E_EMAIL || !process.env.E2E_PASSWORD,
    "Provide credentials for an existing database account.",
  );
  const businessWrites = [];
  page.on("request", (request) => {
    const path = new URL(request.url()).pathname;
    if (
      ["POST", "PUT", "PATCH", "DELETE"].includes(request.method()) &&
      path.startsWith("/api/") &&
      !path.startsWith("/api/auth/")
    )
      businessWrites.push(path);
  });
  await page.goto("/#/login");
  await page.getByLabel("Email", { exact: true }).fill(process.env.E2E_EMAIL);
  await page
    .getByLabel("Mật khẩu", { exact: true })
    .fill(process.env.E2E_PASSWORD);
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await expect(page).toHaveURL(/#\/(venues|owner)$/);
  await expect(
    page.getByRole("navigation", { name: "Điều hướng chính" }),
  ).toBeVisible();
  await expect(page.getByRole("alert")).toHaveCount(0);
  expect(businessWrites).toEqual([]);
});
