import { test, expect } from "@playwright/test";
import { addDays, localDate } from "../src/domain.js";

const unique = Date.now().toString(36);
let errors = [];
test.beforeEach(async ({ page }) => {
  errors = [];
  page.on("pageerror", (error) => errors.push(error.message));
});
test.afterEach(() => {
  expect(errors, "No uncaught frontend errors").toEqual([]);
});
async function login(page, role = "Người chơi") {
  await page.goto("/#/login");
  await page.getByRole("button", { name: role, exact: true }).click();
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await expect(
    page.getByRole("heading", {
      name: role === "Chủ sân" ? "Quản lý sân của bạn" : "Hẹn nhau ra sân.",
    }),
  ).toBeVisible();
}
async function binhThanh(page) {
  await page
    .locator(".venue-card")
    .filter({ hasText: "Cụm sân Bình Thạnh" })
    .getByRole("button", { name: "Xem cụm sân" })
    .click();
  await expect(
    page.getByRole("heading", { name: "Cụm sân Bình Thạnh", exact: true }),
  ).toBeVisible();
  await expect(page.locator(".court-row")).toHaveCount(3);
}

test("login matches the reference, shows genuine credential errors, and works on mobile", async ({
  page,
}) => {
  await page.goto("/#/login");
  await expect(page.getByText("LET’S PLAY.")).toBeVisible();
  await page
    .getByLabel("Email", { exact: true })
    .fill("minhanh@badmintonhub.test");
  await page.getByLabel("Mật khẩu", { exact: true }).fill("incorrect");
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await expect(page.getByRole("alert")).toContainText(
    "Email hoặc mật khẩu chưa đúng",
  );
  await page.screenshot({
    path: "test-results/login-desktop.png",
    fullPage: true,
  });
  await page.setViewportSize({ width: 390, height: 844 });
  await expect(
    page.getByRole("button", { name: "Đăng nhập", exact: true }),
  ).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBeTruthy();
  await page.screenshot({
    path: "test-results/login-mobile.png",
    fullPage: true,
  });
});

