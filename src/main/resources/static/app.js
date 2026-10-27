const $ = id => document.getElementById(id);

let isSignup = false;
let vehicles = [];
let currentUser = null;
let toastTimer;

async function api(url, options = {}) {
  const config = {
    credentials: "same-origin",
    ...options,
    headers: {
      ...(options.body
        ? { "Content-Type": "application/json" }
        : {}),
      ...(options.headers || {})
    }
  };

  const response = await fetch(url, config);

  if (response.status === 204) return null;

  const data = await response.json().catch(() => ({}));

  if (!response.ok) {
    throw new Error(
      data.message || `Request failed (${response.status})`
    );
  }

  return data;
}

function notify(message, error = false) {
  const el = $("toast");

  if (!el) {
    console.log(message);
    return;
  }

  el.textContent = message;
  el.classList.toggle("error", error);
  el.classList.remove("hidden");

  clearTimeout(toastTimer);

  toastTimer = setTimeout(() => {
    el.classList.add("hidden");
  }, 3200);
}

function authMode(signup) {
  isSignup = signup;

  $("loginTab").classList.toggle("active", !signup);
  $("signupTab").classList.toggle("active", signup);

  $("nameLabel").classList.toggle("hidden", !signup);
  $("authName").classList.toggle("hidden", !signup);
  $("authName").required = signup;

  $("authTitle").textContent = signup
    ? "Create your account"
    : "Welcome back";

  $("authSubtitle").textContent = signup
    ? "Start managing your vehicles."
    : "Log in to manage your vehicles.";

  $("authSubmit").textContent = signup
    ? "Create account"
    : "Login";

  $("authPassword").autocomplete = signup
    ? "new-password"
    : "current-password";

  $("authMessage").textContent = "";
  $("authMessage").classList.remove("ok");
}

$("loginTab").addEventListener("click", () => authMode(false));
$("signupTab").addEventListener("click", () => authMode(true));


// LOGIN AND SIGNUP
$("authForm").addEventListener("submit", async event => {
  event.preventDefault();

  const message = $("authMessage");
  const button = $("authSubmit");

  message.textContent = "Please wait...";
  message.classList.remove("ok");
  button.disabled = true;

  const body = {
    email: $("authEmail").value.trim(),
    password: $("authPassword").value
  };

  if (isSignup) {
    body.name = $("authName").value.trim();
  }

  try {
    if (isSignup) {
      await api("/api/auth/signup", {
        method: "POST",
        body: JSON.stringify(body)
      });

      message.textContent =
        "Account created. You can now log in.";

      message.classList.add("ok");

      $("authPassword").value = "";

      authMode(false);
      $("authEmail").value = body.email;

    } else {
      await api("/api/auth/login", {
        method: "POST",
        body: JSON.stringify(body)
      });

      $("authPassword").value = "";

      // Login successful: open dashboard.
      await showApp();
    }

  } catch (err) {
    console.error("Login/signup error:", err);

    message.textContent =
      err.message || "Something went wrong. Please try again.";

  } finally {
    button.disabled = false;
  }
});


// SHOW LOGIN PAGE
function showAuth() {
  $("appView").classList.add("hidden");
  $("authView").classList.remove("hidden");
}


// SHOW DASHBOARD
async function showApp() {
  try {
    // Verify the logged-in user.
    currentUser = await api("/api/auth/me");

    $("welcomeName").textContent =
      currentUser.name || currentUser.email || "User";

    // Show dashboard before loading its data.
    $("authView").classList.add("hidden");
    $("appView").classList.remove("hidden");

    $("authMessage").textContent = "";

    try {
      await loadDashboard();
    } catch (err) {
      console.error("Dashboard loading error:", err);

      // Keep the user on the dashboard if data loading fails.
      notify(
        "Dashboard opened, but some data could not load: " +
        err.message,
        true
      );
    }

  } catch (err) {
    console.error("Authentication verification error:", err);

    showAuth();

    $("authMessage").textContent =
      "Session verification failed. Please log in again.";
  }
}


