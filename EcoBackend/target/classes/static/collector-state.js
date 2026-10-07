// collector-state.js
// Collector portal state, loaded from the backend (/api/collector/requests).
// Pages call `await refreshAssignments()` once, then use the synchronous getters below.

const COLLECTOR_API = "/api/collector";

// Collector points earned per item once a pickup is delivered
const POINTS_PER_ITEM = 30;

let _assignments = [];

function _collectorEmail() {
  const user = JSON.parse(localStorage.getItem("ecorepair_user") || "null");
  return user && user.email;
}

// Backend enum -> status vocabulary used by the collector pages
function _uiStatus(s) {
  switch (s) {
    case "ASSIGNED": return "accepted";
    case "PICKED_UP": return "in_transit";   // pickup started, heading to the customer
    case "COLLECTED": return "picked_up";    // OTP verified, items collected
    case "COMPLETED": return "delivered";
    case "CANCELLED": return "cancelled";
    default: return "pending";
  }
}

function _fromServer(r) {
  return {
    id: r.requestCode,          // e.g. ASN-001, shown in the UI
    dbId: r.id,
    status: _uiStatus(r.status),
    mine: r.mine,
    customer: r.customerName,
    customerEmail: r.customerEmail,
    phone: r.customerPhone,
    location: r.address,
    items: r.itemsSummary,
    points: (r.itemCount || 0) * POINTS_PER_ITEM,
    date: r.date,
    time: `${r.date} · ${r.time}`,
    center: ""
  };
}

// Fetch the latest assignments from the server
async function refreshAssignments() {
  const email = _collectorEmail();
  if (!email) return _assignments;
  try {
    const res = await fetch(`${COLLECTOR_API}/requests?email=${encodeURIComponent(email)}`);
    if (res.ok) _assignments = (await res.json()).map(_fromServer);
  } catch (err) {
    console.error("Could not load assignments:", err);
  }
  return _assignments;
}

// Get all assignments (from the last refresh)
function getAssignments() {
  return _assignments;
}

// Mark an assignment delivered on the server, then refresh.
// (Collecting items goes through verifyPickupOtp, which needs the customer's OTP.)
async function updateAssignmentStatus(id, newStatus) {
  const asn = _assignments.find(a => a.id === id);
  const action = { delivered: "complete" }[newStatus];
  if (!asn || !action) return false;

  try {
    const res = await fetch(
      `${COLLECTOR_API}/requests/${asn.dbId}/${action}?email=${encodeURIComponent(_collectorEmail())}`,
      { method: "PUT" }
    );
    if (!res.ok) {
      const body = await res.json().catch(() => ({}));
      alert(body.message || "Could not update this pickup.");
    }
    await refreshAssignments();
    return res.ok;
  } catch (err) {
    console.error(err);
    alert("Could not connect to the server.");
    return false;
  }
}

// Ask the server to send a fresh OTP to the customer for this pickup
async function requestPickupOtp(id) {
  const asn = _assignments.find(a => a.id === id);
  if (!asn) return { ok: false, message: "Pickup not found" };
  try {
    const res = await fetch(
      `${COLLECTOR_API}/requests/${asn.dbId}/request-otp?email=${encodeURIComponent(_collectorEmail())}`,
      { method: "PUT" }
    );
    const body = await res.json().catch(() => ({}));
    return { ok: res.ok, message: body.message };
  } catch (err) {
    console.error(err);
    return { ok: false, message: "Could not connect to the server." };
  }
}

// Check the OTP the customer read out; on success the pickup moves to "picked up"
async function verifyPickupOtp(id, otp) {
  const asn = _assignments.find(a => a.id === id);
  if (!asn) return { ok: false, message: "Pickup not found" };
  try {
    const res = await fetch(
      `${COLLECTOR_API}/requests/${asn.dbId}/collect?email=${encodeURIComponent(_collectorEmail())}&otp=${encodeURIComponent(otp)}`,
      { method: "PUT" }
    );
    const body = await res.json().catch(() => ({}));
    if (res.ok) await refreshAssignments();
    return { ok: res.ok, message: body.message };
  } catch (err) {
    console.error(err);
    return { ok: false, message: "Could not connect to the server." };
  }
}

// Get assignments by a specific status
function getAssignmentsByStatus(status) {
  return _assignments.filter(a => a.status === status);
}

// Dashboard numbers
function getStats() {
  const now = new Date();
  const monthPrefix = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
  const mine = _assignments.filter(a => a.mine);
  const open = _assignments.filter(a => !["delivered", "cancelled"].includes(a.status));

  return {
    total: open.length,
    pending: _assignments.filter(a => a.status === "pending").length,
    accepted: _assignments.filter(a => a.status === "accepted").length,
    in_progress: mine.filter(a => ["accepted", "in_transit", "picked_up"].includes(a.status)).length,
    completed: mine.filter(a => a.status === "delivered").length,
    cancelled: _assignments.filter(a => a.status === "cancelled").length,
    this_month: mine.filter(a => a.status !== "cancelled" && (a.date || "").startsWith(monthPrefix)).length
  };
}

// Common function to format badge HTML based on status
function getBadgeHTML(status) {
  const badgeMap = {
    'pending': '<span class="badge badge-pending">pending</span>',
    'accepted': '<span class="badge badge-accepted" style="background: var(--green-light); color: var(--green-dark);">accepted</span>',
    'in_transit': '<span class="badge badge-transit" style="background: var(--blue-light); color: var(--blue);">in transit</span>',
    'picked_up': '<span class="badge badge-pickedup" style="background: var(--green-light); color: var(--green-dark);">picked up</span>',
    'delivered': '<span class="badge badge-completed" style="background: var(--gray-200); color: var(--gray-700);">delivered</span>',
    'cancelled': '<span class="badge badge-cancelled" style="background: #fee2e2; color: #dc2626;">cancelled</span>'
  };
  return badgeMap[status] || '';
}

// Pickup ID shown next to an assignment (ASN-001 -> PKP-001)
function generatePickupId(asnId) {
  return `PKP-${asnId.split('-')[1]}`;
}
