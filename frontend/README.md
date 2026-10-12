# CivicPulse Frontend

Single Page Application (SPA) for the Municipal Complaint Prioritization & Smart Dispatch System.

## Architecture & Tech Stack
- **Framework**: Semantic Modern HTML5 & JavaScript (ES6+)
- **Styling**: TailwindCSS via CDN with custom Glassmorphism & dark/light theme
- **Icons**: FontAwesome 6 Pro CDN
- **Typography**: Plus Jakarta Sans & JetBrains Mono

## Features
- **Live Dynamic Priority Queue**: Real-time calculated issue queue with severity, impact, and age metrics.
- **Reporting Interface**: Geo-tagging, municipal zone ID selection, category mapping, and proof photo uploads.
- **Community Upvoting**: Increases affected citizen count atomically and triggers instant score recalculation.
- **Status Lifecycle Pipeline**: Visual workflow modal for field officers and admins (`REPORTED` → `VERIFIED` → `ASSIGNED` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`).
- **Turnaround Analytics**: Department workloads, zone heatmaps, and pipeline resolution rates.
- **One-Click Quick Logins**: Instant switching between Admin (`admin@municipal.gov`) and Citizen User (`citizen@municipal.gov`).

## Running
The frontend connects to the Spring Boot REST API on `http://localhost:8085` (or same-origin when served directly by Spring Boot).
You can open `index.html` directly in any web browser, or serve it with any static server:
```bash
# Optional standalone static server:
npx serve .
# or
python -m http.server 3000
```
