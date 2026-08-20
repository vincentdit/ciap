import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { RequestApi } from '../../core/api/request.api';
import { RequestResponse, RequestStatus, PageResponse } from '../../core/models/api.models';
import { statusLabel, statusPillClass } from './workflow-rules';

@Component({
  selector: 'app-staff-queue',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <header class="page-head">
      <div>
        <h1>Request queue</h1>
        <p class="muted">All consultancy requests across the organisation.</p>
      </div>
    </header>

    <div class="filters">
      <button class="chip" [class.on]="filter() === 'ALL'" (click)="setFilter('ALL')">All</button>
      @for (s of quickFilters; track s) {
        <button class="chip" [class.on]="filter() === s" (click)="setFilter(s)">{{ label(s) }}</button>
      }
    </div>

    @if (loading()) {
      <div class="card load-card">Loading queue…</div>
    } @else if (error()) {
      <div class="alert alert-error">{{ error() }}</div>
    } @else if (visible().length === 0) {
      <div class="card empty"><p>No requests match this filter.</p></div>
    } @else {
      <div class="card table-card">
        <table>
          <thead>
            <tr>
              <th>Reference</th><th>Title</th><th>Client</th><th>Type</th><th>Status</th><th></th>
            </tr>
          </thead>
          <tbody>
            @for (r of visible(); track r.id) {
              <tr>
                <td class="ref">{{ r.referenceNo }}</td>
                <td class="title">{{ r.title }}</td>
                <td>{{ r.clientName }}</td>
                <td class="type">{{ typeLabel(r.consultancyType) }}</td>
                <td><span class="pill" [class]="pill(r.status)">{{ label(r.status) }}</span></td>
                <td class="action-col">
                  <a [routerLink]="['/staff/requests', r.id]" class="btn btn-ghost sm">Open →</a>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      @if (totalPages() > 1) {
        <div class="pager">
          <button class="btn btn-ghost" [disabled]="page() === 0" (click)="go(page() - 1)">← Previous</button>
          <span class="muted">Page {{ page() + 1 }} of {{ totalPages() }}</span>
          <button class="btn btn-ghost" [disabled]="page() + 1 >= totalPages()" (click)="go(page() + 1)">Next →</button>
        </div>
      }
    }
  `,
  styles: [`
    .page-head { margin-bottom: 1.4rem; }
    .page-head h1 { font-size: 2rem; margin-bottom: 0.2rem; }
    .page-head p { margin: 0; font-size: 0.95rem; }

    .filters { display: flex; flex-wrap: wrap; gap: 0.5rem; margin-bottom: 1.4rem; }
    .chip {
      font-family: var(--font-body); font-size: 0.82rem; font-weight: 600;
      padding: 0.4rem 0.85rem; border-radius: 100px; cursor: pointer;
      background: var(--card); color: var(--ink-soft); border: 1px solid var(--line-2);
      transition: all 0.15s ease;
    }
    .chip:hover { border-color: var(--teal-500); }
    .chip.on { background: var(--teal-700); color: #fff; border-color: var(--teal-700); }

    .load-card { padding: 2rem; color: var(--ink-faint); }
    .empty { padding: 2.5rem; text-align: center; }
    .empty p { color: var(--ink-faint); margin: 0; }

    .table-card { padding: 0; overflow: hidden; }
    table { width: 100%; border-collapse: collapse; }
    thead th {
      text-align: left; font-size: 0.76rem; font-weight: 700; letter-spacing: 0.04em;
      text-transform: uppercase; color: var(--ink-faint);
      padding: 0.9rem 1.2rem; border-bottom: 1px solid var(--line); background: var(--paper-2);
    }
    tbody td { padding: 0.9rem 1.2rem; border-bottom: 1px solid var(--line); font-size: 0.92rem; }
    tbody tr:last-child td { border-bottom: none; }
    tbody tr:hover { background: var(--teal-50); }
    .ref { font-weight: 600; color: var(--ink-soft); font-size: 0.84rem; white-space: nowrap; }
    .title { font-weight: 500; }
    .type { color: var(--ink-soft); }
    .action-col { text-align: right; white-space: nowrap; }
    .btn.sm { padding: 0.4rem 0.7rem; font-size: 0.85rem; }

    .pager { display: flex; align-items: center; justify-content: center; gap: 1.2rem; margin-top: 1.4rem; }
    @media (max-width: 760px) { .type, thead th:nth-child(4) { display: none; } }
  `]
})
export class StaffQueueComponent implements OnInit {
  private api = inject(RequestApi);

  protected loading = signal(true);
  protected error = signal<string | null>(null);
  protected all = signal<RequestResponse[]>([]);
  protected filter = signal<RequestStatus | 'ALL'>('ALL');
  protected page = signal(0);
  protected totalPages = signal(1);

  protected readonly quickFilters: RequestStatus[] = [
    'SUBMITTED', 'UNDER_REVIEW', 'QUOTED', 'AWAITING_PAYMENT', 'PAID', 'ASSIGNED', 'IN_EXECUTION', 'REPORT_SUBMITTED'
  ];

  private readonly size = 50;

  ngOnInit(): void { this.load(0); }

  go(p: number): void { this.load(p); }
  setFilter(f: RequestStatus | 'ALL'): void { this.filter.set(f); }

  visible(): RequestResponse[] {
    const f = this.filter();
    return f === 'ALL' ? this.all() : this.all().filter(r => r.status === f);
  }

  private load(p: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.list(p, this.size).subscribe({
      next: (res) => {
        const page: PageResponse<RequestResponse> = res.data;
        this.all.set(page.content);
        this.page.set(page.page);
        this.totalPages.set(Math.max(1, page.totalPages));
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.status === 403
          ? 'Your role does not have access to the request queue.'
          : 'Could not load the queue.');
        this.loading.set(false);
      }
    });
  }

  label(s: RequestStatus): string { return statusLabel(s); }
  pill(s: RequestStatus): string { return statusPillClass(s); }
  typeLabel(t: string): string { return t.charAt(0) + t.slice(1).toLowerCase(); }
}
