import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DepartmentApi } from '../../core/api/department.api';
import { RequestApi } from '../../core/api/request.api';
import {
  DepartmentQueueSummary, RequestResponse, RequestStatus
} from '../../core/models/api.models';
import { statusLabel, statusPillClass } from '../staff/workflow-rules';
import { ListSearchComponent, PagerComponent } from '../../shared/list-controls.component';

@Component({
  selector: 'app-department',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, ListSearchComponent, PagerComponent],
  template: `
    <header class="page-head">
      <div>
        <h1>Department</h1>
        <p class="muted">
          @if (departmentName()) {
            {{ departmentName() }} — review new requests and assign paid work to a member of this department.
          } @else {
            Requests in your department.
          }
        </p>
      </div>
    </header>

    @if (loadError()) { <div class="alert alert-error">{{ loadError() }}</div> }

    @if (summary(); as totals) {
      <section class="stats">
        <button type="button" class="stat card" (click)="filterStatus('SUBMITTED')">
          <div class="stat-num">{{ totals.submitted }}</div>
          <div class="stat-label">Awaiting review</div>
        </button>
        <button type="button" class="stat card" (click)="filterStatus('UNDER_REVIEW')">
          <div class="stat-num">{{ totals.underReview }}</div>
          <div class="stat-label">In review</div>
        </button>
        <button type="button" class="stat card" (click)="filterStatus('PAID')">
          <div class="stat-num">{{ totals.awaitingAssignment }}</div>
          <div class="stat-label">Awaiting assignment</div>
        </button>
        <button type="button" class="stat card" (click)="filterStatus('ASSIGNED')">
          <div class="stat-num">{{ totals.assigned }}</div>
          <div class="stat-label">Assigned</div>
        </button>
        <button type="button" class="stat card" (click)="filterStatus('IN_EXECUTION')">
          <div class="stat-num">{{ totals.inProgress }}</div>
          <div class="stat-label">In progress</div>
        </button>
        <button type="button" class="stat card" (click)="filterOverdue()">
          <div class="stat-num">{{ totals.overdue }}</div>
          <div class="stat-label">Overdue</div>
        </button>
        <button type="button" class="stat card" (click)="filterStatus('REPORT_SUBMITTED')">
          <div class="stat-num">{{ totals.awaitingClose }}</div>
          <div class="stat-label">Awaiting close</div>
        </button>
        <button type="button" class="stat card" (click)="filterStatus('REJECTED')">
          <div class="stat-num">{{ totals.rejected }}</div>
          <div class="stat-label">Rejected</div>
        </button>
      </section>
    }

    <section class="card filters">
      <label>Status
        <select [(ngModel)]="status" (ngModelChange)="onStatusChange()">
          <option value="">All</option>
          @for (s of statuses; track s) {
            <option [value]="s">{{ label(s) }}</option>
          }
        </select>
      </label>
      <label>Service line
        <select [(ngModel)]="type" (ngModelChange)="onFormChange()">
          <option value="">All</option>
          <option value="INDUSTRIAL">Industrial</option>
          <option value="ENGINEERING">Engineering</option>
          <option value="ICT">ICT</option>
          <option value="ENVIRONMENTAL">Environmental</option>
        </select>
      </label>
      <label>From <input type="date" [(ngModel)]="from" (ngModelChange)="onFormChange()" /></label>
      <label>To <input type="date" [(ngModel)]="to" (ngModelChange)="onFormChange()" /></label>
      <app-list-search class="grow" placeholder="Name, title, or CIAP reference" (queryChange)="onQuery($event)" />
    </section>

    <section class="card panel">
      @if (loading()) {
        <p class="muted">Loading the department queue…</p>
      } @else if (rows().length === 0) {
        <p class="muted">No requests match this filter.</p>
      } @else {
        <table>
          <thead>
            <tr>
              <th>Reference</th>
              <th>Title</th>
              <th>Client</th>
              <th>Service</th>
              <th>Submitted</th>
              <th>Assignee</th>
              <th>Deadline</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (row of rows(); track row.id) {
              <tr>
                <td class="ref">{{ row.referenceNo }}</td>
                <td>{{ row.title }}</td>
                <td>{{ row.clientName }}</td>
                <td>{{ typeLabel(row.consultancyType) }}</td>
                <td>{{ row.createdAt ? (row.createdAt | date:'mediumDate') : '—' }}</td>
                <td>{{ row.assignedConsultantName || '—' }}</td>
                <td [class.late]="isOverdue(row)">{{ row.assignmentDeadline || '—' }}</td>
                <td><span class="pill" [class]="pill(row.status)">{{ label(row.status) }}</span></td>
                <td><a class="btn btn-ghost sm" [routerLink]="['/staff/requests', row.id]">Open</a></td>
              </tr>
            }
          </tbody>
        </table>
        <app-pager [page]="page()" [totalPages]="totalPages()" [total]="total()" (pageChange)="go($event)" />
      }
    </section>
  `,
  styles: [`
    .page-head { margin-bottom: 1.4rem; }
    .page-head h1 { margin-bottom: 0.2rem; }
    .stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 0.8rem; margin-bottom: 1rem; }
    .stat { text-align: left; cursor: pointer; border: 0; width: 100%; }
    .stat-num { font-size: 1.5rem; font-weight: 700; }
    .stat-label { color: var(--ink-faint); font-size: 0.82rem; }
    .filters { display: flex; flex-wrap: wrap; gap: 0.8rem; align-items: end; margin-bottom: 1rem; padding: 1rem; }
    .filters label { display: flex; flex-direction: column; gap: 0.25rem; font-size: 0.78rem; color: var(--ink-faint); }
    .grow { flex: 1; min-width: 12rem; }
    .filters input, .filters select { padding: 0.45rem 0.55rem; border: 1px solid var(--line); border-radius: var(--radius-sm); }
    table { width: 100%; border-collapse: collapse; }
    th, td { text-align: left; padding: 0.7rem 0.6rem; border-bottom: 1px solid var(--line); font-size: 0.88rem; vertical-align: top; }
    th { font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.03em; color: var(--ink-faint); }
    .ref { font-weight: 600; white-space: nowrap; }
    .late { color: var(--danger, #9b2c2c); font-weight: 600; }
    .sm { padding: 0.35rem 0.65rem; font-size: 0.8rem; }
    @media (max-width: 900px) {
      .stats { grid-template-columns: 1fr 1fr; }
      th:nth-child(5), td:nth-child(5), th:nth-child(7), td:nth-child(7) { display: none; }
    }
  `]
})
export class DepartmentComponent implements OnInit {
  private departments = inject(DepartmentApi);
  private requests = inject(RequestApi);

