import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { RequestApi } from '../../core/api/request.api';
import { ClientApi } from '../../core/api/client.api';
import { DepartmentApi } from '../../core/api/department.api';
import { WorkflowApi } from '../../core/api/workflow.api';
import { CatalogueApi } from '../../core/api/catalogue.api';
import { AuthService } from '../../core/auth/auth.service';
import { CONSULTANCY_TYPES, CategoryOffer, ClientResponse, CreateRequestDto, EligibleAssignee, RequestResponse, ServiceOffer } from '../../core/models/api.models';

@Component({
  selector: 'app-new-request',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterLink],
  template: `
    <header class="page-head">
      <div>
        <h1>New consultancy request</h1>
        <p class="muted">
          @if (isHead()) {
            Open a request for your department and assign a consultant from that department. You can change the consultant on each later action.
          } @else {
            Describe the engagement you'd like TIRDO to scope.
          }
        </p>
      </div>
      <a routerLink="/requests" class="btn btn-ghost">← Back to list</a>
    </header>

    @if (created()) {
      <div class="card success">
        @if (error()) { <div class="alert alert-error">{{ error() }}</div> }
        <div class="alert alert-ok">
          Request submitted. Reference <strong>{{ created()!.referenceNo }}</strong>,
          status <strong>{{ created()!.status }}</strong>.
          @if (created()!.assignedConsultantName) {
            Assigned to <strong>{{ created()!.assignedConsultantName }}</strong>.
          }
        </div>
        <div class="success-actions">
          @if (isHead()) {
            <a [routerLink]="['/staff/requests', created()!.id]" class="btn btn-primary">Review this request</a>
          } @else {
            <a routerLink="/requests" class="btn btn-primary">View my requests</a>
          }
          <button class="btn btn-ghost" (click)="reset()">Submit another</button>
        </div>
      </div>
    } @else {
      <div class="card form-card">
        @if (error()) { <div class="alert alert-error">{{ error() }}</div> }

        <form [formGroup]="form" (ngSubmit)="submit()">
          @if (isHead()) {
            <div class="field">
              <label for="clientId">Organisation *</label>
              <select id="clientId" formControlName="clientId" [class.invalid]="invalid('clientId')">
                <option [ngValue]="null" disabled>Select the client…</option>
                @for (client of clients(); track client.id) {
                  <option [ngValue]="client.id">{{ client.companyName }}</option>
                }
              </select>
              @if (invalid('clientId')) { <div class="error-text">Choose the client organisation.</div> }
              <div class="hint">Opened for {{ departmentName() || 'your department' }}.</div>
            </div>
          } @else if (resolvedClientName()) {
            <div class="field">
              <label>Organisation</label>
              <div class="resolved">{{ resolvedClientName() }}</div>
              <div class="hint">Submitting on behalf of your registered organisation.</div>
            </div>
          } @else if (needsManualId()) {
            <div class="field">
              <label for="clientId">Client ID *</label>
              <input id="clientId" type="number" formControlName="clientId" [class.invalid]="invalid('clientId')"
                     placeholder="Client ID to submit on behalf of">
              @if (invalid('clientId')) { <div class="error-text">A client ID is required.</div> }
              <div class="hint">Your account has no client profile, so specify which client this request is for.</div>
            </div>
          }

          <div class="field">
            <label for="department">Department *</label>
            <select id="department" [disabled]="isHead()" [ngModel]="chosenDepartmentId()" [ngModelOptions]="{standalone: true}"
                    (ngModelChange)="onDepartment($event)">
              <option [ngValue]="null" disabled>Select a department…</option>
              @for (department of catalogueDepartments(); track department.id) {
                <option [ngValue]="department.id">{{ department.name }}</option>
              }
            </select>
            <div class="hint">A request is opened in one department. Its services are listed only for that department.</div>
          </div>

          <div class="field">
            <label for="service">Service</label>
            <select id="service" [disabled]="!chosenDepartmentId()" [ngModel]="offer()?.id ?? null" [ngModelOptions]="{standalone: true}"
                    (ngModelChange)="onService($event)">
              <option [ngValue]="null">Describe the work without a catalogue service</option>
              @for (service of departmentServices(); track service.id) {
                <option [ngValue]="service.id">{{ service.name }}</option>
              }
            </select>
            @if (chosenDepartmentId() && departmentServices().length === 0) {
              <div class="hint">This department has no published services yet.</div>
            }
          </div>

          <div class="field">
            <label for="consultancyType">Consultancy type *</label>
            <select id="consultancyType" formControlName="consultancyType" [class.invalid]="invalid('consultancyType')">
              <option value="" disabled>Select…</option>
              @for (t of types; track t.value) {
                <option [value]="t.value">{{ t.label }}</option>
              }
            </select>
            @if (invalid('consultancyType')) { <div class="error-text">Please choose a type.</div> }
            @if (chosenDepartmentId()) {
              <div class="hint">This follows the department, so the request is reviewed there.</div>
            }
          </div>

          @if (isHead()) {
            <div class="field">
              <label for="consultantId">Consultant from this department</label>
              <select id="consultantId" formControlName="consultantId">
                <option [ngValue]="null">Assign on a later action</option>
                @for (person of assignees(); track person.consultantId) {
                  <option [ngValue]="person.consultantId">{{ person.fullName }} — {{ person.category }}</option>
                }
              </select>
              <div class="hint">Only an expert in {{ departmentName() || 'your department' }} can take this request.</div>
            </div>
          }

          <div class="field">
            <label for="title">Title *</label>
            <input id="title" formControlName="title" [class.invalid]="invalid('title')"
                   placeholder="e.g. Bottling line throughput optimisation">
            @if (invalid('title')) { <div class="error-text">A title is required.</div> }
          </div>

          <div class="field">
            <label for="description">Description</label>
            <textarea id="description" formControlName="description"
                      placeholder="Describe the scope, objectives, and any constraints…"></textarea>
          </div>

          <div class="field">
            <label for="expectedStartDate">Expected start date</label>
            <input id="expectedStartDate" type="date" formControlName="expectedStartDate">
          </div>

          @if (offer(); as chosen) {
            <div class="field price-box">
              <label>Price for {{ chosen.name }}</label>
              <p class="price">{{ chosen.customerLabel }}</p>
              @if (chosen.pricingMethod === 'VARIABLE') {
                <label for="quantity">Quantity ({{ chosen.unitLabel || 'units' }})</label>
                <input id="quantity" type="number" min="0.01" step="0.01" formControlName="quantity">
              }
              @if (chosen.conditions) { <p class="hint">{{ chosen.conditions }}</p> }
            </div>
          }

          <button type="submit" class="btn btn-primary" [disabled]="loading()">
            @if (loading()) { <span class="spinner"></span> Submitting… }
            @else { Submit request }
          </button>
        </form>
      </div>
    }
  `,
  styles: [`
    .page-head { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 2rem; gap: 1rem; }
    .page-head h1 { font-size: 2rem; margin-bottom: 0.2rem; }
    .page-head p { margin: 0; font-size: 0.95rem; }
    .form-card { padding: 1.8rem 2rem; max-width: 600px; }
    .resolved { font-size: 1rem; font-weight: 600; color: var(--teal-800); padding: 0.4rem 0; }
    .success { padding: 1.8rem 2rem; max-width: 600px; }
    .success-actions { display: flex; gap: 0.8rem; }
    .price-box { background: var(--teal-50); border-radius: 10px; padding: 0.9rem 1rem; }
    .price { margin: 0.2rem 0 0.5rem; font-weight: 650; color: var(--teal-800); }
  `]
})
export class NewRequestComponent implements OnInit {
  private fb = inject(FormBuilder);
  private api = inject(RequestApi);
  private clientApi = inject(ClientApi);
  private departments = inject(DepartmentApi);
  private workflow = inject(WorkflowApi);
  private catalogue = inject(CatalogueApi);
  private route = inject(ActivatedRoute);
  private auth = inject(AuthService);