// LOGOUT
$("logoutBtn").addEventListener("click", async () => {
  try {
    await api("/api/auth/logout", {
      method: "POST"
    });
  } catch (err) {
    console.error("Logout error:", err);
  }

  vehicles = [];
  currentUser = null;

  showAuth();

  $("authPassword").value = "";

  notify("Logged out successfully.");
});


// VEHICLE FORM
function openVehicleForm(v = null) {
  $("vehicleForm").reset();
  $("vehicleFormMessage").textContent = "";

  $("vehicleId").value = v ? v.id : "";

  $("vehicleFormTitle").textContent = v
    ? "Edit vehicle"
    : "Add vehicle";

  if (v) {
    $("vehicleName").value = v.name || "";

    $("registrationNumber").value =
      v.number || v.registrationNumber || "";

    $("vehicleType").value = v.type || "Car";

    $("insuranceExpiry").value =
      v.insurance || v.insuranceExpiry || "";

    $("pucExpiry").value =
      v.puc || v.pucExpiry || "";

    $("fitnessExpiry").value =
      v.fitness || v.fitnessExpiry || "";

    $("nextServiceDate").value =
      v.service || v.nextServiceDate || "";

    $("nextServiceKm").value =
      v.serviceKm ?? v.nextServiceKm ?? "";
  }

  $("vehicleModal").classList.add("open");

  $("vehicleModal").setAttribute(
    "aria-hidden",
    "false"
  );

  $("vehicleName").focus();
}

function closeVehicleForm() {
  $("vehicleModal").classList.remove("open");

  $("vehicleModal").setAttribute(
    "aria-hidden",
    "true"
  );
}

$("addVehicleBtn").addEventListener(
  "click",
  () => openVehicleForm()
);

$("closeModal").addEventListener(
  "click",
  closeVehicleForm
);

$("cancelModal").addEventListener(
  "click",
  closeVehicleForm
);

$("vehicleModal").addEventListener("click", event => {
  if (event.target === $("vehicleModal")) {
    closeVehicleForm();
  }
});

document.addEventListener("keydown", event => {
  if (event.key === "Escape") {
    closeVehicleForm();
  }
});


// SAVE OR UPDATE VEHICLE
$("vehicleForm").addEventListener("submit", async event => {
  event.preventDefault();

  const id = $("vehicleId").value;
  const message = $("vehicleFormMessage");
  const km = $("nextServiceKm").value;

  const body = {
    name: $("vehicleName").value.trim(),

    registrationNumber:
      $("registrationNumber").value.trim().toUpperCase(),

    type: $("vehicleType").value,

    insuranceExpiry:
      $("insuranceExpiry").value || null,

    pucExpiry:
      $("pucExpiry").value || null,

    fitnessExpiry:
      $("fitnessExpiry").value || null,

    nextServiceDate:
      $("nextServiceDate").value || null,

    nextServiceKm:
      km === "" ? null : Number(km)
  };

  if (!body.name || !body.registrationNumber) {
    message.textContent =
      "Vehicle name and registration number are required.";

    return;
  }

  try {
    await api(
      id ? `/api/vehicles/${id}` : "/api/vehicles",
      {
        method: id ? "PUT" : "POST",
        body: JSON.stringify(body)
      }
    );

    closeVehicleForm();

    await loadDashboard();

    notify(id ? "Vehicle updated." : "Vehicle added.");

  } catch (err) {
    console.error("Save vehicle error:", err);

    message.textContent = err.message;
  }
});


// DATE STATUS
function dateStatus(value) {
  if (!value) {
    return {
      text: "Not added",
      cls: "none"
    };
  }

  const d = new Date(`${value}T00:00:00`);

  if (Number.isNaN(d.getTime())) {
    return {
      text: "Invalid date",
      cls: "none"
    };
  }

  const now = new Date();

  const start = new Date(
    now.getFullYear(),
    now.getMonth(),
    now.getDate()
  );

  const days = Math.round(
    (d - start) / 86400000
  );

  if (days < 0) {
    return {
      text: "Expired",
      cls: "expired"
    };
  }

  if (days === 0) {
    return {
      text: "Due today",
      cls: "soon"
    };
  }

  if (days <= 5) {
    return {
      text: `${days} day(s) left`,
      cls: "soon"
    };
  }

  return {
    text: "Valid",
    cls: "valid"
  };
}

