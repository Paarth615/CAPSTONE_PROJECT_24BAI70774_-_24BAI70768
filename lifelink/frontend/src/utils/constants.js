export const BLOOD_GROUPS = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

export const URGENCIES = [
  { value: 'CRITICAL', label: 'Critical', hint: 'Needed within a day' },
  { value: 'URGENT', label: 'Urgent', hint: 'Needed within a few days' },
  { value: 'NORMAL', label: 'Planned', hint: 'Scheduled or flexible' },
];

// City centres used to auto-fill coordinates, which powers distance-based matching.
export const CITIES = [
  { name: 'Lucknow', lat: 26.8467, lng: 80.9462 },
  { name: 'Kanpur', lat: 26.4499, lng: 80.3319 },
  { name: 'Varanasi', lat: 25.3176, lng: 82.9739 },
  { name: 'Delhi', lat: 28.6139, lng: 77.209 },
  { name: 'Noida', lat: 28.5355, lng: 77.391 },
  { name: 'Gurugram', lat: 28.4595, lng: 77.0266 },
  { name: 'Jaipur', lat: 26.9124, lng: 75.7873 },
  { name: 'Ahmedabad', lat: 23.0225, lng: 72.5714 },
  { name: 'Mumbai', lat: 19.076, lng: 72.8777 },
  { name: 'Pune', lat: 18.5204, lng: 73.8567 },
  { name: 'Bengaluru', lat: 12.9716, lng: 77.5946 },
  { name: 'Hyderabad', lat: 17.385, lng: 78.4867 },
  { name: 'Chennai', lat: 13.0827, lng: 80.2707 },
  { name: 'Kolkata', lat: 22.5726, lng: 88.3639 },
  { name: 'Patna', lat: 25.5941, lng: 85.1376 },
  { name: 'Bhopal', lat: 23.2599, lng: 77.4126 },
  { name: 'Indore', lat: 22.7196, lng: 75.8577 },
  { name: 'Chandigarh', lat: 30.7333, lng: 76.7794 },
];

export function findCity(name) {
  if (!name) return null;
  const n = name.trim().toLowerCase();
  return CITIES.find((c) => c.name.toLowerCase() === n) || null;
}

/** Mirror of the server rule: can a donor of group `donor` give to a patient of group `recipient`? */
export function canDonate(donor, recipient) {
  const abo = (g) => g.slice(0, -1);
  const pos = (g) => g.endsWith('+');
  const d = abo(donor);
  const r = abo(recipient);
  const aboOk = d === 'O' || (d === 'A' && (r === 'A' || r === 'AB')) || (d === 'B' && (r === 'B' || r === 'AB')) || (d === 'AB' && r === 'AB');
  const rhOk = !pos(donor) || pos(recipient);
  return aboOk && rhOk;
}

export const DEMO_ACCOUNTS = [
  { label: 'Donor', email: 'donor1@lifelink.com', password: 'Password@123' },
  { label: 'Requester', email: 'hospital1@lifelink.com', password: 'Password@123' },
  { label: 'Admin', email: 'admin@lifelink.com', password: 'Admin@123' },
];