test("customer opens venue, creates a real booking, records payment and cancels it", async ({
  page,
}) => {
  await login(page);
  await binhThanh(page);
  await expect(
    page
      .locator(".court-row")
      .filter({ hasText: "Sân 03" })
      .getByRole("button", { name: "Tạm ngưng" }),
  ).toBeDisabled();
  await page.screenshot({
    path: "test-results/venue-desktop.png",
    fullPage: true,
  });
  await page
    .locator(".court-row")
    .filter({ hasText: "Sân 01" })
    .getByRole("button", { name: "Đặt sân", exact: true })
    .click();
  await page
    .getByLabel("Ngày chơi", { exact: true })
    .fill(addDays(localDate(), 7));
  await page.getByLabel("Giờ bắt đầu", { exact: true }).fill("09:00");
  await page.getByLabel("Giờ kết thúc", { exact: true }).fill("10:00");
  await page.getByRole("button", { name: "Xác nhận giữ chỗ" }).click();
  await expect(
    page.getByRole("heading", { name: "Chi tiết lịch hẹn" }),
  ).toBeVisible();
  await expect(page.getByText("Chờ thanh toán", { exact: true })).toBeVisible();
  await page
    .getByRole("button", { name: "Tạo yêu cầu thanh toán", exact: true })
    .click();
  await page.getByRole("button", { name: "Ghi nhận yêu cầu" }).click();
  await expect(page.getByText("Đang chờ", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Hủy lịch đặt", exact: true }).click();
  await page.getByRole("button", { name: "Xác nhận", exact: true }).click();
  await expect(
    page.locator(".page-heading").getByText("Đã hủy", { exact: true }),
  ).toBeVisible();
  await page.setViewportSize({ width: 390, height: 844 });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBeTruthy();
});

test("registration creates a real account and profile changes persist", async ({
  page,
}) => {
  await page.goto("/#/register");
  await page
    .getByLabel("Họ và tên", { exact: true })
    .fill(`Kiểm thử ${unique}`);
  await page
    .getByLabel("Email", { exact: true })
    .fill(`frontend-${unique}@example.test`);
  await page.getByLabel("Mật khẩu", { exact: true }).fill("Preview123!");
  await page.getByRole("button", { name: "Đăng ký", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Hẹn nhau ra sân." }),
  ).toBeVisible();
  await page.locator(".profile-link").click();
  await page.getByLabel("Họ và tên", { exact: true }).fill(`Hồ sơ ${unique}`);
  await page.getByLabel("Số điện thoại", { exact: true }).fill("0912345678");
  await page.getByRole("button", { name: "Lưu thay đổi" }).click();
  await expect(page.getByRole("status")).toContainText(
    "Đã lưu thông tin cá nhân",
  );
  await expect(page.locator(".profile-link")).toContainText(`Hồ sơ ${unique}`);
});

test("owner creates and edits a venue and court, then manages price rules and blocks", async ({
  page,
}) => {
  test.setTimeout(90000);
  await login(page, "Chủ sân");
  await page.getByRole("button", { name: "Quản lý sân", exact: true }).click();
  await page.getByRole("button", { name: "Cụm sân", exact: true }).click();
  await page
    .getByRole("button", { name: "Thêm cụm sân", exact: true })
    .first()
    .click();
  await page
    .getByLabel("Tên cụm sân", { exact: true })
    .fill(`Cụm sân QA ${unique}`);
  await page
    .getByLabel("Địa chỉ", { exact: true })
    .fill("Địa chỉ kiểm thử trải nghiệm");
  await page.getByRole("button", { name: "Lưu thông tin" }).click();
  await expect(
    page.getByRole("heading", { name: `Cụm sân QA ${unique}`, exact: true }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Sân cầu lông", exact: true }).click();
  await page
    .getByLabel("Cụm sân", { exact: true })
    .selectOption({ label: `Cụm sân QA ${unique}` });
  await page.getByRole("button", { name: "Thêm sân", exact: true }).click();
  await page.getByLabel("Tên sân", { exact: true }).fill("Sân QA 01");
  await page
    .getByLabel("Giá cơ bản / giờ (VND)", { exact: true })
    .fill("90000");
  await page.getByRole("button", { name: "Lưu thông tin" }).click();
  await expect(
    page.getByRole("heading", { name: "Sân QA 01", exact: true }),
  ).toBeVisible();
  await page
    .getByRole("button", { name: "Sửa Sân QA 01", exact: true })
    .click();
  await page
    .getByLabel("Giá cơ bản / giờ (VND)", { exact: true })
    .fill("95000");
  await page.getByRole("button", { name: "Lưu thông tin" }).click();
  await expect(page.getByText("95.000đ / giờ", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Quy tắc giá", exact: true }).click();
  await page
    .getByRole("button", { name: "Thêm quy tắc giá", exact: true })
    .click();
  await page.getByRole("button", { name: "Lưu thông tin" }).click();
  await expect(page.locator("table tbody tr")).toHaveCount(1);
  await page.getByRole("button", { name: "Xóa", exact: true }).click();
  await page.getByRole("button", { name: "Xác nhận", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Chưa có quy tắc giá" }),
  ).toBeVisible();
  await page
    .getByRole("button", { name: "Khóa lịch sân", exact: true })
    .click();
  await page
    .getByRole("button", { name: "Thêm lịch khóa sân", exact: true })
    .click();
  const date = addDays(localDate(), 10);
  await page.getByLabel("Bắt đầu khóa", { exact: true }).fill(`${date}T12:00`);
  await page.getByLabel("Kết thúc khóa", { exact: true }).fill(`${date}T14:00`);
  await page.getByLabel("Lý do", { exact: true }).fill("Lịch khóa kiểm thử");
  await page.getByRole("button", { name: "Lưu thông tin" }).click();
  await expect(
    page.getByRole("heading", { name: "Lịch khóa kiểm thử" }),
  ).toBeVisible();
  await page
    .getByRole("button", { name: "Hủy lịch khóa", exact: true })
    .click();
  await page.getByRole("button", { name: "Xác nhận", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Chưa có lịch khóa sân" }),
  ).toBeVisible();
});

test("admin changes another account role and runs expiry reconciliation", async ({
  page,
  request,
}) => {
  const registered = await request.post("/api/auth/register", {
    data: {
      email: `admin-target-${unique}@example.test`,
      password: "Preview123!",
      fullName: "Tài khoản kiểm thử quản trị",
    },
  });
  expect(registered.ok()).toBeTruthy();
  await login(page, "Admin");
  await page.getByRole("button", { name: "Quản trị", exact: true }).click();
  const row = page
    .locator("tr")
    .filter({ hasText: `admin-target-${unique}@example.test` });
  await row.getByRole("combobox").selectOption("OWNER");
  await page.getByRole("button", { name: "Xác nhận", exact: true }).click();
  await expect(row.getByRole("combobox")).toHaveValue("OWNER");
  await page.getByRole("button", { name: "Vận hành", exact: true }).click();
  await page.getByRole("button", { name: "Kiểm tra và cập nhật" }).click();
  await page.getByRole("button", { name: "Xác nhận", exact: true }).click();
  await expect(page.getByRole("status")).toContainText("lịch đặt hết hạn");
});

test("admin reconciles a payment and its venue owner records a refund", async ({
  page,
  request,
}) => {
  test.setTimeout(90000);
  const signedIn = await request.post("/api/auth/login", {
    data: { email: "minhanh@badmintonhub.test", password: "Preview123!" },
  });
  expect(signedIn.ok()).toBeTruthy();
  const session = await signedIn.json();
  const headers = { Authorization: `Bearer ${session.accessToken}` };
  const venues = await request.get("/api/venues?size=100", { headers });
  const venue = (await venues.json()).content.find(
    (item) => item.name === "Cụm sân Bình Thạnh",
  );
  const courts = await request.get(`/api/venues/${venue.id}/courts?size=100`, {
    headers,
  });
  const court = (await courts.json()).content.find(
    (item) => item.name === "Sân 01",
  );
  const date = addDays(localDate(), 14);
  const booked = await request.post("/api/bookings", {
    headers,
    data: {
      courtId: court.id,
      startsAt: `${date}T02:00:00Z`,
      endsAt: `${date}T03:00:00Z`,
    },
  });
  expect(booked.ok()).toBeTruthy();
  const booking = await booked.json();
  const createdPayment = await request.post("/api/payments", {
    headers,
    data: { bookingId: booking.id, provider: "PREVIEW" },
  });
  expect(createdPayment.ok()).toBeTruthy();

  await login(page, "Admin");
  await page.getByRole("button", { name: "Quản trị", exact: true }).click();
  await page
    .getByRole("button", { name: "Đối soát thanh toán", exact: true })
    .click();
  await page.getByLabel("Mã đặt sân để đối soát").fill(booking.id);
  await page.getByRole("button", { name: "Tra cứu", exact: true }).click();
  await page.getByRole("button", { name: "Xác nhận", exact: true }).click();
  await page.getByLabel("Mã giao dịch nhà cung cấp").fill(`QA-${unique}`);
  await page
    .getByRole("button", { name: "Xác nhận thành công", exact: true })
    .click();
  await expect(page.getByText("Thành công", { exact: true })).toBeVisible();
  await page
    .getByRole("button", { name: "Đổi tài khoản", exact: true })
    .click();

  await login(page, "Chủ sân");
  await page.evaluate((id) => {
    window.location.hash = `/bookings/${id}`;
  }, booking.id);
  await expect(
    page.getByRole("heading", { name: "Chi tiết lịch hẹn" }),
  ).toBeVisible();
  await page
    .getByRole("button", { name: "Ghi nhận hoàn tiền", exact: true })
    .click();
  await page.getByRole("button", { name: "Xác nhận", exact: true }).click();
  await expect(page.getByText("Đã hoàn tiền", { exact: true })).toBeVisible();
  await expect(
    page.locator(".page-heading").getByText("Đã hủy", { exact: true }),
  ).toBeVisible();
});

test("owner lands on their own dashboard and sees accurate daily occupancy", async ({
  page,
  request,
}) => {
  test.setTimeout(90000);
  async function headersFor(email) {
    const response = await request.post("/api/auth/login", {
      data: { email, password: "Preview123!" },
    });
    expect(response.ok()).toBeTruthy();
    return { Authorization: `Bearer ${(await response.json()).accessToken}` };
  }
  const customerHeaders = await headersFor("minhanh@badmintonhub.test");
  const ownerHeaders = await headersFor("chusan@badmintonhub.test");
  const venues = await request.get("/api/owner/venues?size=100", {
    headers: ownerHeaders,
  });
  const venue = (await venues.json()).content.find(
    (item) => item.name === "Cụm sân Bình Thạnh",
  );
  const courts = await request.get(
    `/api/venues/${venue.id}/courts?activeOnly=false&size=100`,
    { headers: ownerHeaders },
  );
  const court = (await courts.json()).content.find(
    (item) => item.name === "Sân 01",
  );
  const day = addDays(localDate(), 21);
  let bookingId;
  let blockId;
  try {
    const booked = await request.post("/api/bookings", {
      headers: customerHeaders,
      data: {
        courtId: court.id,
        startsAt: `${day}T02:00:00Z`,
        endsAt: `${day}T03:00:00Z`,
      },
    });
    expect(booked.ok()).toBeTruthy();
    bookingId = (await booked.json()).id;
    const blocked = await request.post("/api/owner/court-blocks", {
      headers: ownerHeaders,
      data: {
        courtId: court.id,
        startsAt: `${day}T05:00:00Z`,
        endsAt: `${day}T06:00:00Z`,
        reason: "QA kiểm tra lịch trống",
      },
    });
    expect(blocked.ok()).toBeTruthy();
    blockId = (await blocked.json()).id;
    await login(page, "Chủ sân");
    await expect(page).toHaveURL(/#\/owner$/);
    const navigation = page.getByRole("navigation", {
      name: "Điều hướng chính",
    });
    await expect(
      navigation.getByRole("button", { name: "Lịch đặt của tôi" }),
    ).toHaveCount(0);
    await expect(
      navigation.getByRole("button", { name: "Khám phá sân" }),
    ).toHaveCount(0);
    await page.getByLabel("Cụm sân xem lịch").selectOption(venue.id);
    await page.getByLabel("Ngày xem lịch").fill(day);
    await expect(page.locator(".owner-schedule")).toHaveCount(3);
    await expect(
      page.locator(".owner-metrics .stat").nth(2).locator("strong"),
    ).toHaveText("1");
    await expect(
      page.locator(".owner-metrics .stat").nth(3).locator("strong"),
    ).toHaveText("1");
    const schedule = page
      .locator(".owner-schedule")
      .filter({
        has: page.getByRole("heading", { name: "Sân 01", exact: true }),
      });
    await expect(schedule.locator(".free-windows span")).toHaveText([
      "00:00 – 09:00",
      "10:00 – 12:00",
      "13:00 – 24:00",
    ]);
    await expect(
      schedule.getByText("QA kiểm tra lịch trống", { exact: false }),
    ).toBeVisible();
    await page.screenshot({
      path: "test-results/owner-overview-desktop.png",
      fullPage: true,
    });
    await page.setViewportSize({ width: 390, height: 844 });
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBeTruthy();
    await page.screenshot({
      path: "test-results/owner-overview-mobile.png",
      fullPage: true,
    });
    await page.evaluate(() => {
      window.location.hash = "/bookings";
    });
    await expect(page).toHaveURL(/#\/owner$/);
    await expect(
      page.getByRole("heading", { name: "Quản lý sân của bạn" }),
    ).toBeVisible();
  } finally {
    if (bookingId)
      await request.post(`/api/bookings/${bookingId}/cancel`, {
        headers: ownerHeaders,
      });
    if (blockId)
      await request.post(`/api/owner/court-blocks/${blockId}/cancel`, {
        headers: ownerHeaders,
      });
  }
});
