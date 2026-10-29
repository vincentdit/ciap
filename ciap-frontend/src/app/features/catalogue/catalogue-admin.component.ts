import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { CatalogueApi, SaveServiceBody } from '../../core/api/catalogue.api';
import { CategoryOffer, PriceVersion, PricingMethod, ServiceOffer } from '../../core/models/api.models';
import { ListSearchComponent, PagerComponent } from '../../shared/list-controls.component';

@Component({
  selector: 'app-catalogue-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, ListSearchComponent, PagerComponent],
  template: `
    <header class="page-head">
      <div>
        <h1>Service prices</h1>
        <p class="muted">Set the price customers see. A quotation is still required when the service says so. This does not record a payment.</p>
      </div>
      <button class="btn btn-primary" type="button" (click)="toggleForm()">
        {{ showForm() ? 'Close' : '+ New service' }}
      </button>
    </header>

    @if (msg()) { <div class="alert alert-ok">{{ msg() }}</div> }
    @if (err()) { <div class="alert alert-error">{{ err() }}</div> }

    @if (showForm()) {
      <div class="card panel form-card" id="service-form">
        <h3>{{ editingId ? 'Edit service' : 'New service' }}</h3>
        <div class="field"><label>Department</label>
          <select [(ngModel)]="draft.categoryId">
            <option [ngValue]="0" disabled>Select a department…</option>
            @for (category of categories(); track category.id) {
              <option [ngValue]="category.id">{{ category.name }}</option>
            }
          </select>
        </div>
        <div class="field"><label>Name</label><input [(ngModel)]="draft.name" /></div>
        <div class="field"><label>Description</label><textarea rows="2" [(ngModel)]="draft.description"></textarea></div>
        <div class="grid-2">
          <div class="field"><label>Pricing</label>
            <select [(ngModel)]="draft.pricingMethod">
              <option value="FIXED">Fixed fee</option>
              <option value="STARTING">Starting price</option>
              <option value="QUOTATION">Quotation required</option>
              <option value="VARIABLE">Price per unit</option>
              <option value="CUSTOM">Approved case by case</option>
            </select>
          </div>
          @if (draft.pricingMethod === 'VARIABLE') {
            <div class="field"><label>Unit</label><input [(ngModel)]="draft.unitLabel" placeholder="sample" /></div>
          }
        </div>
        @if (needsAmount()) {
          <div class="grid-2">
            <div class="field"><label>Amount</label><input type="number" min="0" step="0.01" [(ngModel)]="draft.amount" /></div>
            <div class="field"><label>Currency</label><input maxlength="3" [(ngModel)]="draft.currency" /></div>
          </div>
        }
        <div class="field"><label>Conditions</label><textarea rows="2" [(ngModel)]="draft.conditions"></textarea></div>
        @if (editingId) {
          <div class="field"><label>Note for this price change</label><input [(ngModel)]="draft.note" /></div>
        }
        <label class="check"><input type="checkbox" [(ngModel)]="draft.published" /> Published</label>
        <label class="check"><input type="checkbox" [(ngModel)]="draft.active" /> Active</label>
        <div class="actions">
          <button class="btn btn-primary" type="button" [disabled]="busy() || !draft.name.trim()" (click)="saveService()">
            @if (busy()) { <span class="spinner"></span> Saving… } @else { Save service }
          </button>
          @if (editingId) { <button class="btn btn-ghost" type="button" (click)="resetDraft()">Clear</button> }
        </div>

        @if (history().length) {
          <div class="history">
            <h3>Price history</h3>
            @for (row of history(); track row.id) {
              <p class="hist"><span>{{ row.changedAt | date:'medium' }}</span> · {{ row.changedBy }} · {{ row.customerLabel }}
                @if (row.note) { <span class="muted"> — {{ row.note }}</span> }
              </p>
            }
          </div>
        }
      </div>
    }

    <div class="chips">
      <button type="button" class="chip" [class.on]="departmentId() === null" (click)="pickDepartment(null)">All departments</button>
      @for (category of categories(); track category.id) {
        <button type="button" class="chip" [class.on]="departmentId() === category.id" (click)="pickDepartment(category.id)">
          {{ category.name }}
        </button>
      }
      @if (departmentId() !== null) {
        <button type="button" class="btn btn-ghost sm" (click)="showDepartment.set(!showDepartment())">
          {{ showDepartment() ? 'Hide department' : 'Edit department' }}
        </button>
      }
    </div>

    @if (showDepartment() && selected(); as category) {
      <div class="card panel form-card">
        <h3>{{ category.name }}</h3>
        <div class="field"><label>Name</label><input [(ngModel)]="categoryName" /></div>
        <div class="field"><label>Description</label><textarea rows="2" [(ngModel)]="categoryDescription"></textarea></div>
        <div class="field"><label>Order</label><input type="number" [(ngModel)]="categoryOrder" /></div>
        <label class="check"><input type="checkbox" [(ngModel)]="categoryActive" /> Published to customers</label>
        <button class="btn btn-primary sm" type="button" [disabled]="busy()" (click)="saveCategory()">Save department</button>
      </div>
    }

    <app-list-search placeholder="Service, department, or price" (queryChange)="onQuery($event)" />

    @if (loading()) {
      <div class="card load-card">Loading services…</div>
    } @else if (rows().length === 0) {
      <div class="card empty"><p>{{ query() ? 'No services match this search.' : 'No services in this department.' }}</p></div>
    } @else {
      <div class="card table-card">
        <table>
          <thead>
            <tr>
              <th>Service</th>
              <th>Department</th>
              <th>Price</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (service of rows(); track service.id) {
              <tr>
                <td class="name">{{ service.name }}</td>
                <td>{{ service.categoryName }}</td>
                <td class="price">{{ shortPrice(service) }}</td>
                <td>
                  @if (!service.active) {
                    <span class="pill pill-danger">Inactive</span>
                  } @else if (!service.published) {
                    <span class="pill pill-warn">Draft</span>
                  } @else {
                    <span class="pill pill-active">Published</span>
                  }
                </td>
                <td class="actions">
                  <button class="btn btn-ghost sm" type="button" (click)="edit(service)">Edit</button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
      <app-pager [page]="page()" [totalPages]="totalPages()" [total]="total()" (pageChange)="page.set($event)" />
    }
  `,
  styles: [`
    .page-head { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 1.4rem; gap: 1rem; }
    .page-head h1 { font-size: 2rem; margin-bottom: 0.2rem; }
    .page-head p { margin: 0; font-size: 0.95rem; max-width: 520px; }
    .panel { padding: 1.4rem 1.6rem; margin-bottom: 1.2rem; }
    .form-card { max-width: 720px; }
    .panel h3 { font-size: 1.1rem; margin-bottom: 0.9rem; }
    .chips { display: flex; flex-wrap: wrap; gap: 0.5rem; align-items: center; margin-bottom: 1rem; }
    .chip {
      font-family: var(--font-body); font-size: 0.82rem; font-weight: 600;
      padding: 0.4rem 0.85rem; border-radius: 100px; cursor: pointer;
      background: var(--card); color: var(--ink-soft); border: 1px solid var(--line-2);
    }
    .chip.on { background: var(--teal-700); color: #fff; border-color: var(--teal-700); }
    .check { display: flex; gap: 0.4rem; align-items: center; margin: 0.4rem 0; }
    .actions { display: flex; gap: 0.5rem; margin-top: 0.6rem; }
    .history { margin-top: 1.2rem; padding-top: 0.8rem; border-top: 1px solid var(--line); }
    .hist { font-size: 0.88rem; margin: 0.35rem 0; }
    .grid-2 { display: grid; grid-template-columns: 1fr 160px; gap: 0 1rem; }
    .load-card { padding: 2rem; color: var(--ink-faint); }
    .empty { padding: 2.5rem; text-align: center; }
    .empty p { color: var(--ink-faint); margin: 0; }
    .table-card { padding: 0; overflow: auto; }
    table { width: 100%; border-collapse: collapse; }
    thead th {
      text-align: left; font-size: 0.76rem; font-weight: 700; letter-spacing: 0.04em;
      text-transform: uppercase; color: var(--ink-faint);
      padding: 0.9rem 1.2rem; border-bottom: 1px solid var(--line); background: var(--paper-2);
    }
    tbody td { padding: 0.75rem 1.2rem; border-bottom: 1px solid var(--line); font-size: 0.92rem; vertical-align: middle; }
    tbody tr:last-child td { border-bottom: none; }
    tbody tr:hover { background: var(--teal-50); }
    .name { font-weight: 600; }
    .price { color: var(--teal-800); }
    .actions { text-align: right; white-space: nowrap; }
    .sm { padding: 0.35rem 0.7rem; font-size: 0.82rem; }
    @media (max-width: 720px) {
      .grid-2 { grid-template-columns: 1fr; }
      thead th:nth-child(2), tbody td:nth-child(2) { display: none; }
    }
  `]
})
export class CatalogueAdminComponent implements OnInit {
  private api = inject(CatalogueApi);
  private readonly size = 10;

