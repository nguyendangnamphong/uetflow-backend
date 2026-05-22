# Render deploy checklist for full mock mode

## What this repo now supports
- `render.yaml` defines `eaccount`, `eform`, `eflow`, `erequest`, and `eai`.
- All five run with `DEMO_MODE=true` and do not require MySQL.
- `erequest` uses in-memory request storage.
- `eaccount`, `eflow`, `eform`, and `eai` return mock data but keep the core response shape close to the current frontend contracts.

## Before you sync Blueprint
1. Push this repo to GitHub.
2. Create a Render workspace.
3. Create services from the Blueprint.
4. After Render gives you real URLs, update:
   - `APPLICATION_CLIENT_EFORM_URL` in `eflow`
   - `APPLICATION_CLIENT_EFLOW_URL` in `erequest`
5. Redeploy `eflow` and `erequest` after those URL changes.

## Deploy order
1. Sync Blueprint.
2. Wait for `eaccount`, `eform`, and `eai` to become live.
3. Copy the real `eform` URL into `eflow` env `APPLICATION_CLIENT_EFORM_URL`.
4. Wait for `eflow` to become live.
5. Copy the real `eflow` URL into `erequest` env `APPLICATION_CLIENT_EFLOW_URL`.
6. Wait for `erequest` to become live.

## Demo headers
- Request creator: `X-Demo-User: demo.requester@uetflow.local`
- Approver: `X-Demo-User: demo.approver@uetflow.local`
- Account fallback admin: `X-Demo-User: demo.admin@uetflow.local`