function badge(value) {
  const status = dateStatus(value);

  return `
    <span class="status ${status.cls}">
      ${escapeHTML(status.text)}
    </span>
  `;
}


// ESCAPE HTML
function escapeHTML(value) {
  return String(value ?? "").replace(
    /[&<>"']/g,
    ch => ({
      "&": "&amp;",
      "<": "&lt;",
      ">": "&gt;",
      '"': "&quot;",
      "'": "&#39;"
    })[ch]
  );
}


// FORMAT DATE
function formatDate(value) {
  if (!value) return "Not added";

  const d = new Date(`${value}T00:00:00`);

  if (Number.isNaN(d.getTime())) {
    return "Invalid date";
  }

  return d.toLocaleDateString("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric"
  });
}


// LOCAL REMINDERS
function allLocalReminders() {
  return vehicles.flatMap(v => [
    {
      label: "Insurance",
      date: v.insurance,
      kind: "DOCUMENT"
    },
    {
      label: "PUC",
      date: v.puc,
      kind: "DOCUMENT"
    },
    {
      label: "Fitness / Permit",
      date: v.fitness,
      kind: "DOCUMENT"
    },
    {
      label: "Next Service",
      date: v.service,
      kind: "SERVICE"
    }
  ]
    .filter(item => item.date)
    .map(item => {
      const date = new Date(`${item.date}T00:00:00`);

      const now = new Date();

      const start = new Date(
        now.getFullYear(),
        now.getMonth(),
        now.getDate()
      );

      return {
        ...item,
        vehicle: v,
        daysLeft: Math.round(
          (date - start) / 86400000
        )
      };
    })
    .filter(item => item.daysLeft <= 5)
  );
}


// NORMALIZE VEHICLE DATA
function normalizeVehicle(v) {
  return {
    ...v,

    id: v.id,

    name: v.name || "",

    number:
      v.number ||
      v.registrationNumber ||
      "",

    type: v.type || "Car",

    insurance:
      v.insurance ||
      v.insuranceExpiry ||
      null,

    puc:
      v.puc ||
      v.pucExpiry ||
      null,

    fitness:
      v.fitness ||
      v.fitnessExpiry ||
      null,

    service:
      v.service ||
      v.nextServiceDate ||
      null,

    serviceKm:
      v.serviceKm ??
      v.nextServiceKm ??
      null
  };
}


// RENDER VEHICLES
function renderVehicles() {
  const query = $("searchInput").value
    .trim()
    .toLowerCase();

  const filtered = vehicles.filter(v =>
    String(v.name || "").toLowerCase().includes(query) ||
    String(v.number || "").toLowerCase().includes(query)
  );

  if (!filtered.length) {
    $("vehicleList").innerHTML = `
      <div class="empty">
        ${
          vehicles.length
            ? "No matching vehicles found."
            : "🚘 No vehicles added yet. Select Add Vehicle to begin."
        }
      </div>
    `;

    return;
  }

  $("vehicleList").innerHTML = filtered.map(v => `
    <article class="vehicle-card">

      <div class="vehicle-head">
        <div>
          <h3>${escapeHTML(v.name)}</h3>

          <p class="plate">
            ${escapeHTML(v.number)} ·
            ${escapeHTML(v.type)}
          </p>
        </div>

        <div class="car-icon">🚗</div>
      </div>

      <div class="details">

        <div class="detail">
          <span>Insurance</span>
          ${badge(v.insurance)}
        </div>

        <div class="detail">
          <span>PUC</span>
          ${badge(v.puc)}
        </div>

        <div class="detail">
          <span>Fitness / Permit</span>
          ${badge(v.fitness)}
        </div>

        <div class="detail">
          <span>Next service</span>
          ${badge(v.service)}
        </div>

        <div class="detail">
          <span>Service target</span>

          <strong>
            ${
              v.serviceKm == null
                ? "Not added"
                : escapeHTML(v.serviceKm) + " km"
            }
          </strong>
        </div>

      </div>

      <div class="actions">
        <button data-edit="${escapeHTML(v.id)}">
          ✏️ Edit
        </button>

        <button
          class="delete"
          data-delete="${escapeHTML(v.id)}"
        >
          🗑 Delete
        </button>
      </div>

    </article>
  `).join("");

  $("vehicleList")
    .querySelectorAll("[data-edit]")
    .forEach(btn => {
      btn.addEventListener("click", () => {
        const vehicle = vehicles.find(
          v => String(v.id) === btn.dataset.edit
        );

        if (vehicle) openVehicleForm(vehicle);
      });
    });

  $("vehicleList")
    .querySelectorAll("[data-delete]")
    .forEach(btn => {
      btn.addEventListener("click", () => {
        deleteVehicle(btn.dataset.delete);
      });
    });
}