  protected readonly statuses: RequestStatus[] = [
    'SUBMITTED', 'UNDER_REVIEW', 'QUOTED', 'AWAITING_PAYMENT', 'PAID',
    'ASSIGNED', 'IN_EXECUTION', 'REPORT_SUBMITTED', 'REJECTED', 'CANCELLED', 'CLOSED'
  ];

  protected summary = signal<DepartmentQueueSummary | null>(null);
  protected rows = signal<RequestResponse[]>([]);
  protected departmentName = signal('');
  protected loading = signal(true);
  protected loadError = signal<string | null>(null);
  protected status = '';
  protected type = '';
  protected from = '';
  protected to = '';
  protected query = '';
  protected overdueOnly = false;
  protected page = signal(0);
  protected totalPages = signal(1);
  protected total = signal(0);

  private readonly size = 10;
  private ticket = 0;
  private applyingCard = false;

  ngOnInit(): void {
    this.departments.list().subscribe({
      next: (res) => this.departmentName.set(res.data[0]?.name ?? ''),
      error: () => this.departmentName.set('')
    });
    this.departments.summary().subscribe({
      next: (res) => this.summary.set(res.data),
      error: () => this.loadError.set('Could not load the department summary.')
    });
    this.reload();
  }

  filterStatus(status: RequestStatus): void {
    this.applyingCard = true;
    this.overdueOnly = false;
    this.status = status;
    this.applyingCard = false;
    this.reload();
  }

  filterOverdue(): void {
    this.applyingCard = true;
    this.overdueOnly = true;
    this.status = '';
    this.applyingCard = false;
    this.reload();
  }

  onStatusChange(): void {
    if (this.applyingCard) return;
    this.overdueOnly = false;
    this.reload();
  }

  onFormChange(): void {
    if (this.applyingCard) return;
    this.reload();
  }

  onQuery(q: string): void {
    this.query = q;
    this.reload();
  }

  go(p: number): void { this.load(p); }

  reload(): void { this.load(0); }

  private load(p: number): void {
    const ticket = ++this.ticket;
    if (this.rows().length === 0) this.loading.set(true);
    this.loadError.set(null);
    this.requests.list(p, this.size, {
      status: this.overdueOnly ? undefined : (this.status || undefined),
      q: this.query,
      type: this.type || undefined,
      from: this.from || undefined,
      to: this.to || undefined,
      overdue: this.overdueOnly
    }).subscribe({
      next: (res) => {
        if (ticket !== this.ticket) return;
        this.rows.set(res.data.content);
        this.page.set(res.data.page);
        this.totalPages.set(Math.max(1, res.data.totalPages));
        this.total.set(res.data.totalElements);
        this.loading.set(false);
      },
      error: () => {
        if (ticket !== this.ticket) return;
        this.loadError.set('Could not load departmental requests.');
        this.loading.set(false);
      }
    });
  }

  isOverdue(row: RequestResponse): boolean {
    if (!row.assignmentDeadline) return false;
    if (row.status !== 'ASSIGNED' && row.status !== 'IN_EXECUTION') return false;
    return row.assignmentDeadline < new Date().toISOString().slice(0, 10);
  }

  label(s: RequestStatus): string { return statusLabel(s); }
  pill(s: RequestStatus): string { return statusPillClass(s); }
  typeLabel(t: string): string { return t.charAt(0) + t.slice(1).toLowerCase(); }
}
