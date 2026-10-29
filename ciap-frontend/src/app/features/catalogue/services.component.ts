import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { CatalogueApi } from '../../core/api/catalogue.api';
import { CategoryOffer, PricingMethod, ServiceOffer } from '../../core/models/api.models';
import { ListSearchComponent, PagerComponent } from '../../shared/list-controls.component';

interface ServiceCard {
  category: CategoryOffer;
  service: ServiceOffer;
}

@Component({
  selector: 'app-services',
  standalone: true,
  imports: [CommonModule, RouterLink, ListSearchComponent, PagerComponent],
  template: `
    <header class="page-head">
      <div>
        <h1>Consultancy services</h1>
        <p class="muted">
          @if (groups().length === 1) {
            Services in {{ groups()[0].category.name }}.
          } @else {
            Choose a department, review the price, then request the service.
          }
        </p>
      </div>
      @if (!loading() && totalServices() > 0) {
        <p class="count">{{ totalServices() }} {{ totalServices() === 1 ? 'service' : 'services' }}</p>
      }
    </header>

    @if (error()) { <div class="alert alert-error">{{ error() }}</div> }
    @if (loading()) {
      <div class="card load-card">Loading services…</div>
    } @else if (groups().length === 0) {
      <div class="card empty"><p>No consultancy services are published yet.</p></div>
    } @else {
      <app-list-search placeholder="Service, department, or description" (queryChange)="onQuery($event)" />

      @if (groups().length > 1) {
      <div class="chips">
        <button type="button" class="chip" [class.on]="departmentId() === null" (click)="pickDepartment(null)">
          All departments
        </button>
        @for (group of groups(); track group.category.id) {
          <button type="button" class="chip" [class.on]="departmentId() === group.category.id"
                  (click)="pickDepartment(group.category.id)">
            {{ group.category.name }}
            <span class="chip-count">{{ group.services.length }}</span>
          </button>
        }
      </div>
      }

      @if (visible().length === 0) {
        <div class="card empty"><p>{{ query() ? 'No services match this search.' : 'No services in this department.' }}</p></div>
      } @else {
        <div class="service-list">
          @for (item of rows(); track item.service.id) {
            <article class="card offer">
              <div class="main">
                <div class="head">
                  <span class="pill pill-active">{{ item.category.name }}</span>
                  <span class="pill pill-neutral">{{ methodLabel(item.service.pricingMethod) }}</span>
                </div>
                <h3>{{ item.service.name }}</h3>
                @if (item.service.description) { <p class="desc">{{ item.service.description }}</p> }
                <p class="price">{{ priceHeadline(item.service) }}</p>
                <p class="note">{{ priceNote(item.service) }}</p>
              </div>
              <div class="action">
                <a class="btn btn-primary sm" [routerLink]="['/requests/new']" [queryParams]="{ service: item.service.id }">
                  Request this service
                </a>
              </div>
            </article>
          }
        </div>
        <app-pager [page]="page()" [totalPages]="totalPages()" [total]="visible().length" (pageChange)="page.set($event)" />
      }
    }
  `,
  styles: [`
    .page-head { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 1.4rem; gap: 1rem; }
    .page-head h1 { font-size: 2rem; margin-bottom: 0.2rem; }
    .page-head p { margin: 0; font-size: 0.95rem; }
    .count { font-size: 0.9rem; font-weight: 600; color: var(--teal-800); white-space: nowrap; }

    .chips { display: flex; flex-wrap: wrap; gap: 0.5rem; margin: 0 0 1.2rem; }
    .chip {
      font-family: var(--font-body);
      font-size: 0.82rem;
      font-weight: 600;
      padding: 0.4rem 0.85rem;
      border-radius: 100px;
      cursor: pointer;
      background: var(--card);
      color: var(--ink-soft);
      border: 1px solid var(--line-2);
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
    }
    .chip.on { background: var(--teal-700); color: #fff; border-color: var(--teal-700); }
    .chip-count {
      font-size: 0.72rem;
      min-width: 1.2rem;
      padding: 0.05rem 0.35rem;
      border-radius: 100px;
      background: var(--paper-2);
      color: var(--ink-soft);
    }
    .chip.on .chip-count { background: rgba(255, 255, 255, 0.2); color: #fff; }

    .load-card { padding: 2rem; color: var(--ink-faint); }
    .empty { padding: 2.5rem; text-align: center; }
    .empty p { color: var(--ink-faint); margin: 0; }

    .service-list { display: flex; flex-direction: column; gap: 1rem; }
    .offer { padding: 1.3rem 1.5rem; display: flex; justify-content: space-between; gap: 1.5rem; align-items: center; }
    .head { display: flex; flex-wrap: wrap; gap: 0.4rem; margin-bottom: 0.55rem; }
    .offer h3 { font-size: 1.15rem; margin-bottom: 0.35rem; }
    .desc {
      font-size: 0.9rem;
      color: var(--ink-soft);
      line-height: 1.5;
      margin: 0 0 0.7rem;
      display: -webkit-box;
      -webkit-line-clamp: 3;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
    .price { font-weight: 650; color: var(--teal-800); margin: 0; font-size: 1rem; }
    .note { margin: 0.25rem 0 0; font-size: 0.82rem; color: var(--ink-faint); line-height: 1.4; }
    .action { flex-shrink: 0; }
    .btn.sm { padding: 0.55rem 0.95rem; font-size: 0.88rem; text-decoration: none; }
    .btn.sm:hover { text-decoration: none; }

    @media (max-width: 720px) {
      .page-head { align-items: flex-start; flex-direction: column; }
      .offer { flex-direction: column; align-items: stretch; }
      .action .btn { width: 100%; justify-content: center; }
    }
  `]
})
export class ServicesComponent implements OnInit {
  private api = inject(CatalogueApi);
  protected groups = signal<{ category: CategoryOffer; services: ServiceOffer[] }[]>([]);
  protected loading = signal(true);
  protected error = signal<string | null>(null);
  protected query = signal('');
  protected departmentId = signal<number | null>(null);
  protected page = signal(0);
  private readonly size = 10;

