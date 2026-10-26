import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ApiResponse, AuthResponse, ChangePasswordRequest, LoginRequest, RefreshTokenRequest
} from '../models/api.models';

const ACCESS_KEY = 'ciap.accessToken';
const REFRESH_KEY = 'ciap.refreshToken';
const SESSION_KEY = 'ciap.session';

interface StoredSession {
  email: string;
  fullName: string;
  roles: string[];
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly base = environment.apiBaseUrl;

  // Reactive session state for the UI (Angular signals).
  private readonly _session = signal<StoredSession | null>(this.readSession());
  readonly session = this._session.asReadonly();
  readonly isAuthenticated = computed(() => this._session() !== null);
  readonly displayName = computed(() => this._session()?.fullName ?? '');
  readonly email = computed(() => this._session()?.email ?? '');
  readonly roles = computed(() => this._session()?.roles ?? []);

  constructor(private http: HttpClient) {}

  login(body: LoginRequest): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.base}/auth/login`, body)
      .pipe(tap(res => this.store(res.data)));
  }

  refresh(): Observable<ApiResponse<AuthResponse>> {
    const body: RefreshTokenRequest = { refreshToken: this.refreshToken ?? '' };
    return this.http.post<ApiResponse<AuthResponse>>(`${this.base}/auth/refresh`, body)
      .pipe(tap(res => this.store(res.data)));
  }

  changePassword(body: ChangePasswordRequest): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.base}/auth/change-password`, body);
  }

  logout(): void {
    const token = this.accessToken;
    this.clearStoredSession();
    if (!token) {
      return;
    }
    this.http.post(`${this.base}/auth/logout`, {}, {
      headers: { Authorization: `Bearer ${token}` }
    }).subscribe({ error: () => undefined });
  }

  get accessToken(): string | null {
    return localStorage.getItem(ACCESS_KEY);
  }

  get refreshToken(): string | null {
    return localStorage.getItem(REFRESH_KEY);
  }

  hasAnyRole(...wanted: string[]): boolean {
    const mine = this.roles();
    // Backend authorities are prefixed ROLE_; tolerate both forms.
    return wanted.some(w =>
      mine.includes(w) || mine.includes(`ROLE_${w}`) ||
      mine.map(r => r.replace(/^ROLE_/, '')).includes(w));
  }

  private clearStoredSession(): void {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(SESSION_KEY);
    this._session.set(null);
  }

  private store(auth: AuthResponse): void {
    localStorage.setItem(ACCESS_KEY, auth.accessToken);
    localStorage.setItem(REFRESH_KEY, auth.refreshToken);
    const session: StoredSession = {
      email: auth.email, fullName: auth.fullName, roles: auth.roles
    };
    localStorage.setItem(SESSION_KEY, JSON.stringify(session));
    this._session.set(session);
  }

  private readSession(): StoredSession | null {
    const raw = localStorage.getItem(SESSION_KEY);
    if (!raw) return null;
    try { return JSON.parse(raw) as StoredSession; }
    catch { return null; }
  }
}
