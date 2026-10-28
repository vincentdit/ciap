# Implementation phase

This pass adds audit and workflow history on top of the existing CIAP statuses. It does not rename a status, call a payment provider, or add a mail or MFA service.

## In this release

- Quotations carry `versionNo`. Sending a quotation expires other `SENT` quotations for that request. A request cannot have a second `ACCEPTED` quotation.
- Payment confirmation stores the signed-in account and an optional reason. A repeated confirmation of a `PAID` control number returns the same payment.
- Each request status change is stored in `request_status_events`, including the actor and an optional reason.
- An in-app notice is written after the business transaction commits. A failed notice does not undo the status change. Signed-in users can read their notices at `/notices`.
- Clients may record an optional country.
- Consultancy requests use a lock version. A conflicting update returns HTTP 409.
- PDF, PNG, and JPEG uploads must match their file signature.
- API responses include `X-Request-Id`. Nginx forwards its request id on `/api/`.
- English UI copy for notices and the session-expiry warning lives in one catalogue.

## Finance desk

Finance and Admin open **Finance** in the sidebar. Managers can issue a control number from a quoted request and can read that request's payments. They cannot open the finance queue or record a settlement.

A request reaches awaiting payment only after a control number is issued. It becomes paid only after Finance records that payment with a transaction reference and a reason. Recording the same control number again does not move the request a second time. Flagging a payment keeps the request awaiting payment. A flagged payment can be replaced with a new control number. The same transaction reference cannot be used on two payments.

This record is a manual finance confirmation. It does not call GePG. The payment and the request status are compared on the finance page; a mismatch is shown and is not rewritten.

## Head of Department

Each consultancy service line routes to one department: Industrial Research, Engineering, ICT, or Environmental. The route uses the service line on the request. A request with no route, or a department with no active head, stays in the administrator exception queue. It is not given to a random person.

The Head of Department role sees only that department. One person heads one department. They review a submitted request and can reject it with a reason. After Finance records payment, that head assigns an active expert who is a member of the department. A manager can still see requests and close them. A manager cannot review or assign unless they are the head. Finance cannot assign.

Reassignment keeps the earlier assignment row, the documents, and the progress. The previous expert no longer sees the request. Assignment notes stay on the assignment history and are not shown to the client.

`V10__departments_and_assignments.sql` adds the department tables, the role, and the assignment history. It does not rename a status and does not drop existing requests.

## Still waiting on TIRDO

- A real GePG client, signature check, and provider credentials.
- An email system for password reset and external notifications.
- An MFA method.
- New request statuses, including draft, clarification, and report acceptance.
- Kiswahili copy, backup RPO/RTO, and service-level targets.

## Rollback

`V9__quotation_versions_and_audit.sql` only adds columns and two tables. `V10__departments_and_assignments.sql` adds department tables and columns on `consultancy_requests`. Rolling V10 back means dropping `request_assignments`, `service_routes`, `department_members`, and `departments`, then dropping `department_id`, `routing_status`, and `assignment_deadline` from `consultancy_requests`, and deleting the HOD role. Do not drop the database volume.