// DELETE VEHICLE
async function deleteVehicle(id) {
  const v = vehicles.find(
    vehicle => String(vehicle.id) === String(id)
  );

  if (!v) return;

  if (!confirm(`Delete ${v.name} (${v.number})?`)) {
    return;
  }

  try {
    await api(`/api/vehicles/${id}`, {
      method: "DELETE"
    });

    await loadDashboard();

    notify("Vehicle deleted.");

  } catch (err) {
    console.error("Delete vehicle error:", err);

    notify(err.message, true);
  }
}


// RENDER REMINDERS
function renderReminders(items) {
  if (!items || !items.length) {
    $("reminderList").innerHTML = `
      <div class="empty">
        ✅ No expired or upcoming reminders.
        <br>
        Add document dates to start tracking deadlines.
      </div>
    `;

    return;
  }

  $("reminderList").innerHTML = items.map(r => {
    const msg = r.daysLeft < 0
      ? `Expired ${Math.abs(r.daysLeft)} day(s) ago`
      : r.daysLeft === 0
        ? "Due today"
        : `Due in ${r.daysLeft} day(s)`;

    const vehicleName =
      r.vehicle?.name || "Vehicle";

    const vehicleNumber =
      r.vehicle?.number ||
      r.vehicle?.registrationNumber ||
      "";

    return `
      <div class="reminder-row">

        <div>
          <div class="reminder-title">
            ${escapeHTML(r.label)} ·
            ${escapeHTML(vehicleName)}
          </div>

          <div class="reminder-sub">
            ${escapeHTML(vehicleNumber)} ·
            ${formatDate(r.date)}
          </div>
        </div>

        <span class="status ${
          r.daysLeft < 0 ? "expired" : "soon"
        }">
          ${escapeHTML(msg)}
        </span>

      </div>
    `;
  }).join("");
}


// LOAD DASHBOARD DATA
async function loadDashboard() {
  const [vehicleData, reminderData] = await Promise.all([
    api("/api/vehicles"),
    api("/api/vehicles/reminders")
  ]);

  // Accept array responses and common wrapped responses.
  const vehicleList = Array.isArray(vehicleData)
    ? vehicleData
    : vehicleData?.vehicles || vehicleData?.content || [];

  vehicles = vehicleList.map(normalizeVehicle);

  $("totalCount").textContent = vehicles.length;

  const reminders = reminderData || {};

  $("dueCount").textContent =
    reminders.dueSoon ?? 0;

  $("expiredCount").textContent =
    reminders.expiredDocuments ?? 0;

  $("maintenanceCount").textContent =
    reminders.maintenanceDue ?? 0;

  renderVehicles();

  renderReminders(
    Array.isArray(reminders.items)
      ? reminders.items
      : allLocalReminders().map(item => ({
          ...item,
          vehicle: {
            ...item.vehicle,
            number: item.vehicle.number
          }
        }))
  );
}


// SEARCH AND REFRESH
$("searchInput").addEventListener(
  "input",
  renderVehicles
);

$("refreshBtn").addEventListener("click", () => {
  loadDashboard()
    .then(() => notify("Dashboard refreshed."))
    .catch(err => {
      console.error("Refresh error:", err);
      notify(err.message, true);
    });
});


// CHECK SESSION WHEN PAGE OPENS
(async function init() {
  try {
    await showApp();
  } catch (err) {
    console.error("Initialization error:", err);
    showAuth();
  }
})();