# Render deploy checklist

## What this repo now supports
- `render.yaml` defines 5 backend services plus a private MySQL service.
- `eForm/Dockerfile` lets Render build the legacy `eForm` app through Docker like the other services.
- CORS is set to allow the Firebase Hosting origin `https://datawarehouse-subject.web.app`.

## Before you sync Blueprint
1. Push this repo to GitHub.
2. Create or connect a Render workspace.
3. Sync the repository as a Blueprint using `render.yaml`.
4. Replace the secret values in Render Dashboard:
   - `SPRING_DATASOURCE_PASSWORD` for every service
   - `MYSQL_PASSWORD`
   - `MYSQL_ROOT_PASSWORD`
   - `GEMINI_API_KEY` for `eai`

## Deploy order
1. Sync the Blueprint.
2. Wait for `mysql` to start first.
3. Verify each backend service starts and passes `/management/health`.
4. Update the frontend API base URLs if it currently points to local Docker ports.

## Notes
- The database service uses the internal hostname `mysql`, so the JDBC URLs in `render.yaml` match the current Docker setup.
- If Render rejects the private MySQL service in your workspace, the fallback is to create MySQL manually from the Render dashboard and keep the same JDBC URLs.
