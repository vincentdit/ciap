import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { ClientApi } from '../../core/api/client.api';
import { ClientResponse } from '../../core/models/api.models';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <header class="page-head">
      <div>
        <h1>Profile</h1>
        <p class="muted">Your account details.</p>
      </div>
    </header>

    <section class="card panel">
      <div class="who">
        <div class="avatar">{{ initials() }}</div>
        <div>
          <h2>{{ auth.displayName() }}</h2>
          <p class="muted">{{ auth.email() }}</p>
        </div>
      </div>
      <dl>
        <div>
          <dt>Role</dt>
          <dd>{{ roleLabel() }}</dd>
        </div>
        <div>
          <dt>Email</dt>
          <dd>{{ auth.email() }}</dd>
        </div>
      </dl>
      <a routerLink="/account/password" class="btn btn-primary">Change password</a>
    </section>

    @if (isClient()) {
      <section class="card panel">
        <h3>Organisation</h3>
        @if (loading()) {
          <p class="muted">Loading organisation profile…</p>
        } @else if (error()) {
          <div class="alert alert-error">{{ error() }}</div>
        } @else if (client(); as org) {
          <dl>
            <div><dt>Organisation</dt><dd>{{ org.companyName }}</dd></div>
            <div><dt>Contact</dt><dd>{{ org.contactPerson }}</dd></div>
            <div><dt>Phone</dt><dd>{{ org.phone }}</dd></div>
            <div><dt>Type</dt><dd>{{ label(org.clientType) }}</dd></div>
            <div><dt>Industry</dt><dd>{{ label(org.industryCategory) }}</dd></div>
            @if (org.tin) { <div><dt>TIN</dt><dd>{{ org.tin }}</dd></div> }
            @if (org.region) { <div><dt>Region</dt><dd>{{ org.region }}</dd></div> }
            @if (org.physicalAddress) { <div><dt>Address</dt><dd>{{ org.physicalAddress }}</dd></div> }
          </dl>
        }
      </section>
    }
  `,
  styles: [`
    .page-head { margin-bottom: 1.4rem; }
    .page-head h1 { font-size: 2rem; margin-bottom: 0.2rem; }
    .page-head p { margin: 0; font-size: 0.95rem; }
    .panel { padding: 1.4rem 1.5rem; margin-bottom: 1.2rem; }
    .who { display: flex; align-items: center; gap: 0.9rem; margin-bottom: 1.2rem; }
    .who h2 { font-size: 1.4rem; margin-bottom: 0.15rem; }
    .who p { margin: 0; }
    .avatar {
      width: 52px; height: 52px; border-radius: 50%;
      background: var(--amber-500); color: var(--teal-900);
      display: flex; align-items: center; justify-content: center;
      font-weight: 700; font-size: 1rem; flex-shrink: 0;
    }
    h3 { font-size: 1.2rem; margin-bottom: 0.8rem; }
    dl { display: grid; grid-template-columns: 1fr 1fr; gap: 0.9rem 1.4rem; margin: 0 0 1.2rem; }
    dl div { min-width: 0; }
    dt { font-size: 0.75rem; font-weight: 700; letter-spacing: 0.04em; text-transform: uppercase; color: var(--ink-faint); margin-bottom: 0.2rem; }
    dd { margin: 0; color: var(--ink); }
    @media (max-width: 640px) { dl { grid-template-columns: 1fr; } }
  `]
})
export class ProfileComponent implements OnInit {
  protected auth = inject(AuthService);
  private clients = inject(ClientApi);

  protected loading = signal(false);
  protected error = signal<string | null>(null);
  protected client = signal<ClientResponse | null>(null);

  ngOnInit(): void {
    if (!this.isClient()) return;
    this.loading.set(true);
    this.clients.me().subscribe({
      next: (res) => {
        this.client.set(res.data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('The organisation profile could not be loaded.');
      }
    });
  }

  isClient(): boolean {
    return this.auth.hasAnyRole('CLIENT');
  }

  initials(): string {
    const name = this.auth.displayName().trim();
    if (!name) return '–';
    const parts = name.split(/\s+/);
    return (parts[0][0] + (parts.length > 1 ? parts[parts.length - 1][0] : '')).toUpperCase();
  }

  roleLabel(): string {
    const roles = this.auth.roles().map(r => r.replace(/^ROLE_/, '').toLowerCase());
    if (roles.length === 0) return '—';
    return roles.map(r => r.charAt(0).toUpperCase() + r.slice(1)).join(', ');
  }

  label(value: string): string {
    return value.toLowerCase().replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
  }
}
