import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationApi } from '../../core/api/notification.api';
import { AppNotification } from '../../core/models/api.models';
import { I18nService } from '../../core/i18n/i18n.service';
import { ListSearchComponent, PagerComponent } from '../../shared/list-controls.component';

@Component({
  selector: 'app-notices',
  standalone: true,
  imports: [CommonModule, ListSearchComponent, PagerComponent],
  template: `
    <header class="page-head">
      <div>
        <h1>{{ i18n.t('notices.title') }}</h1>
        <p class="muted">{{ i18n.t('notices.intro') }}</p>
      </div>
    </header>

    <app-list-search placeholder="Search notices" (queryChange)="onQuery($event)" />

    @if (error()) {
      <div class="alert alert-error">{{ error() }}</div>
    } @else if (loading()) {
      <p class="muted">Loading notices…</p>
    } @else if (notices().length === 0) {
      <section class="card panel">
        <p class="muted">{{ query ? 'No notices match this search.' : i18n.t('notices.empty') }}</p>
      </section>
    } @else {
      <ul class="notice-list">
        @for (notice of notices(); track notice.id) {
          <li class="card panel">
            <p>{{ notice.message }}</p>
            <p class="muted small">{{ notice.createdAt | date:'medium' }}</p>
          </li>
        }
      </ul>
      <app-pager [page]="page()" [totalPages]="totalPages()" [total]="total()" (pageChange)="load($event)" />
    }
  `,
  styles: [`
    .page-head { margin-bottom: 1.4rem; }
    .page-head h1 { font-size: 2rem; margin-bottom: 0.2rem; }
    .page-head p { margin: 0; }
    .panel { padding: 1rem 1.2rem; margin-bottom: 0.8rem; }
    .panel p { margin: 0 0 0.3rem; }
    .notice-list { list-style: none; padding: 0; margin: 0; }
    .small { font-size: 0.8rem; }
  `]
})
export class NoticesComponent implements OnInit {
  protected i18n = inject(I18nService);
  private api = inject(NotificationApi);
  protected notices = signal<AppNotification[]>([]);
  protected loading = signal(true);
  protected error = signal<string | null>(null);
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

  load(p: number): void {
    const ticket = ++this.ticket;
    if (this.notices().length === 0) this.loading.set(true);
    this.error.set(null);
    this.api.list(p, this.size, this.query).subscribe({
      next: (res) => {
        if (ticket !== this.ticket) return;
        this.notices.set(res.data.content);
        this.page.set(res.data.page);
        this.totalPages.set(Math.max(1, res.data.totalPages));
        this.total.set(res.data.totalElements);
        this.loading.set(false);
      },
      error: () => {
        if (ticket !== this.ticket) return;
        this.error.set('Notices could not be loaded.');
        this.loading.set(false);
      }
    });
  }
}
