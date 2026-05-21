# Render deploy checklist for Mode C mock

## What this repo now supports
- `render.yaml` defines `eform`, `eflow`, and `erequest` only.
- All three run with `DEMO_MODE=true` and do not require MySQL, `eAccount`, or `eAi`.
- `erequest` uses in-memory request storage.
- `eflow` and `eform` return mock data but keep the core response shape close to the current frontend contracts.

## Before you sync Blueprint
1. Push this repo to GitHub.
2. Create a Render workspace.
3. Create `eform` first, then `eflow`, then `erequest` from the Blueprint.
4. After Render gives you real URLs, update:
   - `APPLICATION_CLIENT_EFORM_URL` in `eflow`
   - `APPLICATION_CLIENT_EFLOW_URL` in `erequest`
5. Redeploy `eflow` and `erequest` after those URL changes.

## Deploy order
1. Sync Blueprint.
2. Wait for `eform` to become live.
3. Copy the real `eform` URL into `eflow` env `APPLICATION_CLIENT_EFORM_URL`.
4. Wait for `eflow` to become live.
5. Copy the real `eflow` URL into `erequest` env `APPLICATION_CLIENT_EFLOW_URL`.
6. Wait for `erequest` to become live.

## Demo headers
- Request creator: `X-Demo-User: demo.requester@uetflow.local`
- Approver: `X-Demo-User: demo.approver@uetflow.local`