  protected types = CONSULTANCY_TYPES;
  protected loading = signal(false);
  protected error = signal<string | null>(null);
  protected created = signal<RequestResponse | null>(null);
  protected isHead = signal(false);
  protected clients = signal<ClientResponse[]>([]);
  protected assignees = signal<EligibleAssignee[]>([]);
  protected departmentName = signal('');
  protected offer = signal<ServiceOffer | null>(null);
  protected catalogueDepartments = signal<CategoryOffer[]>([]);
  protected departmentServices = signal<ServiceOffer[]>([]);
  protected chosenDepartmentId = signal<number | null>(null);

  // Client resolution: try clients/me; if it resolves we submit silently,
  // otherwise (staff with no client profile) fall back to a manual ID field.
  protected resolvedClientId = signal<number | null>(null);
  protected resolvedClientName = signal<string | null>(null);
  protected needsManualId = signal(false);
  private headDepartmentCode = '';
  private requestedServiceId = 0;
  private pinnedService: ServiceOffer | null = null;

  ngOnInit(): void {
    this.requestedServiceId = Number(this.route.snapshot.queryParamMap.get('service')) || 0;
    this.catalogue.categories().subscribe({
      next: (res) => {
        this.catalogueDepartments.set(res.data);
        this.alignDepartment();
      },
      error: () => this.error.set('Departments could not be loaded.')
    });
    if (this.auth.hasAnyRole('HOD') && !this.auth.hasAnyRole('ADMIN')) {
      this.isHead.set(true);
      this.departments.list().subscribe({
        next: (res) => {
          const department = res.data[0];
          if (!department) return;
          this.departmentName.set(department.name);
          this.headDepartmentCode = department.code;
          this.alignDepartment();
          this.departments.assignees(department.id).subscribe({
            next: (people) => this.assignees.set(people.data),
            error: () => this.error.set('Consultants in this department could not be loaded.')
          });
        }
      });
      this.clientApi.list().subscribe({
        next: (res) => this.clients.set(res.data.content),
        error: () => this.error.set('Client organisations could not be loaded.')
      });
      return;
    }
    this.clientApi.me().subscribe({
      next: (res) => {
        this.resolvedClientId.set(res.data.id);
        this.resolvedClientName.set(res.data.companyName);
        this.form.controls.clientId.clearValidators();
        this.form.controls.clientId.updateValueAndValidity();
      },
      error: () => {
        // No client profile for this user (e.g. MANAGER/ADMIN) — show manual field.
        this.needsManualId.set(true);
      }
    });
  }

