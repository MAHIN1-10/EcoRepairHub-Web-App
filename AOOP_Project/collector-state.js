// collector-state.js
// A simple mock state management for the collector portal prototype

const INITIAL_ASSIGNMENTS = [
  {
    id: "ASN-001",
    status: "pending", // pending, accepted, in_transit, picked_up, delivered, cancelled
    customer: "Ahmed Ali",
    phone: "+966 55 123 4567",
    location: "12 King Fahd Rd, Riyadh",
    items: "2x Laptops, 1x Printer",
    time: "2026-03-20 · 10:00 – 12:00",
    center: ""
  },
  {
    id: "ASN-002",
    status: "accepted",
    customer: "Sara Khalid",
    phone: "+966 50 987 6543",
    location: "45 Olaya St, Riyadh",
    items: "5x Mobile Phones",
    time: "2026-03-20 · 14:00 – 16:00",
    center: "GreenTech Recycling — Industrial Area"
  },
  {
    id: "ASN-003",
    status: "in_transit",
    customer: "Mohammed Nasser",
    phone: "+966 54 222 3344",
    location: "78 Tahlia St, Jeddah",
    items: "1x Washing Machine",
    time: "2026-03-19 · 09:00 – 11:00",
    center: "GreenTech Recycling"
  },
  {
    id: "ASN-004",
    status: "delivered",
    customer: "Fatima Omar",
    phone: "+966 56 111 2233",
    location: "33 Prince Sultan Rd, Jeddah",
    items: "3x Batteries, 1x Monitor",
    time: "2026-03-18 · 13:00 – 15:00",
    center: "CleanEarth Hub"
  },
  {
    id: "ASN-005",
    status: "cancelled",
    customer: "Yusuf Ibrahim",
    phone: "+966 59 444 5566",
    location: "19 Tahaliyah St, Riyadh",
    items: "2x Scanners",
    time: "2026-03-17 · 15:00 – 17:00",
    center: ""
  }
];

// Initialize state if not present
function initCollectorState() {
  if (!localStorage.getItem('collector_assignments')) {
    localStorage.setItem('collector_assignments', JSON.stringify(INITIAL_ASSIGNMENTS));
  }
}

// Get all assignments
function getAssignments() {
  initCollectorState();
  return JSON.parse(localStorage.getItem('collector_assignments'));
}

// Update an assignment's status
function updateAssignmentStatus(id, newStatus) {
  let assignments = getAssignments();
  let updated = assignments.map(a => {
    if (a.id === id) {
      a.status = newStatus;
    }
    return a;
  });
  localStorage.setItem('collector_assignments', JSON.stringify(updated));
}

// Get assignments by a specific status
function getAssignmentsByStatus(status) {
  return getAssignments().filter(a => a.status === status);
}

// Count assignments
function getStats() {
  const assignments = getAssignments();
  return {
    total: assignments.length,
    pending: assignments.filter(a => a.status === 'pending').length,
    accepted: assignments.filter(a => a.status === 'accepted').length,
    in_progress: assignments.filter(a => a.status === 'in_transit' || a.status === 'picked_up').length,
    completed: assignments.filter(a => a.status === 'delivered').length,
    cancelled: assignments.filter(a => a.status === 'cancelled').length,
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

// Helper: map assignment ID to an auto-incrementing pickup ID
function generatePickupId(asnId) {
  // Simple deterministic generation for the prototype
  const num = asnId.split('-')[1];
  return `PKP-${num}`;
}

// Ensure state is initialized on script load
initCollectorState();
