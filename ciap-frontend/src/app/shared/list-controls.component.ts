import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';

/** Search box that reports the typed text after a short pause. */
@Component({
  selector: 'app-list-search',
  standalone: true,
  imports: [FormsModule],
  template: `
    <label class="list-search">
      <span class="sr">Search</span>
      <input type="search" [placeholder]="placeholder" [ngModel]="text" (ngModelChange)="onType($event)" />
    </label>
  `,
  styles: [`
    :host { display: block; margin-bottom: 1rem; }
    :host(.grow) { flex: 1; min-width: 12rem; margin-bottom: 0; }
    .list-search input {
      width: 100%;
      max-width: 28rem;
      padding: 0.55rem 0.75rem;
      border: 1px solid var(--line-2);
      border-radius: var(--radius-sm);
      font-family: var(--font-body);
      font-size: 0.92rem;
      background: var(--card);
      color: var(--ink);
    }
    :host(.grow) .list-search input { max-width: none; }
    .sr { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); }
  `]
})
export class ListSearchComponent {
  @Input() placeholder = 'Search';
  @Output() queryChange = new EventEmitter<string>();

  protected text = '';
  private timer: ReturnType<typeof setTimeout> | null = null;

  onType(value: string): void {
    this.text = value;
    if (this.timer) clearTimeout(this.timer);
    this.timer = setTimeout(() => this.queryChange.emit(value.trim()), 300);
  }
}

/** Previous / next control for a server page. */
@Component({
  selector: 'app-pager',
  standalone: true,
  template: `
    @if (total > 0) {
      <div class="pager">
        <button type="button" class="btn btn-ghost" [disabled]="page <= 0" (click)="pageChange.emit(page - 1)">← Previous</button>
        <span class="muted">Page {{ page + 1 }} of {{ pages }} · {{ total }} {{ total === 1 ? 'record' : 'records' }}</span>
        <button type="button" class="btn btn-ghost" [disabled]="page + 1 >= pages" (click)="pageChange.emit(page + 1)">Next →</button>
      </div>
    }
  `,
  styles: [`
    .pager { display: flex; align-items: center; justify-content: center; gap: 1.2rem; margin-top: 1.2rem; flex-wrap: wrap; }
  `]
})
export class PagerComponent {
  @Input() page = 0;
  @Input() totalPages = 1;
  @Input() total = 0;
  @Output() pageChange = new EventEmitter<number>();

  get pages(): number {
    return Math.max(1, this.totalPages);
  }
}
