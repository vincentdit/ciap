import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BillingApi } from '../../core/api/billing.api';
import { TrainingApi } from '../../core/api/training.api';
import { FinancePayment, FinanceSummary, PaymentStatus, TrainingPaymentRow } from '../../core/models/api.models';
import { ListSearchComponent, PagerComponent } from '../../shared/list-controls.component';

@Component({
  selector: 'app-finance',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, ListSearchComponent, PagerComponent],
  template: `
    <header class="page-head">
      <div>
        <h1>Finance</h1>
        <p class="muted">Control numbers, manual payment records, and mismatches. This desk does not call GePG.</p>
      </div>
    </header>

    @if (loadError()) {
      <div class="alert alert-error">{{ loadError() }}</div>
    }

    @if (summary(); as totals) {
      <section class="stats">
        <button type="button" class="stat card" (click)="showQuotedHint()">
          <div class="stat-num">{{ totals.quoted }}</div>
          <div class="stat-label">Quoted, not yet billed</div>
        </button>
        <button type="button" class="stat card" (click)="filterStatus('PENDING')">
          <div class="stat-num">{{ totals.pendingVerification }}</div>
          <div class="stat-label">Awaiting verification</div>
        </button>
        <button type="button" class="stat card" (click)="filterStatus('PAID')">
          <div class="stat-num">{{ totals.verified }}</div>
          <div class="stat-label">Recorded as paid</div>
        </button>
        <button type="button" class="stat card" (click)="filterStatus('FAILED')">
          <div class="stat-num">{{ totals.failed }}</div>
          <div class="stat-label">Flagged</div>
        </button>
        <button type="button" class="stat card" (click)="filterMismatches()">
          <div class="stat-num">{{ totals.mismatches }}</div>
          <div class="stat-label">Status mismatches</div>
        </button>
      </section>
    }

    @if (hint()) { <div class="alert alert-ok">{{ hint() }}</div> }
    @if (actionMsg()) { <div class="alert alert-ok">{{ actionMsg() }}</div> }
    @if (actionErr()) { <div class="alert alert-error">{{ actionErr() }}</div> }

    <section class="card filters">
      <label>Status
        <select [(ngModel)]="status" (ngModelChange)="onFormChange()">
          <option value="">All</option>
          <option value="PENDING">Awaiting verification</option>
          <option value="PAID">Paid</option>
          <option value="FAILED">Flagged</option>
          <option value="CANCELLED">Cancelled</option>
        </select>
      </label>
      <label>From <input type="date" [(ngModel)]="from" (ngModelChange)="onFormChange()" /></label>
      <label>To <input type="date" [(ngModel)]="to" (ngModelChange)="onFormChange()" /></label>
      <app-list-search class="grow" placeholder="CIAP reference, client, or control number" (queryChange)="onQuery($event)" />
    </section>

    <div class="layout">
      <section class="card panel">
        <h2>Payment records</h2>
        @if (loading()) {
          <p class="muted">Loading payments…</p>
        } @else if (rows().length === 0) {
          <p class="muted">No payments match this filter.</p>
        } @else {
          <ul class="rows">
            @for (row of rows(); track row.id) {
              <li>
                <button type="button" class="row" [class.selected]="selected()?.id === row.id" (click)="select(row)">
                  <span>
                    <strong>{{ row.requestReference }}</strong>
                    <span class="muted"> {{ row.clientName }}</span>
                    @if (row.mismatch) { <span class="pill pill-warn">Mismatch</span> }
                  </span>
                  <span class="pill" [class]="row.status === 'PAID' ? 'pill-done' : row.status === 'FAILED' ? 'pill-danger' : 'pill-warn'">{{ row.status }}</span>
                  <span>{{ row.currency }} {{ row.amount | number:'1.0-0' }}</span>
                </button>
              </li>
            }
          </ul>
          <app-pager [page]="page()" [totalPages]="totalPages()" [total]="total()" (pageChange)="search($event)" />
        }
      </section>

      <section class="card panel">
        @if (selected(); as payment) {
          <h2>{{ payment.requestReference }}</h2>
          <dl>
            <div><dt>Client</dt><dd>{{ payment.clientName }}</dd></div>
            <div><dt>Quotation</dt><dd>{{ payment.quotationNo || '—' }}</dd></div>
            <div><dt>Expected</dt><dd>{{ payment.currency }} {{ payment.amount | number:'1.2-2' }}</dd></div>
            <div><dt>Control number</dt><dd>{{ payment.controlNumber }}</dd></div>
            <div><dt>Request status</dt><dd>{{ payment.requestStatus }}</dd></div>
            <div><dt>Recorded reference</dt><dd>{{ payment.gepgReference || '—' }}</dd></div>
            <div><dt>Recorded by</dt><dd>{{ payment.confirmedBy || '—' }}</dd></div>
            @if (payment.confirmationNote) {
              <div><dt>Note</dt><dd>{{ payment.confirmationNote }}</dd></div>
            }
          </dl>
          <a class="btn btn-ghost sm" [routerLink]="['/staff/requests', payment.requestId]">Open request</a>

          @if (payment.status === 'PENDING') {
            <div class="settle">
              <h3>Record payment</h3>
              <label>Transaction reference
                <input [(ngModel)]="reference" />
              </label>
              <label>Reason
                <textarea [(ngModel)]="reason" maxlength="500" rows="3"></textarea>
              </label>
              @if (!armed) {
                <button type="button" class="btn btn-primary sm" [disabled]="busy() || !reference.trim() || !reason.trim()"
                        (click)="armed = true">Review confirmation</button>
              } @else {
                <p class="hint">Record {{ reference.trim() }} for {{ payment.controlNumber }}? This marks the request paid.</p>
                <div class="form-btns">
                  <button type="button" class="btn btn-primary sm" [disabled]="busy()" (click)="record(payment)">Record payment</button>
                  <button type="button" class="btn btn-ghost sm" [disabled]="busy()" (click)="armed = false">Back</button>
                </div>
              }
              <h3>Flag for investigation</h3>
              <label>Reason
                <textarea [(ngModel)]="flagReason" maxlength="500" rows="2"></textarea>
              </label>
              <button type="button" class="btn btn-ghost sm" [disabled]="busy() || !flagReason.trim()" (click)="flag(payment)">
                Flag and keep awaiting payment
              </button>
            </div>
          }
        } @else {
          <p class="muted">Select a payment to see the quotation, amount, and control number.</p>
        }
      </section>
    </div>

    <section class="card panel training-fees">
      <h3>Training registration fees</h3>
      <p class="muted">These are separate from consultancy payments. Recording one confirms that registration. This desk does not call GePG.</p>
      <div class="grid-2">
        <label>Transaction reference <input [(ngModel)]="trainingReference" /></label>
        <label>Reason <input [(ngModel)]="trainingReason" /></label>
      </div>
      @if (trainingPayments().length === 0) {
        <p class="muted">No training fees are awaiting verification.</p>
      } @else {
        @for (row of trainingPayments(); track row.id) {
          <div class="train-row">
            <div>
              <strong>{{ row.eventTitle }}</strong>
              <div class="muted">{{ row.registrantEmail }} · {{ row.currency }} {{ row.amount }} · {{ row.controlNumber }}</div>
            </div>
            <button type="button" class="btn btn-primary sm"
                    [disabled]="busy() || !trainingReference.trim() || !trainingReason.trim()"
                    (click)="recordTraining(row)">Record payment</button>
          </div>
        }
      }
    </section>
  `,
  styles: [`
    .page-head { margin-bottom: 1.2rem; }
    .page-head h1 { font-size: 2rem; margin-bottom: 0.2rem; }
    .training-fees { margin-top: 1.2rem; padding: 1rem 1.2rem; }
    .train-row { display: flex; justify-content: space-between; gap: 1rem; align-items: center; padding: 0.6rem 0; border-top: 1px solid var(--line, #e6eeec); }
    .stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 0.8rem; margin-bottom: 1rem; }
    .stat { text-align: left; cursor: pointer; padding: 0.9rem 1rem; }
    .stat-num { font-family: var(--font-display); font-size: 1.6rem; color: var(--teal-800); }
    .stat-label { color: var(--ink-soft); font-size: 0.82rem; }
    .filters { display: flex; flex-wrap: wrap; gap: 0.7rem; align-items: end; padding: 0.9rem 1rem; margin-bottom: 1rem; }
    .filters label { display: flex; flex-direction: column; gap: 0.25rem; font-size: 0.78rem; color: var(--ink-soft); }
    .filters input, .filters select, textarea {
      padding: 0.45rem 0.6rem; border: 1px solid var(--line-2); border-radius: var(--radius-sm);
      font-family: var(--font-body); font-size: 0.9rem;
    }
    .grow { flex: 1; min-width: 180px; }
    .layout { display: grid; grid-template-columns: 1.2fr 0.8fr; gap: 1rem; }
    .panel { padding: 1rem 1.1rem; }
    .panel h2, .settle h3 { font-size: 1.05rem; margin: 0 0 0.7rem; }
    .rows { list-style: none; margin: 0; padding: 0; }
    .row {
      width: 100%; display: grid; grid-template-columns: 1fr auto auto; gap: 0.6rem; align-items: center;
      text-align: left; background: transparent; border: 0; border-bottom: 1px solid var(--line);
      padding: 0.7rem 0.2rem; cursor: pointer; font: inherit; color: inherit;
    }
    .row.selected { background: var(--paper-2); }
    dl { margin: 0 0 0.8rem; }
    dl div { display: grid; grid-template-columns: 140px 1fr; gap: 0.4rem; margin-bottom: 0.35rem; }
    dt { color: var(--ink-faint); font-size: 0.8rem; }
    dd { margin: 0; }
    .settle { margin-top: 1rem; padding-top: 0.8rem; border-top: 1px solid var(--line); }
    .settle label { display: flex; flex-direction: column; gap: 0.25rem; margin-bottom: 0.6rem; font-size: 0.8rem; color: var(--ink-soft); }
    .form-btns { display: flex; gap: 0.5rem; margin-bottom: 0.8rem; }
    @media (max-width: 820px) { .layout { grid-template-columns: 1fr; } }
  `]
})
export class FinanceComponent implements OnInit {
  private api = inject(BillingApi);
  private trainingApi = inject(TrainingApi);

