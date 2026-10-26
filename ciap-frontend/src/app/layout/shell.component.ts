import { Component, ElementRef, HostListener, inject, viewChild } from '@angular/core';
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
            @if (isAdmin()) {
              <a routerLink="/staff/users" routerLinkActive="active" class="nav-item">
                <span class="dot"></span> User management
              </a>
            }
          }
        </nav>
      </aside>

      <div class="main">
        <header class="topbar">
          <div class="account" #account>
            <button type="button" class="account-btn" (click)="toggleMenu($event)"
                    [attr.aria-expanded]="menuOpen" aria-haspopup="menu">
              <span class="avatar">{{ initials() }}</span>
              <span class="user-meta">
                <span class="user-name">{{ auth.displayName() }}</span>
                <span class="user-role">{{ primaryRole() }}</span>
              </span>
              <span class="chevron" aria-hidden="true"></span>
            </button>
            @if (menuOpen) {
              <div class="account-menu" role="menu">
                <div class="menu-head">
                  <div class="user-name">{{ auth.displayName() }}</div>
                  <div class="user-email">{{ auth.email() }}</div>
                </div>
                <a routerLink="/profile" routerLinkActive="active" role="menuitem" (click)="closeMenu()">Profile</a>
                <a routerLink="/account/password" routerLinkActive="active" role="menuitem" (click)="closeMenu()">Change password</a>
                <button type="button" class="signout" role="menuitem" (click)="logout()">Sign out</button>
              </div>
            }
          </div>
        </header>

        <main class="content">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
  styles: [`
    .shell { display: grid; grid-template-columns: 264px 1fr; min-height: 100vh; align-items: stretch; }

    .sidebar {
      background: var(--teal-900);
      color: #cfe3de;
      display: flex;
      flex-direction: column;
      padding: 1.6rem 1.1rem;
      align-self: stretch;
      min-height: 100vh;
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

    .main { min-width: 0; display: flex; flex-direction: column; }
    .topbar {
      display: flex;
      justify-content: flex-end;
      align-items: center;
      padding: 0.75rem 1.6rem;
      background: var(--teal-900);
      border-bottom: 1px solid rgba(255,255,255,0.08);
      position: sticky;
      top: 0;
      z-index: 20;
    }
    .account { position: relative; }
    .account-btn {
      display: flex;
      align-items: center;
      gap: 0.65rem;
      background: transparent;
      border: 1px solid rgba(255,255,255,0.14);
      border-radius: 999px;
      padding: 0.28rem 0.7rem 0.28rem 0.28rem;
      cursor: pointer;
      font-family: var(--font-body);
      color: #fff;
    }
    .account-btn:hover { background: rgba(255,255,255,0.06); }
    .avatar {
      width: 34px; height: 34px; border-radius: 50%;
      background: var(--amber-500); color: var(--teal-900);
      display: flex; align-items: center; justify-content: center;
      font-weight: 700; font-size: 0.78rem; flex-shrink: 0;
    }
    .user-meta { display: flex; flex-direction: column; align-items: flex-start; line-height: 1.2; }
    .account-btn .user-name { color: #fff; font-size: 0.86rem; font-weight: 600; }
    .account-btn .user-role { color: #8fb3ac; font-size: 0.72rem; text-transform: capitalize; }
    .chevron {
      width: 0; height: 0;
      border-left: 4px solid transparent;
      border-right: 4px solid transparent;
      border-top: 5px solid #b8d2cc;
      margin-left: 0.15rem;
    }
    .account-menu {
      position: absolute;
      right: 0;
      top: calc(100% + 0.4rem);
      min-width: 220px;
      background: var(--card);
      border: 1px solid var(--line);
      border-radius: var(--radius);
      box-shadow: var(--shadow-md);
      padding: 0.35rem;
      display: flex;
      flex-direction: column;
    }
    .menu-head { padding: 0.55rem 0.75rem 0.65rem; border-bottom: 1px solid var(--line); margin-bottom: 0.3rem; }
    .menu-head .user-name { color: var(--ink); font-size: 0.9rem; font-weight: 600; }
    .user-email { color: var(--ink-faint); font-size: 0.75rem; margin-top: 0.15rem; word-break: break-all; }
    .account-menu a, .account-menu button {
      text-align: left;
      background: none;
      border: none;
      font-family: var(--font-body);
      font-size: 0.92rem;
      font-weight: 500;
      color: var(--ink);
      padding: 0.6rem 0.75rem;
      border-radius: var(--radius-sm);
      cursor: pointer;
      text-decoration: none;
    }
    .account-menu a:hover, .account-menu button:hover,
    .account-menu a.active {
      background: var(--teal-50);
      color: var(--teal-900);
      text-decoration: none;
    }
    .account-menu .signout { color: var(--danger); }
    .account-menu .signout:hover { background: var(--danger-bg); color: var(--danger); }

    .content { padding: 2rem 2.8rem 2.4rem; max-width: 1100px; }

    @media (max-width: 820px) {
      .shell { grid-template-columns: 1fr; }
      .sidebar { position: relative; min-height: 0; height: auto; flex-direction: column; }
      .topbar { padding: 0.7rem 1rem; }
      .content { padding: 1.6rem 1.2rem; }
      .user-meta { display: none; }
    }
  `]
})
export class ShellComponent {
  protected auth = inject(AuthService);
  private router = inject(Router);
  private accountRef = viewChild<ElementRef<HTMLElement>>('account');
  protected menuOpen = false;

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

  isAdmin(): boolean {
    return this.auth.hasAnyRole('ADMIN');
  }

  toggleMenu(event: MouseEvent): void {
    event.stopPropagation();
    this.menuOpen = !this.menuOpen;
  }

  closeMenu(): void {
    this.menuOpen = false;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.menuOpen) return;
    const root = this.accountRef()?.nativeElement;
    if (root && !root.contains(event.target as Node)) {
      this.menuOpen = false;
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.menuOpen = false;
  }

  logout(): void {
    this.menuOpen = false;
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
