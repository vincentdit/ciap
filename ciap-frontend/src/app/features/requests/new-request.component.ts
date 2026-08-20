import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { RequestApi } from '../../core/api/request.api';
import { ClientApi } from '../../core/api/client.api';
import { CONSULTANCY_TYPES, CreateRequestDto, RequestResponse } from '../../core/models/api.models';

@Component({
  selector: 'app-new-request',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <header class="page-head">
      <div>
        <h1>New consultancy request</h1>
        <p class="muted">Describe the engagement you'd like TIRDO to scope.</p>
      </div>
      <a routerLink="/requests" class="btn btn-ghost">← Back to list</a>
    </header>

    @if (created()) {
      <div class="card success">
        <div class="alert alert-ok">
          Request submitted. Reference <strong>{{ created()!.referenceNo }}</strong>,
          status <strong>{{ created()!.status }}</strong>.
        </div>
        <div class="success-actions">
          <a routerLink="/requests" class="btn btn-primary">View my requests</a>
          <button class="btn btn-ghost" (click)="reset()">Submit another</button>
        </div>
      </div>
    } @else {
      <div class="card form-card">
        @if (error()) { <div class="alert alert-error">{{ error() }}</div> }

        <form [formGroup]="form" (ngSubmit)="submit()">
          @if (resolvedClientName()) {
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
            <label for="consultancyType">Consultancy type *</label>
            <select id="consultancyType" formControlName="consultancyType" [class.invalid]="invalid('consultancyType')">
              <option value="" disabled>Select…</option>
              @for (t of types; track t.value) {
                <option [value]="t.value">{{ t.label }}</option>
              }
            </select>
            @if (invalid('consultancyType')) { <div class="error-text">Please choose a type.</div> }
          </div>

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
  `]
})
export class NewRequestComponent implements OnInit {
  private fb = inject(FormBuilder);
  private api = inject(RequestApi);
  private clientApi = inject(ClientApi);
  private router = inject(Router);

  protected types = CONSULTANCY_TYPES;
  protected loading = signal(false);
  protected error = signal<string | null>(null);
  protected created = signal<RequestResponse | null>(null);

  // Client resolution: try clients/me; if it resolves we submit silently,
  // otherwise (staff with no client profile) fall back to a manual ID field.
  protected resolvedClientId = signal<number | null>(null);
  protected resolvedClientName = signal<string | null>(null);
  protected needsManualId = signal(false);

  ngOnInit(): void {
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
    expectedStartDate: ['']
  });

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
    const body: CreateRequestDto = {
      clientId,
      consultancyType: raw.consultancyType as CreateRequestDto['consultancyType'],
      title: raw.title,
      description: raw.description || undefined,
      expectedStartDate: raw.expectedStartDate || undefined
    };
    this.api.submit(body).subscribe({
      next: (res) => { this.loading.set(false); this.created.set(res.data); },
      error: (err) => {
        this.loading.set(false);
        if (err?.status === 404) {
          this.error.set('No client found with that ID. Please check and try again.');
        } else if (err?.status === 422 || err?.status === 400) {
          this.error.set(err?.error?.message || 'Please check the form and try again.');
        } else if (err?.status === 403) {
          this.error.set('You are not permitted to submit this request.');
        } else {
          this.error.set('Submission failed. Please try again later.');
        }
      }
    });
  }

  reset(): void {
    this.form.reset({ clientId: null, consultancyType: '', title: '', description: '', expectedStartDate: '' });
    this.created.set(null);
    this.error.set(null);
  }
}
