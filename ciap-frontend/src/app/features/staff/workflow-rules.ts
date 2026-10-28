import { RequestStatus } from '../../core/models/api.models';

export interface WorkflowAction {
  /** API action segment, e.g. 'start-review'. */
  action: string;
  /** Button label. */
  label: string;
  /** Roles permitted to perform it (matching backend @PreAuthorize). */
  roles: string[];
  /** Visual emphasis. */
  kind: 'primary' | 'ghost' | 'danger';
  /** Optional: requires a small form (review needs approve/notes; assign needs consultant). */
  form?: 'review' | 'assign';
}

/**
 * Mirrors the backend state machine + @PreAuthorize guards. For a given current
 * status, returns the transitions that are legal next steps. The component then
 * filters by the signed-in user's roles before rendering buttons.
 */
export function actionsFor(status: RequestStatus): WorkflowAction[] {
  switch (status) {
    case 'SUBMITTED':
      return [
        { action: 'start-review', label: 'Start review', roles: ['HOD', 'ADMIN'], kind: 'primary' },
        { action: 'cancel', label: 'Cancel', roles: ['CLIENT', 'MANAGER', 'ADMIN'], kind: 'danger' }
      ];
    case 'UNDER_REVIEW':
      return [
        { action: 'review', label: 'Complete review', roles: ['HOD', 'ADMIN'], kind: 'primary', form: 'review' },
        { action: 'cancel', label: 'Cancel', roles: ['CLIENT', 'MANAGER', 'ADMIN'], kind: 'danger' }
      ];
    case 'QUOTED':
      return [
        { action: 'await-payment', label: 'Issue control number', roles: ['MANAGER', 'ADMIN', 'FINANCE'], kind: 'primary' },
        { action: 'cancel', label: 'Cancel', roles: ['CLIENT', 'MANAGER', 'ADMIN'], kind: 'danger' }
      ];
    case 'AWAITING_PAYMENT':
      return [
        { action: 'cancel', label: 'Cancel', roles: ['CLIENT', 'MANAGER', 'ADMIN'], kind: 'danger' }
      ];
    case 'PAID':
      return [
        { action: 'assign', label: 'Assign to a staff member', roles: ['HOD', 'ADMIN'], kind: 'primary', form: 'assign' },
        { action: 'cancel', label: 'Cancel', roles: ['MANAGER', 'ADMIN'], kind: 'danger' }
      ];
    case 'ASSIGNED':
      return [
        { action: 'start-execution', label: 'Start execution', roles: ['EXPERT', 'MANAGER', 'ADMIN'], kind: 'primary' },
        { action: 'cancel', label: 'Cancel', roles: ['MANAGER', 'ADMIN'], kind: 'danger' }
      ];
    case 'IN_EXECUTION':
      return [
        { action: 'submit-report', label: 'Submit report', roles: ['EXPERT', 'MANAGER', 'ADMIN'], kind: 'primary' },
        { action: 'cancel', label: 'Cancel', roles: ['MANAGER', 'ADMIN'], kind: 'danger' }
      ];
    case 'REPORT_SUBMITTED':
      return [
        { action: 'close', label: 'Close engagement', roles: ['MANAGER', 'ADMIN'], kind: 'primary' }
      ];
    default:
      return []; // CLOSED, REJECTED, CANCELLED — terminal
  }
}

export function statusLabel(s: RequestStatus): string {
  return s.replace(/_/g, ' ').toLowerCase();
}

export function statusPillClass(s: RequestStatus): string {
  if (s === 'CLOSED') return 'pill-done';
  if (s === 'REJECTED' || s === 'CANCELLED') return 'pill-danger';
  if (s === 'AWAITING_PAYMENT') return 'pill-warn';
  if (s === 'SUBMITTED') return 'pill-neutral';
  return 'pill-active';
}