  protected totalServices = computed(() =>
    this.groups().reduce((sum, group) => sum + group.services.length, 0));

  protected visible = computed(() => {
    const term = this.query().trim().toLowerCase();
    const departmentId = this.departmentId();
    const cards: ServiceCard[] = [];
    for (const group of this.groups()) {
      if (departmentId != null && group.category.id !== departmentId) continue;
      for (const service of group.services) {
        const haystack = `${service.name} ${service.description ?? ''} ${group.category.name}`.toLowerCase();
        if (!term || haystack.includes(term)) {
          cards.push({ category: group.category, service });
        }
      }
    }
    return cards;
  });

  protected totalPages = computed(() => Math.max(1, Math.ceil(this.visible().length / this.size)));
  protected rows = computed(() => {
    const start = this.page() * this.size;
    return this.visible().slice(start, start + this.size);
  });

  onQuery(value: string): void {
    this.query.set(value);
    this.page.set(0);
  }

  pickDepartment(id: number | null): void {
    this.departmentId.set(id);
    this.page.set(0);
  }

  ngOnInit(): void {
    this.api.categories().subscribe({
      next: (res) => {
        const categories = res.data;
        if (categories.length === 0) {
          this.loading.set(false);
          return;
        }
        forkJoin(categories.map(category => this.api.services(category.id))).subscribe({
          next: (lists) => {
            this.groups.set(categories.map((category, index) => ({
              category,
              services: lists[index].data
            })));
            this.loading.set(false);
          },
          error: () => {
            this.loading.set(false);
            this.error.set('Services could not be loaded.');
          }
        });
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Services could not be loaded.');
      }
    });
  }

  methodLabel(method: PricingMethod): string {
    switch (method) {
      case 'FIXED': return 'Fixed fee';
      case 'STARTING': return 'Starting price';
      case 'VARIABLE': return 'Per unit';
      case 'QUOTATION': return 'Quotation';
      case 'CUSTOM': return 'Case by case';
    }
  }

  priceHeadline(service: ServiceOffer): string {
    const money = service.amount == null ? '' : this.money(service.amount, service.currency);
    switch (service.pricingMethod) {
      case 'FIXED': return money;
      case 'STARTING': return `From ${money}`;
      case 'VARIABLE': return `${money} per ${service.unitLabel || 'unit'}`;
      case 'QUOTATION': return 'Quotation after review';
      case 'CUSTOM': return 'Price set after approval';
    }
  }

  priceNote(service: ServiceOffer): string {
    if (service.pricingMethod === 'QUOTATION' || service.pricingMethod === 'CUSTOM') {
      return 'No final payment amount has been established yet.';
    }
    return service.conditions || 'The quotation is the amount payable.';
  }

  private money(amount: number, currency: string): string {
    const formatted = new Intl.NumberFormat('en-TZ', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(amount);
    return `${currency || 'TZS'} ${formatted}`;
  }
}
