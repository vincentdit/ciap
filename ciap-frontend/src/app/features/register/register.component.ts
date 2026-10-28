import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ClientApi } from '../../core/api/client.api';
import {
  CLIENT_TYPES, INDUSTRY_CATEGORIES, ClientRegistrationRequest
} from '../../core/models/api.models';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="reg-wrap">
      <div class="reg-card card">
        <header class="reg-head">
          <div class="brand-mark">CIAP</div>
          <h2>Register your organisation</h2>
          <p class="muted">Create a client account to submit and track consultancy requests with TIRDO.</p>
        </header>

        @if (error()) { <div class="alert alert-error">{{ error() }}</div> }
        @if (done()) {
          <div class="alert alert-ok">
            Registration successful. You can now <a routerLink="/login">sign in</a> with your email and password.
          </div>
        }

        @if (!done()) {
        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="field">
            <label for="companyName">Organisation name *</label>
            <input id="companyName" formControlName="companyName" [class.invalid]="invalid('companyName')"
                   placeholder="e.g. Acme Industries Ltd">
            @if (invalid('companyName')) { <div class="error-text">Organisation name is required.</div> }
          </div>

          <div class="grid-2">
            <div class="field">
              <label for="clientType">Organisation type *</label>
              <select id="clientType" formControlName="clientType" [class.invalid]="invalid('clientType')">
                <option value="" disabled>Select…</option>
                @for (t of clientTypes; track t.value) {
                  <option [value]="t.value">{{ t.label }}</option>
                }
              </select>
              @if (invalid('clientType')) { <div class="error-text">Please choose a type.</div> }
            </div>

            <div class="field">
              <label for="industryCategory">Industry category *</label>
              <select id="industryCategory" formControlName="industryCategory" [class.invalid]="invalid('industryCategory')">
                <option value="" disabled>Select…</option>
                @for (c of industries; track c.value) {
                  <option [value]="c.value">{{ c.label }}</option>
                }
              </select>
              @if (invalid('industryCategory')) { <div class="error-text">Please choose a category.</div> }
            </div>
          </div>

          <div class="field">
            <label for="tin">TIN <span class="muted">(optional)</span></label>
            <input id="tin" formControlName="tin" placeholder="Taxpayer Identification Number">
          </div>

          <div class="grid-2">
            <div class="field">
              <label for="contactPerson">Contact person *</label>
              <input id="contactPerson" formControlName="contactPerson" [class.invalid]="invalid('contactPerson')"
                     placeholder="Full name">
              @if (invalid('contactPerson')) { <div class="error-text">Contact person is required.</div> }
            </div>
            <div class="field">
              <label for="phone">Phone *</label>
              <input id="phone" formControlName="phone" [class.invalid]="invalid('phone')"
                     placeholder="+255…">
              @if (invalid('phone')) { <div class="error-text">Phone is required.</div> }
            </div>
          </div>

          <div class="field">
            <label for="email">Email address *</label>
            <input id="email" type="email" formControlName="email" [class.invalid]="invalid('email')"
                   placeholder="you@organisation.co.tz" autocomplete="username">
            @if (invalid('email')) { <div class="error-text">A valid email is required.</div> }
            <div class="hint">This becomes your login.</div>
          </div>

          <div class="field">
            <label for="password">Password *</label>
            <input id="password" type="password" formControlName="password" [class.invalid]="invalid('password')"
                   placeholder="At least 8 characters" autocomplete="new-password">
            @if (invalid('password')) { <div class="error-text">Password must be at least 8 characters.</div> }
          </div>

          <div class="grid-2">
            <div class="field">
              <label for="region">Region <span class="muted">(optional)</span></label>
              <input id="region" formControlName="region" placeholder="e.g. Dar es Salaam">
            </div>
            <div class="field">
              <label for="country">Country <span class="muted">(optional)</span></label>
              <input id="country" formControlName="country" maxlength="80" placeholder="e.g. Tanzania">
            </div>
            <div class="field">
              <label for="physicalAddress">Physical address <span class="muted">(optional)</span></label>
              <input id="physicalAddress" formControlName="physicalAddress" placeholder="Street / plot">
            </div>
          </div>

          <button type="submit" class="btn btn-primary btn-block" [disabled]="loading()">
            @if (loading()) { <span class="spinner"></span> Registering… }
            @else { Create account }
          </button>
        </form>

        <p class="center foot">
          Already registered? <a routerLink="/login">Sign in</a>
        </p>
        }
      </div>
    </div>
  `,
  styles: [`
    .reg-wrap { min-height: 100vh; display: flex; align-items: flex-start; justify-content: center; padding: 2.5rem 1.2rem; }
    .reg-card { width: 100%; max-width: 560px; padding: 2.2rem 2.4rem; }
    .reg-head { margin-bottom: 1.6rem; }
    .brand-mark {
      font-family: var(--font-display); font-weight: 700; font-size: 1.5rem;
      color: var(--teal-700); letter-spacing: 0.05em; margin-bottom: 1rem;
    }
    .reg-head h2 { font-size: 1.6rem; margin-bottom: 0.3rem; }
    .reg-head p { font-size: 0.92rem; line-height: 1.5; margin: 0; }
    .foot { margin-top: 1.3rem; font-size: 0.9rem; color: var(--ink-soft); }
  `]
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private api = inject(ClientApi);
  private router = inject(Router);

  protected clientTypes = CLIENT_TYPES;
  protected industries = INDUSTRY_CATEGORIES;

  protected loading = signal(false);
  protected error = signal<string | null>(null);
  protected done = signal(false);

  protected form = this.fb.nonNullable.group({
    companyName: ['', [Validators.required]],
    clientType: ['', [Validators.required]],
    industryCategory: ['', [Validators.required]],
    tin: [''],
    contactPerson: ['', [Validators.required]],
    phone: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    region: [''],
    country: [''],
    physicalAddress: ['']
  });

  invalid(name: string): boolean {
    const c = this.form.get(name);
    return !!c && c.invalid && (c.dirty || c.touched);
  }

  submit(): void {
    this.error.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    // getRawValue gives all fields; cast through the request shape.
    const body = this.form.getRawValue() as unknown as ClientRegistrationRequest;
    this.api.register(body).subscribe({
      next: () => {
        this.loading.set(false);
        this.done.set(true);
      },
      error: (err) => {
        this.loading.set(false);
        if (err?.status === 409) {
          this.error.set('An account with this email or TIN already exists.');
        } else if (err?.status === 400 || err?.status === 422) {
          this.error.set(err?.error?.message || 'Please check the form and try again.');
        } else {
          this.error.set('Registration failed. Please try again later.');
        }
      }
    });
  }
}
