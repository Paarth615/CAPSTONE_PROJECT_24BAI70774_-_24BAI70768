# CAPSTONE_PROJECT_24BAI70774_-_24BAI70768

# LifeLink: Blood Donation Network with Smart Matching

FSD-II capstone project. **React** front-end + **Spring Boot** back-end, deployable to the web.

LifeLink connects people who need blood with donors who can safely give it. When a request is raised, the
**smart matching engine** filters out donors who are incompatible, unavailable or not yet rested, scores the rest out
of 100, and alerts the best matches immediately.

---

## 1. Features

| Area | What it does |
|---|---|
| Accounts | Register as **Donor** or **Requester** (hospital / family). JWT sign-in, BCrypt-hashed passwords. A seeded **Admin** exists. |
| Blood requests | Raise a request (blood group, units, hospital, city, urgency, needed-by date). Browse, filter by group / city / urgency. Sorted Critical > Urgent > Planned, then by date. |
| Smart matching | ABO/Rh compatibility, distance (haversine), rest since last donation, past reliability. Explains every score with reasons. |
| Donor workflow | Offer to donate, requester accepts, donation confirmed, donor enters a 90-day rest period. Donors can withdraw. |
| Safety rules | Age 18 to 65, weight at least 45 kg, 90-day gap between donations, donor availability switch. |
| Notifications | In-app alerts: new matching request, offer received / accepted / declined, fulfilled, expired. |
| Admin | Platform statistics, enable/disable users, delete requests. |
| Housekeeping | Hourly job expires requests past their needed-by date and tells everyone involved. |
| Public stats | Landing page shows live available donors by blood group. |

## 2. Tech stack

- **Front-end:** React 18, Vite, React Router, Axios (plain CSS, no UI framework)
- **Back-end:** Java 17, Spring Boot 3.4 (Web, Data JPA, Security, Validation, Actuator), JJWT
- **Database:** H2 file database locally (zero setup), PostgreSQL in production
- **Tests:** JUnit 5, Spring Boot Test, MockMvc (full workflow test through the real security chain)
- **Deploy:** Docker (API), static hosting (React), `render.yaml` blueprint included

## 3. Architecture

```
React (Vite)  --HTTPS + JSON + Bearer JWT-->  Spring Boot REST API  --JPA-->  PostgreSQL / H2
 pages, context, axios                         Controller -> Service -> Repository
```

Back-end layers (`backend/src/main/java/com/lifelink`):

```
controller/   REST endpoints only, no logic
service/      business rules (matching, eligibility, request + response workflow, stats)
repository/   Spring Data JPA interfaces
model/        JPA entities + enums (User, BloodRequest, DonationResponse, Notification, BloodGroup...)
dto/          request/response records, so entities are never exposed directly
security/     JwtService, JwtAuthFilter, CurrentUser, CorrelationIdFilter (MDC logging)
exception/    ApiException + GlobalExceptionHandler -> one consistent JSON error shape
config/       SecurityConfig (roles, CORS), ProdDataSourceConfig, DataSeeder
```

### Database tables

`users` 1-* `blood_requests` (requester) 1-* `donation_responses` *-1 `users` (donor), plus `notifications` (per user).
`donation_responses` has a unique key on (request, donor), so a donor cannot respond twice.

## 4. Smart matching algorithm

Hard filters (anyone failing is never shown):
active account, marked available, ABO/Rh-compatible with the patient, eligible (age, weight, 90-day rest), not the requester.

Score for the rest (out of 100):

| Factor | Points | Rule |
|---|---|---|
| Blood group | 40 / 25 | Exact match = 40, compatible only = 25 (O- counts as universal donor) |
| Proximity | up to 30 | By distance when both sides have coordinates: 5 km = 30, 15 km = 26, 50 km = 20, 150 km = 10, beyond = 3. Otherwise same city = 24, else 0 |
| Rest | 10 to 20 | 10 once the cool-down ends, rising to 20 after another 90 days. Never donated = 16 |
| Reliability | up to 10 | 3 points for each donation completed through LifeLink |

Each match returns `reasons[]` so the requester sees *why* a donor ranked where they did.
Code: `MatchingService.java`, compatibility rule: `BloodGroup.canDonateTo`.

