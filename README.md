# Electricity monitor

Spring Boot prototype that records electricity usage from a simulated meter, rolls it up per hour, day, month and year, and pops up an alert when usage runs above the average.

## Run

Needs Java 21 and Maven.

    mvn spring-boot:run

Open http://localhost:8080. Data is stored in `./data` (H2), so it survives restarts.
Optional PostgreSQL: `docker compose up -d`, then
`mvn spring-boot:run -Dspring-boot.run.profiles=postgres`.

## Deploy (Neon + Render + Vercel)

The database, the Java backend and the web page each live on their own host. Set them up in this order.

**1. Database (Neon)**
1. Create a project at neon.tech and pick the Singapore region.
2. From the connection details, note the host, database name, user and password.
3. Build the JDBC URL from them (it is not the `postgresql://...` string Neon shows):

        jdbc:postgresql://<host>/<database>?sslmode=require

**2. Backend (Render)**
1. Push this repo to GitHub.
2. In Render choose New > Blueprint and pick the repo. It reads `render.yaml` and builds the `Dockerfile`.
3. When asked, fill in `DATABASE_URL` (the JDBC URL above), `DATABASE_USERNAME` and `DATABASE_PASSWORD`.
4. When the deploy is live, open `https://<your-service>.onrender.com/api/meters`. You should see a JSON list with one meter.

**3. Web page (Vercel)**
1. In `vercel.json`, replace `REPLACE-WITH-YOUR-RENDER-URL` with your Render service name, and commit.
2. In Vercel choose Add New > Project and import the same repo. Leave the build settings alone; `vercel.json` already sets them.
3. Open the Vercel URL. The page calls `/api/...` on its own domain and Vercel forwards those calls to Render, so no CORS setup is needed.

Free-tier things to know:
- Render's free service sleeps after 15 minutes without traffic and takes about a minute to wake. While it sleeps the simulator makes no readings. An open dashboard polls every few seconds, so it keeps the service awake.
- The app has no login. Anyone with the link can use the Simulator, including "Reset all data".

## How it works

- `Meter` holds the initial reading and the alert settings.
- `Reading` is one raw measurement (power in watts over a few seconds). Energy = watts x seconds / 3,600,000.
- `UsageSummary` holds the total for one hour, day, month or year. Every new reading updates all four.
- `Notification` is an alert shown as a pop-up on the dashboard.
- The auto simulator adds a reading every 10 s (daily power curve, noise, random spikes).
  The "Add a reading" form adds one with numbers you choose. "Generate history" fills past days.
- Raw readings older than 7 days are deleted nightly; the totals stay.

## Alert rules (set point = "alert above x times the average", default 1.5)

- Power spike: this reading's power vs. the average of the previous 30 readings (5 minute cooldown).
- High hour: this hour's total vs. the average of the same hour of day on past days (needs 3 days of history).
- Daily budget: today's total passed the daily budget (if set).

## API

    GET    /api/meters                      POST /api/meters
    PUT    /api/meters/{id}                 GET  /api/meters/{id}/overview
    GET    /api/usage?meterId=1&type=HOUR   (HOUR | DAY | MONTH | YEAR, optional count)
    GET    /api/readings?meterId=1          POST /api/readings   (for a real device later)
    GET    /api/notifications               PATCH /api/notifications/{id}/read
    POST   /api/notifications/read-all
    GET    /api/simulator                   POST /api/simulator/auto?enabled=true
    POST   /api/simulator/manual            POST /api/simulator/history
    DELETE /api/simulator/data

Reading body: `{"meterId":1,"powerWatts":1500,"durationSeconds":600,"recordedAt":"2026-10-02T14:30:00"}`
(`durationSeconds` defaults to 10, `recordedAt` to now).

## Settings (`application.properties`)

    app.zone=Asia/Bangkok
    simulator.enabled=true
    simulator.interval-ms=10000
    retention.days=7
