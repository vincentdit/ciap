import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="shell">
      <aside class="sidebar">
        <div class="brand">
          <img src="assets/tirdo-logo.png" alt="TIRDO" class="brand-logo" />
          <div class="brand-mark">CIAP</div>
          <div class="brand-sub">TIRDO Advisory Portal</div>
        </div>

        <nav class="nav">
          <a routerLink="/dashboard" routerLinkActive="active" class="nav-item">
            <span class="dot"></span> Dashboard
          </a>
          <a routerLink="/requests" routerLinkActive="active" class="nav-item">
            <span class="dot"></span> Consultancy Requests
          </a>
          <a routerLink="/requests/new" routerLinkActive="active" class="nav-item">
            <span class="dot"></span> New Request
          </a>
          <a routerLink="/knowledge" routerLinkActive="active" class="nav-item">
            <span class="dot"></span> Knowledge Portal
          </a>
          <a routerLink="/training" routerLinkActive="active" class="nav-item">
            <span class="dot"></span> Training &amp; Webinars
          </a>

          @if (isStaff()) {
            <div class="nav-group">Staff</div>
            <a routerLink="/staff/requests" routerLinkActive="active" class="nav-item">
              <span class="dot"></span> Request Queue
            </a>
            @if (canManageConsultants()) {
              <a routerLink="/staff/consultants" routerLinkActive="active" class="nav-item">
                <span class="dot"></span> Consultants
              </a>
            }
          }
        </nav>

        <div class="sidebar-foot">
          <div class="user">
            <div class="avatar">{{ initials() }}</div>
            <div class="user-meta">
              <div class="user-name">{{ auth.displayName() }}</div>
              <div class="user-role">{{ primaryRole() }}</div>
            </div>
          </div>
          <button class="btn btn-ghost btn-block" (click)="logout()">Sign out</button>
        </div>
      </aside>

      <main class="content">
        <router-outlet />
      </main>
    </div>
  `,
  styles: [`
    .shell { display: grid; grid-template-columns: 264px 1fr; min-height: 100vh; }

    .sidebar {
      background: var(--teal-900);
      color: #cfe3de;
      display: flex;
      flex-direction: column;
      padding: 1.6rem 1.1rem;
      position: sticky;
      top: 0;
      height: 100vh;
    }
    .brand { padding: 0 0.6rem 1.6rem; border-bottom: 1px solid rgba(255,255,255,0.08); }
    .brand-logo { height: 44px; width: auto; margin-bottom: 0.9rem; display: block; }
    .brand-mark {
      font-family: var(--font-display);
      font-weight: 700;
      font-size: 1.8rem;
      color: #fff;
      letter-spacing: 0.04em;
    }
    .brand-sub { font-size: 0.78rem; color: #8fb3ac; margin-top: 0.15rem; letter-spacing: 0.02em; }

    .nav { display: flex; flex-direction: column; gap: 0.2rem; margin-top: 1.4rem; flex: 1; }
    .nav-group {
      font-size: 0.7rem; font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase;
      color: #6d938b; padding: 0.9rem 0.8rem 0.4rem; margin-top: 0.6rem;
      border-top: 1px solid rgba(255,255,255,0.07);
    }
    .nav-item {
      display: flex; align-items: center; gap: 0.7rem;
      padding: 0.7rem 0.8rem;
      border-radius: var(--radius-sm);
      color: #b8d2cc;
      font-size: 0.92rem;
      font-weight: 500;
      text-decoration: none;
      transition: background 0.15s ease, color 0.15s ease;
    }
    .nav-item:hover { background: rgba(255,255,255,0.05); color: #fff; text-decoration: none; }
    .nav-item.active { background: var(--teal-700); color: #fff; }
    .dot { width: 6px; height: 6px; border-radius: 50%; background: currentColor; opacity: 0.6; }

    .sidebar-foot { border-top: 1px solid rgba(255,255,255,0.08); padding-top: 1rem; }
    .user { display: flex; align-items: center; gap: 0.7rem; margin-bottom: 0.8rem; padding: 0 0.4rem; }
    .avatar {
      width: 38px; height: 38px; border-radius: 50%;
      background: var(--amber-500); color: var(--teal-900);
      display: flex; align-items: center; justify-content: center;
      font-weight: 700; font-size: 0.85rem; flex-shrink: 0;
    }
    .user-name { color: #fff; font-size: 0.88rem; font-weight: 600; }
    .user-role { color: #8fb3ac; font-size: 0.74rem; text-transform: capitalize; }
    .sidebar-foot .btn-ghost { color: #b8d2cc; }
    .sidebar-foot .btn-ghost:hover { background: rgba(255,255,255,0.06); }

    .content { padding: 2.4rem 2.8rem; max-width: 1100px; }

    @media (max-width: 820px) {
      .shell { grid-template-columns: 1fr; }
      .sidebar { position: relative; height: auto; flex-direction: column; }
      .content { padding: 1.6rem 1.2rem; }
    }
  `]
})
export class ShellComponent {
  protected auth = inject(AuthService);
  private router = inject(Router);

  initials(): string {
    const name = this.auth.displayName().trim();
    if (!name) return '–';
    const parts = name.split(/\s+/);
    return (parts[0][0] + (parts.length > 1 ? parts[parts.length - 1][0] : '')).toUpperCase();
  }

  primaryRole(): string {
    const r = this.auth.roles()[0] ?? '';
    return r.replace(/^ROLE_/, '').toLowerCase();
  }

  isStaff(): boolean {
    return this.auth.hasAnyRole('MANAGER', 'ADMIN', 'FINANCE', 'EXPERT');
  }

  canManageConsultants(): boolean {
    return this.auth.hasAnyRole('MANAGER', 'ADMIN');
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