  protected summary = signal<FinanceSummary | null>(null);
  protected rows = signal<FinancePayment[]>([]);
  protected selected = signal<FinancePayment | null>(null);
  protected loading = signal(true);
  protected busy = signal(false);
  protected loadError = signal<string | null>(null);
  protected actionMsg = signal<string | null>(null);
  protected actionErr = signal<string | null>(null);
  protected hint = signal<string | null>(null);
  protected mismatchesOnly = signal(false);
  protected page = signal(0);
  protected totalPages = signal(1);
  protected total = signal(0);

  protected status = '';
  protected query = '';
  private applyingCard = false;
  private ticket = 0;
  protected from = '';
  protected to = '';
  protected reference = '';
  protected reason = '';
  protected flagReason = '';
  protected armed = false;
  protected trainingPayments = signal<TrainingPaymentRow[]>([]);
  protected trainingReference = '';
  protected trainingReason = '';

  ngOnInit(): void {
    this.api.summary().subscribe({
      next: (res) => this.summary.set(res.data),
      error: () => this.loadError.set('Finance totals could not be loaded.')
    });
    this.search(0);
    this.loadTraining();
  }

  onQuery(q: string): void {
    this.query = q;
    this.search(0);
  }

  onFormChange(): void {
    if (this.applyingCard) return;
    this.mismatchesOnly.set(false);
    this.search(0);
  }

