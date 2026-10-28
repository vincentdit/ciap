import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { RequestApi } from '../../core/api/request.api';
import { RequestResponse, RequestStatus, PageResponse } from '../../core/models/api.models';
import { ListSearchComponent, PagerComponent } from '../../shared/list-controls.component';

@Component({
  selector: 'app-request-list',
  standalone: true,
  imports: [CommonModule, RouterLink, ListSearchComponent, PagerComponent],
  template: `
    <header class="page-head">
      <div>
        <h1>Consultancy requests</h1>
        <p class="muted">All engagements you have submitted.</p>
      </div>
      <a routerLink="/requests/new" class="btn btn-primary">+ New Request</a>
    </header>

    <app-list-search placeholder="Reference, title, department, or assignee" (queryChange)="onQuery($event)" />

    @if (loading()) {
      <div class="card load-card">Loading requests…</div>
    } @else if (error()) {
      <div class="alert alert-error">{{ error() }}</div>
    } @else if (rows().length === 0) {
      <div class="card empty">
        <p>{{ query ? 'No requests match this search.' : 'No consultancy requests found.' }}</p>
        @if (!query) {
          <a routerLink="/requests/new" class="btn btn-primary">Submit your first request</a>
        }
      </div>
    } @else {
      <div class="card table-card">
        <table>
          <thead>
            <tr>
              <th>Reference</th>
              <th>Title</th>
              <th>Department</th>
              <th>Assignee</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            @for (r of rows(); track r.id) {
              <tr>
                <td class="ref">{{ r.referenceNo }}</td>
                <td class="title">{{ r.title }}</td>
                <td>{{ r.departmentName || '—' }}</td>
                <td>{{ r.assignedConsultantName || '—' }}</td>
                <td><span class="pill" [class]="pillClass(r.status)">{{ label(r.status) }}</span></td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      <app-pager [page]="page()" [totalPages]="totalPages()" [total]="total()" (pageChange)="go($event)" />
    }
  `,
  styles: [`
    .page-head { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 2rem; gap: 1rem; }
    .page-head h1 { font-size: 2rem; margin-bottom: 0.2rem; }
    .page-head p { margin: 0; font-size: 0.95rem; }

    .load-card { padding: 2rem; color: var(--ink-faint); }
    .empty { padding: 2.5rem; text-align: center; }
    .empty p { color: var(--ink-faint); margin-bottom: 1rem; }

    .table-card { padding: 0; overflow: hidden; }
    table { width: 100%; border-collapse: collapse; }
    thead th {
      text-align: left; font-size: 0.76rem; font-weight: 700; letter-spacing: 0.04em;
      text-transform: uppercase; color: var(--ink-faint);
      padding: 0.9rem 1.2rem; border-bottom: 1px solid var(--line); background: var(--paper-2);
    }
    tbody td { padding: 0.95rem 1.2rem; border-bottom: 1px solid var(--line); font-size: 0.92rem; }
    tbody tr:last-child td { border-bottom: none; }
    tbody tr:hover { background: var(--teal-50); }
    .ref { font-weight: 600; color: var(--ink-soft); font-size: 0.84rem; white-space: nowrap; }
    .title { font-weight: 500; color: var(--ink); }
    .type { color: var(--ink-soft); }

    @media (max-width: 720px) {
      thead th:nth-child(4), tbody td:nth-child(4) { display: none; }
    }
  `]
})
export class RequestListComponent implements OnInit {
  private api = inject(RequestApi);

  protected loading = signal(true);
  protected error = signal<string | null>(null);
  protected rows = signal<RequestResponse[]>([]);
  protected page = signal(0);
  protected totalPages = signal(1);
  protected total = signal(0);
  protected query = '';

  private readonly size = 10;
  private ticket = 0;

  ngOnInit(): void { this.load(0); }

  onQuery(q: string): void {
    this.query = q;
    this.load(0);
  }

  go(p: number): void { this.load(p); }

  private load(p: number): void {
    const ticket = ++this.ticket;
    if (this.rows().length === 0) this.loading.set(true);
    this.error.set(null);
    this.api.list(p, this.size, { q: this.query }).subscribe({
      next: (res) => {
        if (ticket !== this.ticket) return;
        const page: PageResponse<RequestResponse> = res.data;
        this.rows.set(page.content);
        this.page.set(page.page);
        this.totalPages.set(Math.max(1, page.totalPages));
        this.total.set(page.totalElements);
        this.loading.set(false);
      },
      error: () => {
        if (ticket !== this.ticket) return;
        this.error.set('Could not load requests.');
        this.loading.set(false);
      }
    });
  }

  label(s: RequestStatus): string { return s.replace(/_/g, ' ').toLowerCase(); }

  pillClass(s: RequestStatus): string {
    if (s === 'CLOSED') return 'pill-done';
    if (s === 'REJECTED' || s === 'CANCELLED') return 'pill-danger';
    if (s === 'AWAITING_PAYMENT') return 'pill-warn';
    if (s === 'SUBMITTED') return 'pill-neutral';
    return 'pill-active';
  }
}