## 5. Run it locally in VS Code

**Prerequisites:** JDK 17+, Maven 3.9+, Node 18+ (check with `java -version`, `mvn -v`, `node -v`).
Open the `lifelink` folder in VS Code and accept the recommended extensions.

**Terminal 1: back-end** (http://localhost:8080)

```bash
cd backend
mvn spring-boot:run
```

**Terminal 2: front-end** (http://localhost:5173)

```bash
cd frontend
cp .env.example .env      # Windows: copy .env.example .env
npm install
npm run dev
```

Or use *Terminal > Run Task > Run everything* (see `.vscode/tasks.json`).

The first start creates a local H2 database in `backend/data/` and loads demo data.

### Demo accounts

| Role | Email | Password |
|---|---|---|
| Admin | `admin@lifelink.com` | `Admin@123` |
| Donor | `donor1@lifelink.com` ... `donor28@lifelink.com` | `Password@123` |
| Requester | `hospital1@lifelink.com` ... `hospital3@lifelink.com` | `Password@123` |

The sign-in page also has one-click demo buttons. Set `SEED_DEMO_DATA=false` to skip demo data.

### Run the tests

```bash
cd backend
mvn test
```

`BloodGroupTest` checks the compatibility matrix. `LifeLinkFlowTest` runs the whole journey: register, raise request,
smart match, respond, accept, complete, cool-down, plus the security and validation guard rails.

## 6. Deployment (required by the university brief)

### Option A: Render (one blueprint, easiest)

1. Push this folder to a GitHub repo (the `render.yaml` must be at the repo root).
2. Render dashboard > **New > Blueprint** > select the repo > Apply.
   It creates the PostgreSQL database, the API (Docker) and the React static site.
3. If Render says a service name is taken, rename `lifelink-api` / `lifelink-web` in `render.yaml`
   and update `CORS_ALLOWED_ORIGINS` and `VITE_API_URL` to the real URLs, then redeploy.
4. Open the web URL. The free API sleeps when idle, so the first load can take up to a minute (the landing page says so).

### Option B: split hosting

| Part | Host | Settings |
|---|---|---|
| Database | Neon / Supabase / Render Postgres | Copy the connection string |
| API | Render / Railway (Docker, root `backend/`) | env: `SPRING_PROFILES_ACTIVE=prod`, `DB_URL=jdbc:postgresql://HOST/DB?sslmode=require`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (32+ chars), `CORS_ALLOWED_ORIGINS=https://your-site` |
| React | Vercel / Netlify | root `frontend/`, build `npm run build`, output `dist`, env `VITE_API_URL=https://your-api` (rewrites already in `vercel.json` and `public/_redirects`) |

A `DATABASE_URL=postgres://user:pass@host/db` value (Render / Railway style) also works instead of `DB_URL`.

### Environment variables

| Variable | Where | Meaning |
|---|---|---|
| `DATABASE_URL` or `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` | API | PostgreSQL connection |
| `JWT_SECRET` | API | Signing key, 32+ characters |
| `CORS_ALLOWED_ORIGINS` | API | Comma-separated front-end URLs allowed to call the API |
| `SEED_DEMO_DATA` | API | `true` loads demo data into an empty database |
| `VITE_API_URL` | Front-end (build time) | API base URL, no trailing `/api` |

After deploying, check `https://YOUR-API/actuator/health` returns `{"status":"UP"}` and sign in with a demo account.

## 7. REST API

All routes except `/api/auth/*`, `/api/public/*` and `/actuator/health` need `Authorization: Bearer <token>`.

| Method | Path | Who | Purpose |
|---|---|---|---|
| POST | `/api/auth/register` | public | Create donor / requester account |
| POST | `/api/auth/login` | public | Sign in, returns JWT + profile |
| GET | `/api/public/stats` | public | Live counts + available donors by group |
| GET / PUT | `/api/users/me` | any | View / update profile |
| PUT | `/api/users/me/availability` | donor | Pause or resume alerts |
| POST | `/api/requests` | donor, requester | Raise a request (auto-alerts top matches) |
| GET | `/api/requests?blood=&city=&urgency=&forMe=` | any | Open requests, filtered |
| GET | `/api/requests/mine` | any | Requests I raised |
| GET | `/api/requests/{id}` | any | One request |
| GET | `/api/requests/{id}/matches` | owner, admin | Ranked donors with reasons |
| POST | `/api/requests/{id}/notify-matches` | owner, admin | Alert top matches again |
| PUT | `/api/requests/{id}/status` | owner, admin | `CANCELLED` or `FULFILLED` |
| POST | `/api/requests/{id}/respond` | donor | Offer to donate |
| GET | `/api/requests/{id}/responses` | owner, admin | Offers received |
| GET | `/api/responses/mine` | any | My offers |
| PUT | `/api/responses/{id}/status` | owner / donor | `ACCEPTED`, `DECLINED`, `COMPLETED`, `WITHDRAWN` |
| GET / PUT | `/api/notifications`, `/{id}/read`, `/read-all` | any | In-app alerts |
| GET | `/api/admin/stats`, `/users`, `/requests` | admin | Monitoring |
| PUT / DELETE | `/api/admin/users/{id}/enabled`, `/requests/{id}` | admin | Moderation |

Errors always look like `{ "status": 400, "message": "...", "errors": { "field": "..." }, "correlationId": "..." }`.

## 8. Scenarios the system handles

| Scenario | Behaviour |
|---|---|
| Wrong blood type offers to help | Rejected with a clear message; never matched |
| Donor donated recently | Blocked from responding and from matching until the 90-day rest ends |
| Under 18 or over 65 | Cannot register as a donor |
| Under 45 kg | Can register, but is excluded from matching and cannot respond until the weight on file changes |
| Donor switches availability off | No alerts, not matched |
| Same donor responds twice | 409 Conflict (unique key); a withdrawn offer can be re-made |
| Someone else's request | Matches, offers and status changes are owner/admin only (403) |
| Requester cannot respond to own request | Rejected |
| Multi-unit request | Each confirmed donation adds a unit; when all are received the request becomes FULFILLED and other pending donors are told |
| Request cancelled / expired | Pending offers are closed and donors notified; hourly job handles expiry |
| Required-by date in the past or > 90 days out | Validation error |
| Disabled account | Cannot sign in; existing tokens stop working |
| Expired / invalid token | API returns 401, front-end sends the user to sign in |
| Server asleep on free hosting | Landing page explains the wait; 70 s request timeout |

## 9. Suggested split for a group of 2 (both explain their own part in the viva)

- **Member A, back-end:** entities and relationships, JWT security and roles, `MatchingService`, request/response workflow, exception handling, tests, Docker.
- **Member B, front-end:** routing and protected routes, auth context, forms with validation, request detail (donor vs owner views), dashboard, admin page, deployment.

## 10. Viva cheat-sheet

- **Why JWT and not sessions?** Stateless API that scales horizontally; React sends the token in a header, so no CSRF cookie risk.
- **Why DTO records?** Entities never leak (e.g. password hash); the API shape is independent of the schema.
- **How is compatibility decided?** `BloodGroup.canDonateTo`: ABO rule (O to all, A to A/AB, B to B/AB, AB to AB) plus Rh rule (Rh-negative recipients accept only Rh-negative donors).
- **Why filter first, then score?** Safety rules must never be traded off against convenience; scoring only ranks already-safe donors.
- **How do you stop a donor being matched twice or too soon?** Unique (request, donor) key, and `EligibilityService` computes the cool-down from `lastDonationDate`, which is set automatically when a donation is confirmed.
- **How is the correlation ID used?** `CorrelationIdFilter` puts an ID in the logging MDC for each request and returns it as `X-Correlation-Id` and in error bodies, so one request can be traced through the logs.
- **What would you add next?** SMS / e-mail alerts, blood-bank inventory, map view, rate limiting, refresh tokens.

## 11. Known limitations

- Notifications are in-app (polled every 30 s), not SMS / push.
- Matching is computed on demand over all available donors, which is fine for a college-scale dataset; at large scale use a geo index.
- Render's free database and API tiers sleep or expire, so use a paid or alternative database for long-term hosting.
- This is a coursework project and not a certified medical system. Final eligibility is always decided by a medical professional.
