import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth-wrap">
      <div class="auth-aside">
        <div class="aside-inner">
          <div class="emblem-row">
            <img src="assets/national-emblem.png" alt="United Republic of Tanzania" class="emblem" />
            <img src="assets/tirdo-logo.png" alt="TIRDO" class="aside-logo" />
          </div>
          <div class="brand-mark">CIAP</div>
          <h1>Consultancy &amp; Industrial Advisory Portal</h1>
          <p>Tanzania Industrial Research and Development Organisation — managing
             industrial consultancy from request through delivery.</p>
        </div>
      </div>

      <div class="auth-panel">
        <div class="auth-card">
          <h2>Sign in</h2>
          <p class="muted sub">Access your consultancy dashboard.</p>

          @if (sessionExpired()) {
            <div class="alert alert-error">Your session expired. Sign in again.</div>
          }
          @if (passwordChanged()) {
            <div class="alert alert-ok">Password updated. Sign in with your new password.</div>
          }
          @if (error()) {
            <div class="alert alert-error">{{ error() }}</div>
          }

          <form [formGroup]="form" (ngSubmit)="submit()">
            <div class="field">
              <label for="email">Email address</label>
              <input id="email" type="email" formControlName="email"
                     [class.invalid]="invalid('email')" autocomplete="username"
                     placeholder="you@organisation.co.tz">
              @if (invalid('email')) {
                <div class="error-text">A valid email is required.</div>
              }
            </div>

            <div class="field">
              <label for="password">Password</label>
              <div class="password-wrap">
                <input id="password" [type]="showPassword() ? 'text' : 'password'" formControlName="password"
                       [class.invalid]="invalid('password')" autocomplete="current-password"
                       placeholder="••••••••">
                <button type="button" class="show-password"
                        (click)="showPassword.set(!showPassword())"
                        [attr.aria-pressed]="showPassword()"
                        [attr.aria-label]="showPassword() ? 'Hide password' : 'Show password'">
                  {{ showPassword() ? 'Hide' : 'Show' }}
                </button>
              </div>
              @if (invalid('password')) {
                <div class="error-text">Password is required.</div>
              }
            </div>

            <button type="submit" class="btn btn-primary btn-block" [disabled]="loading()">
              @if (loading()) { <span class="spinner"></span> Signing in… }
              @else { Sign in }
            </button>
          </form>

          <p class="center foot">
            New client organisation?
            <a routerLink="/register">Register here</a>
          </p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .auth-wrap { display: grid; grid-template-columns: 1.1fr 1fr; min-height: 100vh; }
    .auth-aside {
      background: linear-gradient(150deg, var(--teal-900) 0%, var(--teal-700) 100%);
      color: #d4e9e4;
      display: flex; align-items: center; padding: 3rem;
      position: relative; overflow: hidden;
    }
    .auth-aside::after {
      content: ''; position: absolute; right: -120px; bottom: -120px;
      width: 360px; height: 360px; border-radius: 50%;
      background: rgba(255,255,255,0.04);
    }
    .aside-inner { max-width: 380px; position: relative; z-index: 1; }
    .emblem-row { display: flex; align-items: center; gap: 1rem; margin-bottom: 1.6rem; }
    .emblem { height: 64px; width: auto; }
    .aside-logo { height: 48px; width: auto; }
    .brand-mark {
      font-family: var(--font-display); font-weight: 700; font-size: 2.6rem;
      color: #fff; letter-spacing: 0.05em; margin-bottom: 2rem;
    }
    .auth-aside h1 { color: #fff; font-size: 2rem; line-height: 1.15; margin-bottom: 1rem; }
    .auth-aside p { font-size: 1rem; line-height: 1.6; color: #a9ccc4; }

    .auth-panel { display: flex; align-items: center; justify-content: center; padding: 2rem; background: var(--paper); }
    .auth-card { width: 100%; max-width: 380px; }
    .auth-card h2 { font-size: 1.7rem; margin-bottom: 0.3rem; }
    .sub { margin: 0 0 1.6rem; font-size: 0.92rem; }
    .foot { margin-top: 1.4rem; font-size: 0.9rem; color: var(--ink-soft); }
    .password-wrap { position: relative; }
    .password-wrap input { padding-right: 4.4rem; }
    .show-password {
      position: absolute; right: 0.35rem; top: 50%; transform: translateY(-50%);
      border: 0; background: transparent; color: var(--teal-700);
      font-family: var(--font-body); font-weight: 700; font-size: 0.8rem;
      cursor: pointer; padding: 0.3rem 0.45rem; border-radius: 4px;
    }
    .show-password:hover { background: var(--teal-50); }
    .show-password:focus-visible { outline: 2px solid var(--teal-500); outline-offset: 1px; }

    @media (max-width: 860px) {
      .auth-wrap { grid-template-columns: 1fr; }
      .auth-aside { display: none; }
    }
  `]
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  protected loading = signal(false);
  protected error = signal<string | null>(null);
  protected showPassword = signal(false);
  protected passwordChanged = signal(this.route.snapshot.queryParamMap.get('changed') === '1');
  protected sessionExpired = signal(
    this.route.snapshot.queryParamMap.get('expired') === '1' || this.auth.consumeExpiredNotice()
  );

  protected form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]]
  });

  invalid(name: 'email' | 'password'): boolean {
    const c = this.form.controls[name];
    return c.invalid && (c.dirty || c.touched);
  }

  submit(): void {
    this.error.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        const returnUrl = safeInternalPath(this.route.snapshot.queryParamMap.get('returnUrl'));
        this.router.navigateByUrl(returnUrl);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(
          err?.status === 429
            ? 'Too many sign-in attempts. Wait a minute and try again.'
            : err?.status === 401 || err?.status === 403
            ? 'Invalid email or password.'
            : 'Unable to sign in. Please check your connection and try again.'
        );
      }
    });
  }
}

function safeInternalPath(raw: string | null): string {
  if (!raw || !raw.startsWith('/') || raw.startsWith('//') || raw.includes('\\') || raw.includes('://')) {
    return '/dashboard';
  }
  return raw;
}
