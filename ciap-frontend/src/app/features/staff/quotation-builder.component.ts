import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { QuotationApi } from '../../core/api/quotation.api';
import { QuotationItemDto, CreateQuotationRequest } from '../../core/models/api.models';

interface ItemRow { description: string; quantity: number | null; unitPrice: number | null; }

@Component({
  selector: 'app-quotation-builder',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <a [routerLink]="['/staff/requests', requestId]" class="back">← Back to request</a>

    <header class="page-head">
      <h1>Create quotation</h1>
      <p class="muted">Add line items. Totals and VAT are calculated by the server on submit;
        the figures below are a live preview.</p>
    </header>

    @if (error()) { <div class="alert alert-error">{{ error() }}</div> }
    @if (done()) {
      <div class="card panel">
        <div class="alert alert-ok">
          Quotation <strong>{{ doneNo() }}</strong> created (status DRAFT).
        </div>
        <a [routerLink]="['/staff/requests', requestId]" class="btn btn-primary">Back to request</a>
      </div>
    } @else {
      <div class="card panel">
        <table class="items">
          <thead>
            <tr><th>Description</th><th class="num">Qty</th><th class="num">Unit price (TZS)</th><th class="num">Line total</th><th></th></tr>
          </thead>
          <tbody>
            @for (it of items(); track $index) {
              <tr>
                <td><input [(ngModel)]="it.description" placeholder="e.g. Process audit" /></td>
                <td class="num"><input type="number" min="0.01" step="0.01" [(ngModel)]="it.quantity" /></td>
                <td class="num"><input type="number" min="0" step="1000" [(ngModel)]="it.unitPrice" /></td>
                <td class="num lt">{{ lineTotal(it) | number:'1.0-0' }}</td>
                <td class="num">
                  <button class="x" (click)="removeRow($index)" [disabled]="items().length === 1" title="Remove">✕</button>
                </td>
              </tr>
            }
          </tbody>
        </table>
        <button class="btn btn-ghost sm" (click)="addRow()">+ Add line item</button>

        <div class="settings">
          <div class="field-inline">
            <label>VAT rate</label>
            <select [(ngModel)]="vatRate">
              <option [ngValue]="0.18">18% (standard)</option>
              <option [ngValue]="0">0% (exempt)</option>
            </select>
          </div>
          <div class="field-inline">
            <label>Valid until</label>
            <input type="date" [(ngModel)]="validUntil" />
          </div>
        </div>

        <div class="field-inline full">
          <label>Notes (optional)</label>
          <input [(ngModel)]="notes" placeholder="Any conditions or remarks" />
        </div>

        <div class="totals">
          <div class="t-row"><span>Subtotal</span><span>{{ subtotal() | number:'1.0-0' }}</span></div>
          <div class="t-row"><span>VAT ({{ (vatRate * 100) | number:'1.0-0' }}%)</span><span>{{ vatAmount() | number:'1.0-0' }}</span></div>
          <div class="t-row total"><span>Total</span><span>TZS {{ total() | number:'1.0-0' }}</span></div>
        </div>

        <button class="btn btn-primary" [disabled]="busy() || !valid()" (click)="submit()">
          @if (busy()) { <span class="spinner"></span> Creating… } @else { Create quotation }
        </button>
      </div>
    }
  `,
  styles: [`
    .back { display: inline-block; margin-bottom: 1.2rem; font-size: 0.9rem; }
    .page-head { margin-bottom: 1.4rem; }
    .page-head h1 { font-size: 1.9rem; margin-bottom: 0.2rem; }
    .page-head p { margin: 0; font-size: 0.92rem; max-width: 620px; }
    .panel { padding: 1.5rem 1.7rem; max-width: 760px; }

    table.items { width: 100%; border-collapse: collapse; margin-bottom: 0.8rem; }
    table.items th { text-align: left; font-size: 0.76rem; text-transform: uppercase; letter-spacing: 0.03em; color: var(--ink-faint); padding: 0 0.5rem 0.5rem; }
    table.items th.num, td.num { text-align: right; }
    table.items td { padding: 0.3rem 0.5rem; }
    table.items input {
      width: 100%; padding: 0.5rem 0.6rem; border: 1px solid var(--line-2);
      border-radius: var(--radius-sm); font-family: var(--font-body); font-size: 0.9rem;
    }
    td.num input { text-align: right; }
    .lt { font-weight: 600; white-space: nowrap; padding-right: 0.6rem; }
    .x { background: none; border: none; color: var(--ink-faint); cursor: pointer; font-size: 0.9rem; padding: 0.4rem; }
    .x:hover:not(:disabled) { color: var(--danger); }
    .x:disabled { opacity: 0.3; cursor: not-allowed; }

    .btn.sm { padding: 0.45rem 0.8rem; font-size: 0.85rem; }

    .settings { display: flex; gap: 1.5rem; margin: 1.4rem 0 1rem; flex-wrap: wrap; }
    .field-inline { display: flex; flex-direction: column; gap: 0.3rem; }
    .field-inline.full { margin-bottom: 1.2rem; }
    .field-inline label { font-size: 0.8rem; font-weight: 600; color: var(--ink-soft); }
    .field-inline select, .field-inline input {
      padding: 0.55rem 0.7rem; border: 1px solid var(--line-2);
      border-radius: var(--radius-sm); font-family: var(--font-body); font-size: 0.9rem;
    }
    .field-inline.full input { width: 100%; }

    .totals { border-top: 1px solid var(--line); padding-top: 1rem; margin-bottom: 1.3rem; max-width: 320px; margin-left: auto; }
    .t-row { display: flex; justify-content: space-between; padding: 0.3rem 0; font-size: 0.95rem; }
    .t-row.total { border-top: 1px solid var(--line); margin-top: 0.4rem; padding-top: 0.7rem; font-weight: 700; font-size: 1.1rem; font-family: var(--font-display); color: var(--teal-800); }
  `]
})
export class QuotationBuilderComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private api = inject(QuotationApi);

  protected requestId!: number;
  protected items = signal<ItemRow[]>([{ description: '', quantity: 1, unitPrice: null }]);
  protected vatRate = 0.18;
  protected validUntil = '';
  protected notes = '';

  protected busy = signal(false);
  protected error = signal<string | null>(null);
  protected done = signal(false);
  protected doneNo = signal('');

  ngOnInit(): void {
    this.requestId = Number(this.route.snapshot.paramMap.get('id'));
  }

  addRow(): void { this.items.update(rows => [...rows, { description: '', quantity: 1, unitPrice: null }]); }
  removeRow(i: number): void { this.items.update(rows => rows.filter((_, idx) => idx !== i)); }

  lineTotal(it: ItemRow): number {
    const q = Number(it.quantity) || 0;
    const p = Number(it.unitPrice) || 0;
    return q * p;
  }

  subtotal = computed(() => this.items().reduce((s, it) => s + this.lineTotal(it), 0));
  vatAmount = computed(() => this.subtotal() * this.vatRate);
  total = computed(() => this.subtotal() + this.vatAmount());

  valid(): boolean {
    return this.items().every(it =>
      it.description.trim().length > 0 &&
      Number(it.quantity) > 0 &&
      Number(it.unitPrice) >= 0);
  }

  submit(): void {
    if (!this.valid()) return;
    this.busy.set(true);
    this.error.set(null);
    const items: QuotationItemDto[] = this.items().map(it => ({
      description: it.description.trim(),
      quantity: Number(it.quantity),
      unitPrice: Number(it.unitPrice)
    }));
    const body: CreateQuotationRequest = {
      requestId: this.requestId,
      vatRate: this.vatRate,
      validUntil: this.validUntil || undefined,
      notes: this.notes || undefined,
      items
    };
    this.api.create(body).subscribe({
      next: (res) => { this.doneNo.set(res.data.quotationNo); this.done.set(true); this.busy.set(false); },
      error: (err) => {
        this.busy.set(false);
        if (err?.status === 422) this.error.set(err?.error?.message || 'A quotation cannot be created at this stage.');
        else if (err?.status === 403) this.error.set('Only managers can create quotations.');
        else this.error.set('Could not create the quotation. Please try again.');
      }
    });
  }
}