  protected categories = signal<CategoryOffer[]>([]);
  protected allServices = signal<ServiceOffer[]>([]);
  protected history = signal<PriceVersion[]>([]);
  protected loading = signal(true);
  protected busy = signal(false);
  protected showForm = signal(false);
  protected showDepartment = signal(false);
  protected msg = signal<string | null>(null);
  protected err = signal<string | null>(null);
  protected departmentId = signal<number | null>(null);
  protected page = signal(0);
  protected query = signal('');
  protected categoryName = '';
  protected categoryDescription = '';
  protected categoryOrder = 0;
  protected categoryActive = true;
  protected editingId: number | null = null;
  protected draft: SaveServiceBody = this.empty();

  protected selected = computed(() =>
    this.categories().find(category => category.id === this.departmentId()) ?? null);

  protected filtered = computed(() => {
    const term = this.query().trim().toLowerCase();
    const departmentId = this.departmentId();
    return this.allServices().filter(service => {
      if (departmentId != null && service.categoryId !== departmentId) return false;
      if (!term) return true;
      return `${service.name} ${service.description ?? ''} ${service.categoryName} ${service.customerLabel}`
        .toLowerCase()
        .includes(term);
    });
  });

  protected total = computed(() => this.filtered().length);
  protected totalPages = computed(() => Math.max(1, Math.ceil(this.total() / this.size)));
  protected rows = computed(() => {
    const start = this.page() * this.size;
    return this.filtered().slice(start, start + this.size);
  });