  protected form = this.fb.nonNullable.group({
    clientId: [null as number | null, [Validators.required, Validators.min(1)]],
    consultancyType: ['', [Validators.required]],
    title: ['', [Validators.required]],
    description: [''],
    expectedStartDate: [''],
    consultantId: [null as number | null],
    quantity: [null as number | null]
  });

  onDepartment(id: number | null): void {
    if (id == null) return;
    const category = this.catalogueDepartments().find(row => row.id === id);
    if (!category) return;
    this.chosenDepartmentId.set(category.id);
    if (this.pinnedService && this.pinnedService.categoryId !== category.id) {
      this.pinnedService = null;
    }
    this.offer.set(this.pinnedService);
    this.form.controls.consultancyType.setValue(category.consultancyType);
    this.form.controls.consultancyType.disable();
    this.catalogue.services(category.id).subscribe({
      next: (res) => {
        this.departmentServices.set(res.data);
        if (this.pinnedService && res.data.some(row => row.id === this.pinnedService!.id)) {
          this.offer.set(this.pinnedService);
        }
      },
      error: () => this.error.set('Services in this department could not be loaded.')
    });
  }

  onService(id: number | null): void {
    const service = id == null ? null : this.departmentServices().find(row => row.id === id) ?? null;
    this.pinnedService = service;
    this.offer.set(service);
    if (service && !this.form.controls.title.value) {
      this.form.controls.title.setValue(service.name);
    }
  }

  private alignDepartment(): void {
    if (this.isHead() && this.headDepartmentCode && this.catalogueDepartments().length > 0) {
      const line = this.catalogueDepartments().find(row =>
        row.consultancyType === this.headDepartmentCode || row.code === this.headDepartmentCode);
      if (line && this.chosenDepartmentId() !== line.id) {
        this.onDepartment(line.id);
      } else if (!line && this.types.some(type => type.value === this.headDepartmentCode)) {
        this.form.controls.consultancyType.setValue(this.headDepartmentCode);
        this.form.controls.consultancyType.disable();
      }
    }
    if (this.isHead() && !this.headDepartmentCode) return;
    if (this.requestedServiceId > 0 && this.catalogueDepartments().length > 0) {
      const serviceId = this.requestedServiceId;
      this.requestedServiceId = 0;
      this.loadRequestedService(serviceId);
    }
  }

