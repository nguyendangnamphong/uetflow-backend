# Mode B notes: `eRequest` + `eFlow` + `MySQL`

## Minimal runtime tables
- `eFlow` core: `flow`, `node`, `relate_node`, `performer`
- `eFlow` optional for branching and mapping: `relate_demand`, `switch_node`, `map_form`, `variable`
- `eRequest` core: `ticket`, `ticket_step`
- `eRequest` optional for richer UI only: `ticket_data_link`, `ticket_relation`, `ticket_attachment`, `ticket_sla`, `ticket_comment`

## Why these are enough
- `select flow` uses `eFlow` workflow listing.
- `create request` uses `ticket` and `ticket_step`, then asks `eFlow` for the first action plan.
- `submit` uses `ticket_step` history plus `eFlow` next-node calculation.
- `approve/reject` updates only `ticket` and the current `ticket_step`.

## Fast MySQL options
- Fastest hosted option: Render private MySQL container from `render.yaml`. It is not truly free, but it is the fewest clicks if the Blueprint is accepted.
- Lowest-friction external fallback: any hosted MySQL that gives raw host, port, db name, username, and password. Point both JDBC URLs at it and keep separate schemas `eflow` and `erequest`.

## No-MySQL fallback
- Without MySQL, `eRequest` and `eFlow` will not persist tickets or workflow metadata because both are JPA services.
- The current repo does not have an H2 or in-memory profile wired for this demo path.
- Practical fallback for deadline: keep MySQL, but reduce the system to just `mysql`, `eflow`, and `erequest`.

## Demo users
- Request creator: `demo.requester@uetflow.local`
- Approver: `demo.approver@uetflow.local`
- Pass them through header `X-Demo-User` when calling `eRequest` directly without auth.
