import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { RequestApi } from '../../core/api/request.api';
import { RequestResponse, RequestStatus } from '../../core/models/api.models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <header class="page-head">
      <div>
        <h1>Welcome, {{ firstName() }}</h1>
        <p class="muted">{{ intro() }}</p>
      </div>
      @if (isDepartmentHead()) {
        <div class="head-actions">
          <a routerLink="/department" class="btn btn-ghost">Department</a>
          <a routerLink="/requests/new" class="btn btn-primary">+ New request</a>
        </div>
      } @else if (isStaff()) {
        <a routerLink="/staff/requests" class="btn btn-primary">Open request queue</a>
      } @else {
        <a routerLink="/requests/new" class="btn btn-primary">+ New Request</a>
      }
    </header>

    @if (loading()) {
      <div class="card load-card">Loading your activity…</div>
    } @else if (error()) {
      <div class="alert alert-error">{{ error() }}</div>
    } @else {
      <section class="stats">
        <div class="stat card">
          <div class="stat-num">{{ total() }}</div>
          <div class="stat-label">Total requests</div>
        </div>
        <div class="stat card">
          <div class="stat-num">{{ countOf(activeStatuses) }}</div>
          <div class="stat-label">In progress</div>
        </div>
        <div class="stat card">
          <div class="stat-num">{{ countOf(['CLOSED']) }}</div>
          <div class="stat-label">Completed</div>
        </div>
        <div class="stat card">
          <div class="stat-num">{{ countOf(['AWAITING_PAYMENT']) }}</div>
          <div class="stat-label">Awaiting payment</div>
        </div>
      </section>

      <section class="recent card">
        <div class="recent-head">
          <h3>{{ isStaff() ? 'Recent across organisation' : 'Recent requests' }}</h3>
          <a [routerLink]="isStaff() ? '/staff/requests' : '/requests'">View all</a>
        </div>
        @if (recent().length === 0) {
          <div class="empty">
            <p>No requests yet.</p>
            @if (!isStaff()) {
              <a routerLink="/requests/new" class="btn btn-primary">Submit your first request</a>
            }
          </div>
        } @else {
          <ul class="recent-list">
            @for (r of recent(); track r.id) {
              <li [class.clickable]="isStaff()" (click)="openIfStaff(r)">
                <div class="r-main">
                  <span class="r-ref">{{ r.referenceNo }}</span>
                  <span class="r-title">{{ r.title }}</span>
                </div>
                <span class="pill" [class]="pillClass(r.status)">{{ label(r.status) }}</span>
              </li>
            }
          </ul>
        }
      </section>
    }
  `,
  styles: [`
    .page-head { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 2rem; gap: 1rem; }
    .page-head h1 { font-size: 2rem; margin-bottom: 0.2rem; }
    .page-head p { margin: 0; font-size: 0.95rem; }
    .head-actions { display: flex; gap: 0.6rem; align-items: center; }

    .load-card { padding: 2rem; color: var(--ink-faint); }

    .stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem; margin-bottom: 1.6rem; }
    .stat { padding: 1.3rem 1.4rem; }
    .stat-num { font-family: var(--font-display); font-size: 2.2rem; font-weight: 600; color: var(--teal-700); line-height: 1; }
    .stat-label { font-size: 0.84rem; color: var(--ink-soft); margin-top: 0.4rem; }
    @media (max-width: 720px) { .stats { grid-template-columns: 1fr 1fr; } }

    .recent { padding: 1.4rem 1.6rem 1.6rem; }
    .recent-head { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 0.8rem; }
    .recent-head h3 { font-size: 1.2rem; }
    .recent-head a { font-size: 0.88rem; }

    .recent-list { list-style: none; margin: 0; padding: 0; }
    .recent-list li {
      display: flex; justify-content: space-between; align-items: center;
      padding: 0.9rem 0; border-bottom: 1px solid var(--line);
    }
    .recent-list li:last-child { border-bottom: none; }
    .r-main { display: flex; flex-direction: column; gap: 0.15rem; }
    .r-ref { font-size: 0.78rem; color: var(--ink-faint); font-weight: 600; letter-spacing: 0.02em; }
    .r-title { font-size: 0.95rem; color: var(--ink); font-weight: 500; }

    .empty { text-align: center; padding: 2rem 1rem; }
    .empty p { color: var(--ink-faint); margin-bottom: 1rem; }
    .recent-list li.clickable { cursor: pointer; }
    .recent-list li.clickable:hover { background: var(--teal-50); margin: 0 -1.6rem; padding-left: 1.6rem; padding-right: 1.6rem; }
  `]
})
export class DashboardComponent implements OnInit {
  private auth = inject(AuthService);
  private api = inject(RequestApi);
  private router = inject(Router);

  protected loading = signal(true);
  protected error = signal<string | null>(null);
  protected all = signal<RequestResponse[]>([]);

  protected readonly activeStatuses: RequestStatus[] = [
    'SUBMITTED', 'UNDER_REVIEW', 'QUOTED', 'PAID', 'ASSIGNED', 'IN_EXECUTION', 'REPORT_SUBMITTED'
  ];

  ngOnInit(): void {
    this.api.list(0, 50).subscribe({
      next: (res) => { this.all.set(res.data.content); this.loading.set(false); },
      error: () => { this.error.set('Could not load your requests.'); this.loading.set(false); }
    });
  }

  firstName(): string {
    return (this.auth.displayName().split(/\s+/)[0]) || 'there';
  }

  isStaff(): boolean {
    return this.auth.hasAnyRole('MANAGER', 'ADMIN', 'FINANCE', 'EXPERT', 'HOD');
  }

  isDepartmentHead(): boolean {
    return this.auth.hasAnyRole('HOD') && !this.auth.hasAnyRole('ADMIN');
  }

  intro(): string {
    if (this.isDepartmentHead()) {
      return 'Review requests in your department, open a new one, and assign a consultant from that department.';
    }
    return this.isStaff()
      ? 'Organisation-wide consultancy overview.'
      : "Here's an overview of your consultancy activity.";
  }

  openIfStaff(r: RequestResponse): void {
    if (this.isStaff()) this.router.navigate(['/staff/requests', r.id]);
  }

  total(): number { return this.all().length; }

  countOf(statuses: string[]): number {
    return this.all().filter(r => statuses.includes(r.status)).length;
  }

  recent(): RequestResponse[] {
    return [...this.all()].slice(0, 5);
  }

  label(s: RequestStatus): string {
    return s.replace(/_/g, ' ').toLowerCase();
  }

  pillClass(s: RequestStatus): string {
    if (s === 'CLOSED') return 'pill-done';
    if (s === 'REJECTED' || s === 'CANCELLED') return 'pill-danger';
    if (s === 'AWAITING_PAYMENT') return 'pill-warn';
    if (s === 'SUBMITTED') return 'pill-neutral';
    return 'pill-active';
  }
}