  private loadRequestedService(serviceId: number): void {
    this.catalogue.service(serviceId).subscribe({
      next: (res) => {
        const service = res.data;
        const line = this.catalogueDepartments().find(row => row.id === service.categoryId);
        if (this.isHead() && line && this.chosenDepartmentId() != null && line.id !== this.chosenDepartmentId()) {
          this.error.set('That service belongs to another department.');
          return;
        }
        this.pinnedService = service;
        if (line && this.chosenDepartmentId() !== line.id) {
          this.onDepartment(line.id);
        }
        this.offer.set(service);
        this.form.controls.consultancyType.setValue(service.consultancyType);
        this.form.controls.consultancyType.disable();
        if (!this.form.controls.title.value) this.form.controls.title.setValue(service.name);
      },
      error: () => this.error.set('That service is not available.')
    });
  }

  invalid(name: string): boolean {
    const c = this.form.get(name);
    return !!c && c.invalid && (c.dirty || c.touched);
  }

  submit(): void {
    this.error.set(null);
    const clientId = this.resolvedClientId() ?? Number(this.form.getRawValue().clientId);
    if (!clientId || clientId < 1) {
      this.form.markAllAsTouched();
      this.error.set('A client could not be determined for this request.');
      return;
    }
    if (this.form.controls.consultancyType.invalid || this.form.controls.title.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    const raw = this.form.getRawValue();
    const chosen = this.offer();
    const body: CreateRequestDto = {
      clientId,
      consultancyType: (chosen?.consultancyType || raw.consultancyType) as CreateRequestDto['consultancyType'],
      title: raw.title,
      description: raw.description || undefined,
      expectedStartDate: raw.expectedStartDate || undefined,
      serviceId: chosen?.id,
      seenPriceVersionId: chosen?.priceVersionId ?? undefined,
      quantity: chosen?.pricingMethod === 'VARIABLE' ? Number(raw.quantity) : undefined
    };
    this.api.submit(body).subscribe({
      next: (res) => {
        if (this.isHead() && raw.consultantId) {
          this.workflow.assign(res.data.id, { consultantId: raw.consultantId }).subscribe({
            next: (assigned) => { this.loading.set(false); this.created.set(assigned.data); },
            error: (err) => {
              this.loading.set(false);
              this.created.set(res.data);
              this.error.set(err?.error?.message || 'The request was opened, but the consultant could not be assigned.');
            }
          });
          return;
        }
        this.loading.set(false);
        this.created.set(res.data);
      },
      error: (err) => {
        this.loading.set(false);
        if (err?.status === 404) {
          this.error.set('No client found with that ID. Please check and try again.');
        } else if (err?.status === 422 || err?.status === 400) {
          const message = err?.error?.message || 'Please check the form and try again.';
          this.error.set(message);
          if (message.includes('The price changed') && this.offer()) {
            this.catalogue.service(this.offer()!.id).subscribe({
              next: (res) => this.offer.set(res.data)
            });
          }
        } else if (err?.status === 403) {
          this.error.set('You are not permitted to submit this request.');
        } else {
          this.error.set('Submission failed. Please try again later.');
        }
      }
    });
  }

  reset(): void {
    this.form.reset({ clientId: null, consultancyType: '', title: '', description: '', expectedStartDate: '', consultantId: null, quantity: null });
    this.created.set(null);
    this.error.set(null);
    this.offer.set(null);
    this.pinnedService = null;
    if (this.isHead() && this.chosenDepartmentId()) {
      const category = this.catalogueDepartments().find(row => row.id === this.chosenDepartmentId());
      if (category) {
        this.form.controls.consultancyType.setValue(category.consultancyType);
        this.form.controls.consultancyType.disable();
      }
      return;
    }
    this.chosenDepartmentId.set(null);
    this.departmentServices.set([]);
    this.form.controls.consultancyType.enable();
  }
}
