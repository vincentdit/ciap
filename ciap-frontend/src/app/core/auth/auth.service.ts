import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, finalize, shareReplay, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ApiResponse, AuthResponse, ChangePasswordRequest, LoginRequest, RefreshTokenRequest
} from '../models/api.models';

const ACCESS_KEY = 'ciap.accessToken';
const REFRESH_KEY = 'ciap.refreshToken';
const SESSION_KEY = 'ciap.session';
const EXPIRED_KEY = 'ciap.sessionExpired';

interface StoredSession {
  email: string;
  fullName: string;
  roles: string[];
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly base = environment.apiBaseUrl;
  private refreshInFlight: Observable<ApiResponse<AuthResponse>> | null = null;
  private expiryTimer: ReturnType<typeof setTimeout> | null = null;
  /** Ignores a refresh that finishes after the session was already ended. */
  private epoch = 0;

  private readonly _session = signal<StoredSession | null>(this.readSession());
  readonly session = this._session.asReadonly();
  readonly isAuthenticated = computed(() => this._session() !== null);
  readonly displayName = computed(() => this._session()?.fullName ?? '');
  readonly email = computed(() => this._session()?.email ?? '');
  readonly roles = computed(() => this._session()?.roles ?? []);

  constructor(private http: HttpClient, private router: Router) {
    if (this._session() && this.isRefreshExpired()) {
      this.clearStoredSession();
      sessionStorage.setItem(EXPIRED_KEY, '1');
    } else {
      setTimeout(() => this.armExpiry());
    }
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'visible') this.armExpiry();
    });
  }

  login(body: LoginRequest): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.base}/auth/login`, body)
      .pipe(tap(res => this.store(res.data)));
  }

  refresh(): Observable<ApiResponse<AuthResponse>> {
    if (this.refreshInFlight) return this.refreshInFlight;
    const epoch = this.epoch;
    const body: RefreshTokenRequest = { refreshToken: this.refreshToken ?? '' };
    this.refreshInFlight = this.http.post<ApiResponse<AuthResponse>>(`${this.base}/auth/refresh`, body)
      .pipe(
        tap(res => {
          if (epoch === this.epoch) this.store(res.data);
        }),
        finalize(() => { this.refreshInFlight = null; }),
        shareReplay(1)
      );
    return this.refreshInFlight;
  }

  changePassword(body: ChangePasswordRequest): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.base}/auth/change-password`, body);
  }

  logout(): void {
    this.epoch++;
    this.clearExpiryTimer();
    sessionStorage.removeItem(EXPIRED_KEY);
    const token = this.accessToken;
    this.clearStoredSession();
    if (!token) return;
    this.http.post(`${this.base}/auth/logout`, {}, {
      headers: { Authorization: `Bearer ${token}` }
    }).subscribe({ error: () => undefined });
  }

  /**
   * The refresh token is the session. When it is missing or past its expiry,
   * the signed-in UI must end.
   */
  hasUsableSession(): boolean {
    return this._session() !== null && !!this.refreshToken && !this.isRefreshExpired();
  }

  isAccessExpired(): boolean {
    return this.isExpired(this.accessToken);
  }

  isRefreshExpired(): boolean {
    return this.isExpired(this.refreshToken);
  }

  sessionJustExpired(): boolean {
    return sessionStorage.getItem(EXPIRED_KEY) === '1';
  }

  consumeExpiredNotice(): boolean {
    const expired = this.sessionJustExpired();
    if (expired) sessionStorage.removeItem(EXPIRED_KEY);
    return expired;
  }

  /** Clears a dead session and sends the user to sign in. */
  expireSession(): void {
    if (!this._session() && !this.accessToken && !this.refreshToken && !this.sessionJustExpired()) {
      return;
    }
    this.epoch++;
    this.clearExpiryTimer();
    const token = this.accessToken;
    const accessStillValid = !!token && !this.isExpired(token);
    this.clearStoredSession();
    sessionStorage.setItem(EXPIRED_KEY, '1');
    if (accessStillValid && token) {
      this.http.post(`${this.base}/auth/logout`, {}, {
        headers: { Authorization: `Bearer ${token}` }
      }).subscribe({ error: () => undefined });
    }
    const path = this.router.url.split('?')[0];
    if (path !== '/login' && path !== '/register') {
      this.router.navigate(['/login'], { queryParams: { expired: '1' } });
    }
  }

  get accessToken(): string | null {
    return localStorage.getItem(ACCESS_KEY);
  }

  get refreshToken(): string | null {
    return localStorage.getItem(REFRESH_KEY);
  }

  hasAnyRole(...wanted: string[]): boolean {
    const mine = this.roles();
    return wanted.some(w =>
      mine.includes(w) || mine.includes(`ROLE_${w}`) ||
      mine.map(r => r.replace(/^ROLE_/, '')).includes(w));
  }

  private armExpiry(): void {
    this.clearExpiryTimer();
    if (!this._session()) return;
    if (this.isRefreshExpired()) {
      this.expireSession();
      return;
    }
    if (this.isAccessExpired()) {
      this.refresh().subscribe({
        next: () => undefined,
        error: () => this.expireSession()
      });
      return;
    }
    const refreshExp = this.expiryMs(this.refreshToken);
    const accessExp = this.expiryMs(this.accessToken);
    const wakeAt = Math.min(accessExp ?? refreshExp!, refreshExp!);
    const delay = Math.min(Math.max(1000, wakeAt - Date.now()), 2_000_000_000);
    this.expiryTimer = setTimeout(() => this.armExpiry(), delay);
  }

  private isExpired(token: string | null): boolean {
    const exp = this.expiryMs(token);
    return exp === null || exp <= Date.now() + 5000;
  }

  private expiryMs(token: string | null): number | null {
    if (!token) return null;
    const parts = token.split('.');
    if (parts.length < 2) return null;
    try {
      const payload = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      const padded = payload + '='.repeat((4 - (payload.length % 4)) % 4);
      const json = JSON.parse(atob(padded)) as { exp?: number };
      return typeof json.exp === 'number' ? json.exp * 1000 : null;
    } catch {
      return null;
    }
  }

  private clearExpiryTimer(): void {
    if (this.expiryTimer) {
      clearTimeout(this.expiryTimer);
      this.expiryTimer = null;
    }
  }

  private clearStoredSession(): void {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(SESSION_KEY);
    this._session.set(null);
  }

  private store(auth: AuthResponse): void {
    sessionStorage.removeItem(EXPIRED_KEY);
    localStorage.setItem(ACCESS_KEY, auth.accessToken);
    localStorage.setItem(REFRESH_KEY, auth.refreshToken);
    const session: StoredSession = {
      email: auth.email, fullName: auth.fullName, roles: auth.roles
    };
    localStorage.setItem(SESSION_KEY, JSON.stringify(session));
    this._session.set(session);
    this.armExpiry();
  }

  private readSession(): StoredSession | null {
    const raw = localStorage.getItem(SESSION_KEY);
    if (!raw) return null;
    try { return JSON.parse(raw) as StoredSession; }
    catch { return null; }
  }
}
