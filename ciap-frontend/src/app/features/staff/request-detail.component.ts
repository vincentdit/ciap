import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, of, catchError } from 'rxjs';
import { RequestApi } from '../../core/api/request.api';
import { WorkflowApi } from '../../core/api/workflow.api';
import { QuotationApi } from '../../core/api/quotation.api';
import { BillingApi } from '../../core/api/billing.api';
import { AuthService } from '../../core/auth/auth.service';
import {
  RequestResponse, RequestStatus, QuotationResponse, PaymentResponse, RequestEvent,
  EligibleAssignee, AssignmentRecord
} from '../../core/models/api.models';
import { actionsFor, statusLabel, statusPillClass, WorkflowAction } from './workflow-rules';
import { DocumentsPanelComponent } from './documents-panel.component';

@Component({
  selector: 'app-request-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, DocumentsPanelComponent],
  template: `
    <a [routerLink]="backLink()" class="back">← Back to queue</a>

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
              <dt>Department</dt><dd>{{ r.departmentName || 'Not routed' }}</dd>
              <dt>Expected start</dt><dd>{{ r.expectedStartDate || '—' }}</dd>
              <dt>Assigned consultant</dt><dd>{{ r.assignedConsultantName || '—' }}</dd>
              <dt>Assignment deadline</dt><dd>{{ r.assignmentDeadline || '—' }}</dd>
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
                      <span class="pill pill-neutral">v{{ q.versionNo }}</span>
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
                    @if (p.confirmedBy) {
                      <p class="hint">
                        Confirmed by {{ p.confirmedBy }}
                        @if (p.confirmationNote) { <span> — {{ p.confirmationNote }}</span> }
                      </p>
                    }
                  </li>
                }
              </ul>
            }
            @if (canFinance() && hasPendingPayment()) {
              <div class="settle">
                <h4>Manual payment record</h4>
                <p class="hint">This records a finance confirmation. It does not call GePG.</p>
                <div class="settle-row">
                  <input [(ngModel)]="gepgRef" placeholder="Transaction reference" />
                </div>
                <div class="settle-row">
                  <input [(ngModel)]="confirmNote" maxlength="500" placeholder="Reason for this confirmation" />
                </div>
                <div class="settle-row">
                  <input [(ngModel)]="flagReason" maxlength="500" placeholder="Reason if it cannot be confirmed" />
                </div>
                @if (!settleArmed) {
                  <div class="form-btns">
                    <button class="btn btn-primary sm" type="button"
                            [disabled]="busy() || !gepgRef.trim() || !confirmNote.trim()"
                            (click)="settleArmed = true">Review confirmation</button>
                    <button class="btn btn-ghost sm" type="button"
                            [disabled]="busy() || !flagReason.trim()"
                            (click)="flagPayment()">Flag for investigation</button>
                  </div>
                } @else {
                  <p class="hint">Record {{ gepgRef.trim() }} against the pending control number?</p>
                  <div class="form-btns">
                    <button class="btn btn-primary sm" type="button" [disabled]="busy()" (click)="settle()">Record payment</button>
                    <button class="btn btn-ghost sm" type="button" [disabled]="busy()" (click)="settleArmed = false">Back</button>
                  </div>
                }
              </div>
            }
          </section>

          <section class="card panel">
            <h3>Assignment history</h3>
            @if (assignments().length === 0) {
              <p class="muted small">No assignment yet.</p>
            } @else {
              <ul class="q-list">
                @for (row of assignments(); track row.id) {
                  <li>
                    <div>
                      <span class="q-no">{{ row.consultantName }}</span>
                      <span class="muted small">by {{ row.assignedBy }}</span>
                    </div>
                    <div class="muted small">{{ row.createdAt | date:'medium' }}</div>
                    @if (row.previousConsultantName) {
                      <p class="hint">Reassigned from {{ row.previousConsultantName }}</p>
                    }
                    @if (row.reason) { <p class="hint">Reason: {{ row.reason }}</p> }
                    @if (row.note) { <p class="hint">{{ row.note }}</p> }
                  </li>
                }
              </ul>
            }
          </section>

          <section class="card panel">
            <h3>History</h3>
            @if (events().length === 0) {
              <p class="muted small">No status history yet.</p>
            } @else {
              <ul class="q-list">
                @for (event of events(); track event.id) {
                  <li>
                    <div>
                      <span class="q-no">{{ event.fromStatus || 'New' }} → {{ event.toStatus }}</span>
                      <span class="muted small">{{ event.actor }}</span>
                    </div>
                    <div class="muted small">{{ event.createdAt | date:'medium' }}</div>
                    @if (event.reason) { <p class="hint">{{ event.reason }}</p> }
                  </li>
                }
              </ul>
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
                  <textarea [(ngModel)]="reviewNotes" placeholder="Review notes"></textarea>
                  @if (!reviewApproved && !reviewNotes.trim()) {
                    <p class="hint">A rejection needs a reason.</p>
                  }
                  <div class="form-btns">
                    <button class="btn btn-primary sm" [disabled]="busy() || (!reviewApproved && !reviewNotes.trim())" (click)="doReview(a)">Submit</button>
                    <button class="btn btn-ghost sm" (click)="openForm.set(null)">Cancel</button>
                  </div>
                </div>
              }
              <!-- Assign form -->
              @else if (a.form === 'assign' && openForm() === 'assign') {
                <div class="inline-form">
                  <select [(ngModel)]="selectedConsultant">
                    <option [ngValue]="null" disabled>Select a staff member…</option>
                    @for (c of assignees(); track c.consultantId) {
                      <option [ngValue]="c.consultantId">
                        {{ c.fullName }} — {{ c.role }} — {{ c.category }} ({{ c.activeAssignments }} active)
                      </option>
                    }
                  </select>
                  @if (assignees().length === 0) {
                    <p class="hint">No eligible expert is active in this department.</p>
                  }
                  <textarea [(ngModel)]="assignNote" maxlength="500" placeholder="Instructions for the assignee"></textarea>
                  <input type="date" [(ngModel)]="assignDeadline" aria-label="Assignment deadline" />
                  @if (!assignArmed) {
                    <div class="form-btns">
                      <button class="btn btn-primary sm" type="button" [disabled]="busy() || !selectedConsultant"
                              (click)="assignArmed = true">Review assignment</button>
                      <button class="btn btn-ghost sm" type="button" (click)="openForm.set(null)">Cancel</button>
                    </div>
                  } @else {
                    <p class="hint">Assign this request to the selected person?</p>
                    <div class="form-btns">
                      <button class="btn btn-primary sm" type="button" [disabled]="busy()" (click)="doAssign(a)">Confirm assignment</button>
                      <button class="btn btn-ghost sm" type="button" (click)="assignArmed = false">Back</button>
                    </div>
                  }
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
            @if (canLead() && (r.status === 'ASSIGNED' || r.status === 'IN_EXECUTION')) {
              <button class="btn btn-block btn-ghost action-btn" type="button" [disabled]="busy()"
                      (click)="openReassign()">Reassign</button>
              @if (openForm() === 'reassign') {
                <div class="inline-form">
                  <select [(ngModel)]="selectedConsultant">
                    <option [ngValue]="null" disabled>Select the new assignee…</option>
                    @for (c of assignees(); track c.consultantId) {
                      <option [ngValue]="c.consultantId">{{ c.fullName }} — {{ c.activeAssignments }} active</option>
                    }
                  </select>
                  <textarea [(ngModel)]="reassignReason" maxlength="500" placeholder="Reason for reassignment"></textarea>
                  <textarea [(ngModel)]="assignNote" maxlength="500" placeholder="Instructions for the new assignee"></textarea>
                  <input type="date" [(ngModel)]="assignDeadline" aria-label="New deadline" />
                  <div class="form-btns">
                    <button class="btn btn-primary sm" type="button"
                            [disabled]="busy() || !selectedConsultant || !reassignReason.trim()"
                            (click)="doReassign()">Confirm reassignment</button>
                    <button class="btn btn-ghost sm" type="button" (click)="openForm.set(null)">Cancel</button>
                  </div>
                </div>
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
    .settle-row { display: flex; gap: 0.5rem; margin-top: 0.5rem; }
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
  private auth = inject(AuthService);

  protected loading = signal(true);
  protected loadError = signal<string | null>(null);
  protected busy = signal(false);
  protected actionMsg = signal<string | null>(null);
  protected actionErr = signal<string | null>(null);

  protected request = signal<RequestResponse | null>(null);
  protected quotations = signal<QuotationResponse[]>([]);
  protected payments = signal<PaymentResponse[]>([]);
  protected events = signal<RequestEvent[]>([]);
  protected assignees = signal<EligibleAssignee[]>([]);
  protected assignments = signal<AssignmentRecord[]>([]);
  protected project = signal<{ id: number; projectCode: string; progressPercent: number } | null>(null);

  protected openForm = signal<'review' | 'assign' | 'reassign' | null>(null);

  // form models
  protected reviewApproved = true;
  protected reviewNotes = '';
  protected selectedConsultant: number | null = null;
  protected assignNote = '';
  protected assignDeadline = '';
  protected assignArmed = false;
  protected reassignReason = '';
  protected gepgRef = '';
  protected confirmNote = '';
  protected flagReason = '';
  protected settleArmed = false;

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
      events: this.requestApi.events(this.id).pipe(catchError(() => of(null))),
      assignees: this.canLead()
        ? this.requestApi.assignees(this.id).pipe(catchError(() => of(null)))
        : of(null),
      assignments: this.auth.hasAnyRole('HOD', 'ADMIN', 'MANAGER', 'EXPERT')
        ? this.requestApi.assignments(this.id).pipe(catchError(() => of(null)))
        : of(null)
    }).subscribe(({ quotes, pays, events, assignees, assignments }) => {
      if (quotes) this.quotations.set(quotes.data);
      if (pays) this.payments.set(pays.data);
      if (events) this.events.set(events.data);
      if (assignees) this.assignees.set(assignees.data);
      if (assignments) this.assignments.set(assignments.data);
    });
  }

  // ---- role helpers ----
  canManage(): boolean { return this.auth.hasAnyRole('MANAGER', 'ADMIN'); }
  canLead(): boolean { return this.auth.hasAnyRole('HOD', 'ADMIN'); }
  canFinance(): boolean { return this.auth.hasAnyRole('FINANCE', 'ADMIN'); }
  backLink(): string {
    return this.auth.hasAnyRole('HOD') && !this.auth.hasAnyRole('ADMIN') ? '/department' : '/staff/requests';
  }

  hasPendingPayment(): boolean {
    return this.payments().some(p => p.status === 'PENDING');
  }

  projectCreatable(s: RequestStatus): boolean {
    return s === 'PAID' || s === 'ASSIGNED' || s === 'IN_EXECUTION';
  }

  // ---- actions ----
  onAction(a: WorkflowAction): void {
    if (a.form === 'review') { this.openForm.set('review'); return; }
    if (a.form === 'assign') { this.openForm.set('assign'); return; }
    if (a.action === 'await-payment') { this.issueControlNumber(); return; }
    this.run(a.action, () => this.callPlain(a.action));
  }

  private issueControlNumber(): void {
    this.busy.set(true);
    this.clearMsgs();
    this.billingApi.issue(this.id).subscribe({
      next: () => {
        this.actionMsg.set('Control number issued. The request is awaiting payment.');
        this.busy.set(false);
        this.loadAll();
      },
      error: (err) => { this.actionErr.set(this.errText(err)); this.busy.set(false); }
    });
  }

  private callPlain(action: string) {
    switch (action) {
      case 'start-review': return this.workflow.startReview(this.id);
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
    this.run('assign', () => this.workflow.assign(this.id, {
      consultantId: this.selectedConsultant!,
      note: this.assignNote.trim() || undefined,
      deadline: this.assignDeadline || undefined
    }));
  }

  openReassign(): void {
    this.selectedConsultant = null;
    this.openForm.set('reassign');
  }

  doReassign(): void {
    if (!this.selectedConsultant || !this.reassignReason.trim()) return;
    this.busy.set(true);
    this.clearMsgs();
    this.workflow.reassign(this.id, {
      consultantId: this.selectedConsultant,
      reason: this.reassignReason.trim(),
      note: this.assignNote.trim() || undefined,
      deadline: this.assignDeadline || undefined
    }).subscribe({
      next: (res) => {
        this.request.set(res.data);
        this.openForm.set(null);
        this.reassignReason = '';
        this.assignNote = '';
        this.busy.set(false);
        this.actionMsg.set('Reassigned. Earlier notes and documents are unchanged.');
        this.loadSideData();
      },
      error: (err) => { this.actionErr.set(this.errText(err)); this.busy.set(false); }
    });
  }

  settle(): void {
    const pending = this.payments().find(p => p.status === 'PENDING');
    if (!pending) return;
    this.busy.set(true);
    this.clearMsgs();
    const note = this.confirmNote.trim();
    if (!note || !this.gepgRef.trim()) return;
    this.billingApi.gepgCallback({
      controlNumber: pending.controlNumber,
      gepgReference: this.gepgRef.trim(),
      confirmationNote: note
    }).subscribe({
        next: () => {
          this.actionMsg.set('Payment recorded. The request is paid and can be assigned.');
          this.gepgRef = '';
          this.confirmNote = '';
          this.settleArmed = false;
          this.busy.set(false);
          this.loadAll();
        },
        error: (err) => { this.actionErr.set(this.errText(err)); this.busy.set(false); }
      });
  }

  flagPayment(): void {
    const pending = this.payments().find(p => p.status === 'PENDING');
    const reason = this.flagReason.trim();
    if (!pending || !reason) return;
    this.busy.set(true);
    this.clearMsgs();
    this.billingApi.flag(pending.id, reason).subscribe({
      next: () => {
        this.actionMsg.set('Payment flagged. The request is still awaiting payment.');
        this.flagReason = '';
        this.busy.set(false);
        this.loadAll();
      },
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
    if (err?.status === 409) return 'This record was updated by someone else. Reload and try again.';
    return err?.error?.message || 'The action failed. Please try again.';
  }

  private clearMsgs(): void { this.actionMsg.set(null); this.actionErr.set(null); }

  label(s: RequestStatus): string { return statusLabel(s); }
  pill(s: RequestStatus): string { return statusPillClass(s); }
  typeLabel(t: string): string { return t.charAt(0) + t.slice(1).toLowerCase(); }
}
