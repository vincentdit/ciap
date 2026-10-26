import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <header class="page-head">
      <div>
        <h1>Change password</h1>
        <p class="muted">You will be signed out and asked to sign in with the new password.</p>
      </div>
    </header>

    <section class="card panel">
      @if (error()) {
        <div class="alert alert-error">{{ error() }}</div>
      }

      <form [formGroup]="form" (ngSubmit)="submit()">
        <div class="field">
          <label for="currentPassword">Current password</label>
          <input id="currentPassword" type="password" formControlName="currentPassword"
                 autocomplete="current-password" [class.invalid]="invalid('currentPassword')">
        </div>
        <div class="field">
          <label for="newPassword">New password</label>
          <input id="newPassword" type="password" formControlName="newPassword"
                 autocomplete="new-password" [class.invalid]="invalid('newPassword')">
          <div class="hint">At least 8 characters, and different from the current password.</div>
        </div>
        <div class="field">
          <label for="confirmPassword">Confirm new password</label>
          <input id="confirmPassword" type="password" formControlName="confirmPassword"
                 autocomplete="new-password" [class.invalid]="invalid('confirmPassword')">
          @if (mismatch()) {
            <div class="error-text">The new passwords do not match.</div>
          }
        </div>
        <div class="actions">
          <button class="btn btn-primary" type="submit" [disabled]="loading()">
            @if (loading()) { <span class="spinner"></span> }
            Update password
          </button>
          <a routerLink="/profile" class="btn btn-ghost">Cancel</a>
        </div>
      </form>
    </section>
  `,
  styles: [`
    .page-head { margin-bottom: 1.4rem; }
    .page-head h1 { font-size: 2rem; margin-bottom: 0.2rem; }
    .page-head p { margin: 0; font-size: 0.95rem; max-width: 460px; }
    .panel { padding: 1.4rem 1.5rem; max-width: 460px; }
    .actions { display: flex; align-items: center; gap: 0.4rem; margin-top: 0.4rem; }
  `]
})
export class ChangePasswordComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);

  protected loading = signal(false);
  protected error = signal<string | null>(null);
  protected mismatch = signal(false);

  protected form = this.fb.nonNullable.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', Validators.required]
  });

  invalid(name: 'currentPassword' | 'newPassword' | 'confirmPassword'): boolean {
    const control = this.form.controls[name];
    return control.invalid && (control.dirty || control.touched);
  }

  submit(): void {
    this.error.set(null);
    this.mismatch.set(false);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { currentPassword, newPassword, confirmPassword } = this.form.getRawValue();
    if (newPassword !== confirmPassword) {
      this.mismatch.set(true);
      return;
    }
    this.loading.set(true);
    this.auth.changePassword({ currentPassword, newPassword }).subscribe({
      next: () => {
        this.auth.logout();
        this.router.navigate(['/login'], { queryParams: { changed: '1' } });
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(
          err?.status === 401
            ? 'Current password is incorrect.'
            : err?.status === 422
              ? 'Choose a password that is different from the current one.'
              : err?.status === 400
                ? 'Password must be at least 8 characters.'
                : 'Unable to update the password. Try again.'
        );
      }
    });
  }
}