  ngOnInit(): void { this.loadServices(); }

  needsAmount(): boolean {
    return this.draft.pricingMethod === 'FIXED'
      || this.draft.pricingMethod === 'STARTING'
      || this.draft.pricingMethod === 'VARIABLE';
  }

  toggleForm(): void {
    if (this.showForm()) {
      this.showForm.set(false);
      this.editingId = null;
      this.history.set([]);
      return;
    }
    this.resetDraft();
    this.showForm.set(true);
  }

  pickDepartment(id: number | null): void {
    this.departmentId.set(id);
    this.page.set(0);
    this.showDepartment.set(false);
    const category = this.categories().find(row => row.id === id);
    if (category) this.fillCategory(category);
  }

  onQuery(value: string): void {
    this.query.set(value);
    this.page.set(0);
  }

  edit(service: ServiceOffer): void {
    this.editingId = service.id;
    this.draft = {
      categoryId: service.categoryId,
      name: service.name,
      description: service.description || '',
      pricingMethod: service.pricingMethod,
      amount: service.amount,
      currency: service.currency,
      unitLabel: service.unitLabel || '',
      conditions: service.conditions || '',
      published: service.published,
      active: service.active,
      note: ''
    };
    this.showForm.set(true);
    this.api.prices(service.id).subscribe({
      next: (res) => this.history.set(res.data),
      error: () => this.history.set([])
    });
  }

