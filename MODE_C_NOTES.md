# Mode C mock notes

## Deployed services
- `eform`
- `eflow`
- `erequest`

## No database
- `DEMO_MODE=true` makes each service boot a dedicated demo app.
- The demo apps exclude JDBC, Hibernate, and Liquibase autoconfiguration entirely.
- `erequest` stores tickets in memory only, so data resets on restart or redeploy.

## Core demo endpoints
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
