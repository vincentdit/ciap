import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, of, catchError } from 'rxjs';
import { RequestApi } from '../../core/api/request.api';
import { WorkflowApi } from '../../core/api/workflow.api';
import { QuotationApi } from '../../core/api/quotation.api';
import { BillingApi } from '../../core/api/billing.api';
import { ConsultantApi } from '../../core/api/consultant.api';
import { AuthService } from '../../core/auth/auth.service';
import {
  RequestResponse, RequestStatus, QuotationResponse, PaymentResponse, ConsultantResponse
} from '../../core/models/api.models';
import { actionsFor, statusLabel, statusPillClass, WorkflowAction } from './workflow-rules';
import { DocumentsPanelComponent } from './documents-panel.component';

@Component({
  selector: 'app-request-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, DocumentsPanelComponent],
  template: `
    <a routerLink="/staff/requests" class="back">← Back to queue</a>

    @if (loading()) {
      <div class="card load-card">Loading request…</div>
    } @else if (loadError()) {
      <div class="alert alert-error">{{ loadError() }}</div>
    } @else if (request(); as r) {
      <header class="detail-head">
        <div>
          <div class="ref-line">{{ r.referenceNo }}</div>
          <h1>{{ r.title }}</h1>
        </div>
        <span class="pill big" [class]="pill(r.status)">{{ label(r.status) }}</span>
      </header>

      @if (actionMsg()) { <div class="alert alert-ok">{{ actionMsg() }}</div> }
      @if (actionErr()) { <div class="alert alert-error">{{ actionErr() }}</div> }

      <div class="grid">
        <!-- Main column -->
        <div class="col-main">
          <section class="card panel">
            <h3>Details</h3>
            <dl class="meta">
              <dt>Client</dt><dd>{{ r.clientName }}</dd>
              <dt>Type</dt><dd>{{ typeLabel(r.consultancyType) }}</dd>
              <dt>Expected start</dt><dd>{{ r.expectedStartDate || '—' }}</dd>
              <dt>Assigned consultant</dt><dd>{{ r.assignedConsultantName || '—' }}</dd>
            </dl>
            @if (r.description) {
              <div class="desc"><h4>Description</h4><p>{{ r.description }}</p></div>
            }
            @if (r.reviewNotes) {
              <div class="desc"><h4>Review notes</h4><p>{{ r.reviewNotes }}</p></div>
            }
          </section>

          <!-- Quotations -->
          <section class="card panel">
            <h3>Quotations</h3>
            @if (quotations().length === 0) {
              <p class="muted small">No quotation yet.</p>
            } @else {
              <ul class="q-list">
                @for (q of quotations(); track q.id) {
                  <li>
                    <div>
                      <span class="q-no">{{ q.quotationNo }}</span>
                      <span class="pill pill-neutral">{{ q.status }}</span>
                    </div>
                    <div class="q-total">{{ q.currency }} {{ q.total | number:'1.0-0' }}</div>
                  </li>
                }
              </ul>
            }
            @if (canManage() && (r.status === 'UNDER_REVIEW' || r.status === 'QUOTED')) {
              <a [routerLink]="['/staff/requests', r.id, 'quote']" class="btn btn-ghost sm mt">
                + Create quotation
              </a>
            }
          </section>

          <!-- Payments -->
          <section class="card panel">
            <h3>Payments</h3>
            @if (payments().length === 0) {
              <p class="muted small">No control number issued yet.</p>
            } @else {
              <ul class="p-list">
                @for (p of payments(); track p.id) {
                  <li>
                    <div>
                      <span class="p-cn">{{ p.controlNumber }}</span>
                      <span class="pill" [class]="p.status === 'PAID' ? 'pill-done' : 'pill-warn'">{{ p.status }}</span>
                    </div>
                    <div class="p-amt">{{ p.currency }} {{ p.amount | number:'1.0-0' }}</div>
                  </li>
                }
              </ul>
            }
            <!-- FINANCE: simulate a GePG settlement for a pending control number -->
            @if (canFinance() && hasPendingPayment()) {
              <div class="settle">
                <h4>Record GePG settlement</h4>
                <div class="settle-row">
                  <input [(ngModel)]="gepgRef" placeholder="GePG reference" />
                  <button class="btn btn-primary sm" [disabled]="busy() || !gepgRef.trim()"
                          (click)="settle()">Confirm settlement</button>
                </div>
                <p class="hint">Marks the pending control number as paid and advances the workflow.</p>
              </div>
            }
          </section>

          <!-- Documents -->
          <app-documents-panel [requestId]="id" />

          @if (project()) {
            <section class="card panel">
              <h3>Delivery project</h3>
              <p class="muted small">{{ project()!.projectCode }} — {{ project()!.progressPercent }}% complete</p>
              <a [routerLink]="['/staff/projects', project()!.id]" class="btn btn-ghost sm mt">Open project →</a>
            </section>
          } @else if (canManage() && projectCreatable(r.status)) {
            <section class="card panel">
              <h3>Delivery project</h3>
              <p class="muted small">This request is ready to spawn a delivery project.</p>
              <button class="btn btn-ghost sm mt" [disabled]="busy()" (click)="createProject()">
                + Create project
              </button>
            </section>
          }
        </div>

        <!-- Action sidebar -->
        <aside class="col-side">
          <section class="card panel actions">
            <h3>Actions</h3>
            @if (availableActions().length === 0) {
              <p class="muted small">No actions available for your role at this stage.</p>
            }

            @for (a of availableActions(); track a.action) {
              <!-- Review form -->
              @if (a.form === 'review' && openForm() === 'review') {
                <div class="inline-form">
                  <label class="radio">
                    <input type="radio" name="approved" [value]="true" [(ngModel)]="reviewApproved" />
                    Approve &amp; quote
                  </label>
                  <label class="radio">
                    <input type="radio" name="approved" [value]="false" [(ngModel)]="reviewApproved" />
                    Reject
                  </label>
                  <textarea [(ngModel)]="reviewNotes" placeholder="Review notes (optional)"></textarea>
                  <div class="form-btns">
                    <button class="btn btn-primary sm" [disabled]="busy()" (click)="doReview(a)">Submit</button>
                    <button class="btn btn-ghost sm" (click)="openForm.set(null)">Cancel</button>
                  </div>
                </div>
              }
              <!-- Assign form -->
              @else if (a.form === 'assign' && openForm() === 'assign') {
                <div class="inline-form">
                  <select [(ngModel)]="selectedConsultant">
                    <option [ngValue]="null" disabled>Select consultant…</option>
                    @for (c of consultants(); track c.id) {
                      <option [ngValue]="c.id">
                        {{ c.fullName }} — {{ avail(c) }} ({{ c.activeAssignments }} active)
                      </option>
                    }
                  </select>
                  <div class="form-btns">
                    <button class="btn btn-primary sm" [disabled]="busy() || !selectedConsultant"
                            (click)="doAssign(a)">Assign</button>
                    <button class="btn btn-ghost sm" (click)="openForm.set(null)">Cancel</button>
                  </div>
                </div>
              }
              <!-- Plain button (or opener for a form) -->
              @else {
                <button class="btn btn-block action-btn"
                        [class.btn-primary]="a.kind === 'primary'"
                        [class.btn-ghost]="a.kind === 'ghost'"
                        [class.btn-danger]="a.kind === 'danger'"
                        [disabled]="busy()"
                        (click)="onAction(a)">
                  {{ a.label }}
                </button>
              }
            }
          </section>
        </aside>
      </div>
    }
  `,
  styles: [`
    .back { display: inline-block; margin-bottom: 1.2rem; font-size: 0.9rem; }
    .load-card { padding: 2rem; color: var(--ink-faint); }

    .detail-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; margin-bottom: 1.4rem; }
    .ref-line { font-size: 0.82rem; font-weight: 600; color: var(--ink-faint); letter-spacing: 0.02em; margin-bottom: 0.3rem; }
    .detail-head h1 { font-size: 1.8rem; }
    .pill.big { font-size: 0.78rem; padding: 0.35rem 0.8rem; }

    .grid { display: grid; grid-template-columns: 1fr 300px; gap: 1.2rem; align-items: start; }
    @media (max-width: 880px) { .grid { grid-template-columns: 1fr; } }

    .panel { padding: 1.3rem 1.5rem; margin-bottom: 1.2rem; }
    .panel h3 { font-size: 1.1rem; margin-bottom: 0.9rem; }
    .panel h4 { font-size: 0.82rem; text-transform: uppercase; letter-spacing: 0.03em; color: var(--ink-faint); margin: 0 0 0.3rem; }

    .meta { display: grid; grid-template-columns: 130px 1fr; gap: 0.55rem 1rem; margin: 0; }
    .meta dt { color: var(--ink-faint); font-size: 0.86rem; }
    .meta dd { margin: 0; font-size: 0.92rem; font-weight: 500; }
    .desc { margin-top: 1rem; }
    .desc p { margin: 0; font-size: 0.92rem; line-height: 1.55; color: var(--ink-soft); }

    .small { font-size: 0.88rem; }
    .mt { margin-top: 0.9rem; }
    .btn.sm { padding: 0.45rem 0.8rem; font-size: 0.85rem; }

    .q-list, .p-list { list-style: none; margin: 0; padding: 0; }
    .q-list li, .p-list li { display: flex; justify-content: space-between; align-items: center; padding: 0.6rem 0; border-bottom: 1px solid var(--line); }
    .q-list li:last-child, .p-list li:last-child { border-bottom: none; }
    .q-no, .p-cn { font-weight: 600; font-size: 0.86rem; margin-right: 0.6rem; }
    .q-total, .p-amt { font-weight: 600; font-family: var(--font-display); }

    .settle { margin-top: 1rem; padding-top: 1rem; border-top: 1px solid var(--line); }
    .settle-row { display: flex; gap: 0.5rem; }
    .settle-row input { flex: 1; padding: 0.5rem 0.7rem; border: 1px solid var(--line-2); border-radius: var(--radius-sm); font-family: var(--font-body); }

    .actions { position: sticky; top: 1.5rem; }
    .action-btn { margin-bottom: 0.6rem; }
    .btn-danger { background: var(--danger-bg); color: var(--danger); }
    .btn-danger:hover:not(:disabled) { background: #f0d4cf; }

    .inline-form { background: var(--paper-2); border-radius: var(--radius-sm); padding: 0.9rem; margin-bottom: 0.8rem; }
    .inline-form select, .inline-form textarea {
      width: 100%; padding: 0.5rem 0.6rem; border: 1px solid var(--line-2);
      border-radius: var(--radius-sm); font-family: var(--font-body); font-size: 0.88rem; margin-bottom: 0.6rem;
    }
    .inline-form textarea { min-height: 70px; resize: vertical; }
    .radio { display: flex; align-items: center; gap: 0.5rem; font-size: 0.9rem; margin-bottom: 0.4rem; cursor: pointer; }
    .form-btns { display: flex; gap: 0.5rem; }
    .hint { font-size: 0.78rem; color: var(--ink-faint); margin: 0.4rem 0 0; }
  `]
})
export class RequestDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private requestApi = inject(RequestApi);
  private workflow = inject(WorkflowApi);
  private quotationApi = inject(QuotationApi);
  private billingApi = inject(BillingApi);
  private consultantApi = inject(ConsultantApi);
  private auth = inject(AuthService);

  protected loading = signal(true);
  protected loadError = signal<string | null>(null);
  protected busy = signal(false);
  protected actionMsg = signal<string | null>(null);
  protected actionErr = signal<string | null>(null);

  protected request = signal<RequestResponse | null>(null);
  protected quotations = signal<QuotationResponse[]>([]);
  protected payments = signal<PaymentResponse[]>([]);
  protected consultants = signal<ConsultantResponse[]>([]);
  protected project = signal<{ id: number; projectCode: string; progressPercent: number } | null>(null);

  protected openForm = signal<'review' | 'assign' | null>(null);

  // form models
  protected reviewApproved = true;
  protected reviewNotes = '';
  protected selectedConsultant: number | null = null;
  protected gepgRef = '';

  protected id!: number;

  protected availableActions = computed<WorkflowAction[]>(() => {
    const r = this.request();
    if (!r) return [];
    return actionsFor(r.status).filter(a => this.auth.hasAnyRole(...a.roles));
  });

  ngOnInit(): void {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    this.loadAll();
  }

  private loadAll(): void {
    this.loading.set(true);
    this.requestApi.get(this.id).subscribe({
      next: (res) => {
        this.request.set(res.data);
        this.loading.set(false);
        this.loadSideData();
      },
      error: (err) => {
        this.loadError.set(err?.status === 404 ? 'Request not found.' : 'Could not load this request.');
        this.loading.set(false);
      }
    });
  }

  private loadSideData(): void {
    // Pull quotations, payments, consultants, and any project — tolerate 403s per panel.
    forkJoin({
      quotes: this.quotationApi.listForRequest(this.id).pipe(catchError(() => of(null))),
      pays: this.billingApi.listForRequest(this.id).pipe(catchError(() => of(null))),
      consultants: this.canManage()
        ? this.consultantApi.list(0, 100).pipe(catchError(() => of(null)))
        : of(null)
    }).subscribe(({ quotes, pays, consultants }) => {
      if (quotes) this.quotations.set(quotes.data);
      if (pays) this.payments.set(pays.data);
      if (consultants) this.consultants.set(consultants.data.content);
    });
  }

  // ---- role helpers ----
  canManage(): boolean { return this.auth.hasAnyRole('MANAGER', 'ADMIN'); }
  canFinance(): boolean { return this.auth.hasAnyRole('FINANCE', 'ADMIN'); }

  hasPendingPayment(): boolean {
    return this.payments().some(p => p.status === 'PENDING');
  }

  projectCreatable(s: RequestStatus): boolean {
    return s === 'PAID' || s === 'ASSIGNED' || s === 'IN_EXECUTION';
  }

  avail(c: ConsultantResponse): string {
    return c.availabilityStatus.replace(/_/g, ' ').toLowerCase();
  }

  // ---- actions ----
  onAction(a: WorkflowAction): void {
    if (a.form === 'review') { this.openForm.set('review'); return; }
    if (a.form === 'assign') { this.openForm.set('assign'); return; }
    this.run(a.action, () => this.callPlain(a.action));
  }

  private callPlain(action: string) {
    switch (action) {
      case 'start-review': return this.workflow.startReview(this.id);
      case 'await-payment': return this.workflow.awaitPayment(this.id);
      case 'confirm-payment': return this.workflow.confirmPayment(this.id);
      case 'start-execution': return this.workflow.startExecution(this.id);
      case 'submit-report': return this.workflow.submitReport(this.id);
      case 'close': return this.workflow.close(this.id);
      case 'cancel': return this.workflow.cancel(this.id);
      default: return this.workflow.startReview(this.id);
    }
  }

  doReview(_a: WorkflowAction): void {
    this.run('review', () =>
      this.workflow.review(this.id, { approved: this.reviewApproved, notes: this.reviewNotes || undefined }));
  }

  doAssign(_a: WorkflowAction): void {
    if (!this.selectedConsultant) return;
    this.run('assign', () => this.workflow.assign(this.id, { consultantId: this.selectedConsultant! }));
  }

  settle(): void {
    const pending = this.payments().find(p => p.status === 'PENDING');
    if (!pending) return;
    this.busy.set(true);
    this.clearMsgs();
    this.billingApi.gepgCallback({ controlNumber: pending.controlNumber, gepgReference: this.gepgRef.trim() })
      .subscribe({
        next: () => { this.actionMsg.set('Settlement recorded.'); this.gepgRef = ''; this.busy.set(false); this.loadAll(); },
        error: (err) => { this.actionErr.set(this.errText(err)); this.busy.set(false); }
      });
  }

  createProject(): void {
    this.busy.set(true);
    this.clearMsgs();
    this.router.navigate(['/staff/requests', this.id, 'project-new']);
  }

  private run(_name: string, call: () => ReturnType<WorkflowApi['startReview']>): void {
    this.busy.set(true);
    this.clearMsgs();
    call().subscribe({
      next: (res) => {
        this.request.set(res.data);
        this.openForm.set(null);
        this.busy.set(false);
        this.actionMsg.set('Done. Status is now ' + statusLabel(res.data.status) + '.');
        this.loadSideData();
      },
      error: (err) => { this.actionErr.set(this.errText(err)); this.busy.set(false); }
    });
  }

  private errText(err: any): string {
    if (err?.status === 403) return 'Your role is not permitted to perform this action.';
    if (err?.status === 422) return err?.error?.message || 'That transition is not allowed from the current status.';
    if (err?.status === 404) return 'Not found.';
    return err?.error?.message || 'The action failed. Please try again.';
  }

  private clearMsgs(): void { this.actionMsg.set(null); this.actionErr.set(null); }

  label(s: RequestStatus): string { return statusLabel(s); }
  pill(s: RequestStatus): string { return statusPillClass(s); }
  typeLabel(t: string): string { return t.charAt(0) + t.slice(1).toLowerCase(); }
}