  saveCategory(): void {
    const category = this.selected();
    if (!category) return;
    this.busy.set(true);
    this.err.set(null);
    this.api.saveCategory({
      code: category.code,
      name: this.categoryName,
      description: this.categoryDescription,
      consultancyType: category.consultancyType,
      displayOrder: this.categoryOrder,
      active: this.categoryActive
    }, category.id).subscribe({
      next: () => {
        this.busy.set(false);
        this.msg.set('Department saved.');
        this.showDepartment.set(false);
        this.loadServices();
      },
      error: (err) => { this.busy.set(false); this.err.set(err?.error?.message || 'Department was not saved.'); }
    });
  }

  saveService(): void {
    const category = this.categories().find(row => row.id === this.draft.categoryId);
    if (!category) {
      this.err.set('Choose the department this service belongs to.');
      return;
    }
    this.busy.set(true);
    this.err.set(null);
    const body: SaveServiceBody = { ...this.draft, categoryId: category.id, currency: (this.draft.currency || 'TZS').toUpperCase() };
    if (!this.needsAmount()) body.amount = null;
    this.api.saveService(body, this.editingId || undefined).subscribe({
      next: () => {
        this.busy.set(false);
        this.msg.set('Service saved in ' + category.name + '.');
        this.showForm.set(false);
        this.editingId = null;
        this.history.set([]);
        this.departmentId.set(category.id);
        this.page.set(0);
        this.loadServices();
      },
      error: (err) => { this.busy.set(false); this.err.set(err?.error?.message || 'Service was not saved.'); }
    });
  }

  shortPrice(service: ServiceOffer): string {
    const money = service.amount == null ? '' : `${service.currency} ${service.amount.toLocaleString('en-TZ', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
    switch (service.pricingMethod) {
      case 'FIXED': return money;
      case 'STARTING': return `From ${money}`;
      case 'VARIABLE': return `${money} per ${service.unitLabel || 'unit'}`;
      case 'QUOTATION': return 'Quotation';
      case 'CUSTOM': return 'After approval';
    }
  }

  protected resetDraft(): void {
    this.editingId = null;
    this.history.set([]);
    this.draft = this.empty();
    this.draft.categoryId = this.departmentId() ?? this.categories()[0]?.id ?? 0;
  }

  private fillCategory(category: CategoryOffer): void {
    this.categoryName = category.name;
    this.categoryDescription = category.description || '';
    this.categoryOrder = category.displayOrder;
    this.categoryActive = category.active;
  }

  private loadServices(): void {
    this.api.manageCategories().subscribe({
      next: (res) => {
        this.categories.set(res.data);
        const chosen = this.categories().find(category => category.id === this.departmentId());
        if (chosen) this.fillCategory(chosen);
        if (res.data.length === 0) {
          this.allServices.set([]);
          this.loading.set(false);
          return;
        }
        forkJoin(res.data.map(category => this.api.manageServices(category.id))).subscribe({
          next: (lists) => {
            const rows = lists.flatMap(list => list.data);
            rows.sort((left, right) =>
              left.categoryName.localeCompare(right.categoryName) || left.name.localeCompare(right.name));
            this.allServices.set(rows);
            this.loading.set(false);
          },
          error: () => {
            this.loading.set(false);
            this.err.set('Services could not be loaded.');
          }
        });
      },
      error: () => {
        this.loading.set(false);
        this.err.set('Departments could not be loaded.');
      }
    });
  }

  private empty(): SaveServiceBody {
    return {
      categoryId: 0, name: '', description: '', pricingMethod: 'QUOTATION' as PricingMethod,
      amount: null, currency: 'TZS', unitLabel: '', conditions: '', published: false, active: true, note: ''
    };
  }
}
