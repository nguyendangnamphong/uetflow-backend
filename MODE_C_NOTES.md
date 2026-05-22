# Full mock mode notes

## Deployed services
- `eaccount`
- `eform`
- `eflow`
- `erequest`
- `eai`

## No database
- `DEMO_MODE=true` makes each service boot a dedicated demo app.
- The demo apps exclude JDBC, Hibernate, and Liquibase autoconfiguration entirely.
- `erequest` stores tickets in memory only, so data resets on restart or redeploy.
- `eaccount` stores demo users in memory only.
- `eai` stores demo extraction tasks in memory only.

## Core demo endpoints
- `eaccount`
  - `GET /api/account`
  - `GET /api/account/profile`
  - `POST /api/account/profile`
  - `PUT /api/account/profile`
  - `POST /api/management/account/create`
  - `POST /api/management/account/search`
  - `GET /api/demo/users`
- `eform`
  - `GET /api/owner/menu-views`
  - `GET /api/owner/form?formId=FORM-001`
  - `POST /api/owner/form`
  - `GET /api/owner/form/change-status?formId=FORM-001`
- `eflow`
  - `POST /api/workflow`
  - `GET /api/workflow/1/summary`
  - `POST /api/workflow/1/status`
  - `GET /api/internal/flow/1/first-action-plan`
  - `GET /api/internal/flow/1/next-node?currentNodeId=101`
- `erequest`
  - `GET /api/request/workflows`
  - `POST /api/request/ticket/init`
  - `GET /api/request/tickets/my-requests`
  - `GET /api/request/tickets/pending-tasks`
  - `GET /api/request/ticket/{ticketId}/detail`
  - `POST /api/request/ticket/{ticketId}/submit`
  - `POST /api/request/ticket/{ticketId}/action`
- `eai`
  - `POST /api/document-maps`
  - `POST /api/document-maps/sync`
  - `POST /api/document-maps/demo-extract`
  - `GET /api/document-maps/{taskId}/status`
  - `GET /api/document-maps/{taskId}/result`