  search(page = 0): void {
    const ticket = ++this.ticket;
    if (this.rows().length === 0) this.loading.set(true);
    this.actionErr.set(null);
    this.api.search({
      status: (this.mismatchesOnly() ? undefined : (this.status || undefined)) as PaymentStatus | undefined,
      q: this.query,
      from: this.from || undefined,
      to: this.to || undefined,
      page,
      mismatches: this.mismatchesOnly()
    }).subscribe({
      next: (res) => {
        if (ticket !== this.ticket) return;
        this.rows.set(res.data.content);
        this.page.set(res.data.page);
        this.totalPages.set(Math.max(1, res.data.totalPages));
        this.total.set(res.data.totalElements);
        this.loading.set(false);
        const current = this.selected();
        if (current) this.selected.set(res.data.content.find(row => row.id === current.id) ?? null);
      },
      error: () => {
        if (ticket !== this.ticket) return;
        this.loadError.set('Payments could not be loaded.');
        this.loading.set(false);
      }
    });
  }

  filterStatus(status: PaymentStatus): void {
    this.applyingCard = true;
    this.status = status;
    this.mismatchesOnly.set(false);
    this.applyingCard = false;
    this.hint.set(null);
    this.search(0);
  }

  filterMismatches(): void {
    this.applyingCard = true;
    this.status = '';
    this.mismatchesOnly.set(true);
    this.applyingCard = false;
    this.hint.set('Showing records where the payment and the request status do not agree. Totals are not changed.');
    this.search(0);
  }

  recordTraining(row: TrainingPaymentRow): void {
    this.busy.set(true);
    this.trainingApi.confirmPayment(row.controlNumber, this.trainingReference.trim(), this.trainingReason.trim()).subscribe({
      next: () => {
        this.busy.set(false);
        this.actionMsg.set('Training payment recorded. The registration is confirmed.');
        this.trainingReference = '';
        this.trainingReason = '';
        this.loadTraining();
      },
      error: (err) => {
        this.busy.set(false);
        this.actionErr.set(err?.error?.message || 'The training payment was not recorded.');
      }
    });
  }

  private loadTraining(): void {
    this.trainingApi.pendingPayments().subscribe({
      next: (res) => this.trainingPayments.set(res.data),
      error: () => this.trainingPayments.set([])
    });
  }

  showQuotedHint(): void {
    this.hint.set('Quoted requests are billed from the request page with Issue control number.');
  }

  select(row: FinancePayment): void {
    this.selected.set(row);
    this.armed = false;
    this.reference = '';
    this.reason = '';
    this.flagReason = '';
    this.actionErr.set(null);
    this.actionMsg.set(null);
  }

  record(payment: FinancePayment): void {
    this.busy.set(true);
    this.actionErr.set(null);
    this.api.gepgCallback({
      controlNumber: payment.controlNumber,
      gepgReference: this.reference.trim(),
      confirmationNote: this.reason.trim()
    }).subscribe({
      next: () => {
        this.actionMsg.set('Payment recorded. The request can now be assigned.');
        this.busy.set(false);
        this.armed = false;
        this.refresh();
      },
      error: (err) => {
        this.actionErr.set(err?.error?.message || 'The payment could not be recorded.');
        this.busy.set(false);
      }
    });
  }

  flag(payment: FinancePayment): void {
    this.busy.set(true);
    this.actionErr.set(null);
    this.api.flag(payment.id, this.flagReason.trim()).subscribe({
      next: () => {
        this.actionMsg.set('Payment flagged. The request is still awaiting payment.');
        this.busy.set(false);
        this.refresh();
      },
      error: (err) => {
        this.actionErr.set(err?.error?.message || 'The payment could not be flagged.');
        this.busy.set(false);
      }
    });
  }

  private refresh(): void {
    this.api.summary().subscribe({ next: (res) => this.summary.set(res.data) });
    this.search(this.page());
  }
}
