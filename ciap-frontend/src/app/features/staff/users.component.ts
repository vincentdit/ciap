import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth/auth.service';
import { CreateStaffRequest, UserAccount, UserApi } from '../../core/api/user.api';

interface RoleOption {
  value: string;
  label: string;
}

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <header class="page-head">
      <div>
        <h1>User management</h1>
        <p class="muted">Assign one role to each account. Only administrators can change access.</p>
      </div>
      <button class="btn btn-primary" (click)="showForm.set(!showForm())">
        {{ showForm() ? 'Close' : '+ New account' }}
      </button>
    </header>

    @if (msg()) { <div class="alert alert-ok">{{ msg() }}</div> }
    @if (err()) { <div class="alert alert-error">{{ err() }}</div> }

    @if (showForm()) {
      <div class="card panel form-card">
        <h3>Create staff account</h3>
        <div class="grid-2">
          <div class="field">
            <label for="fullName">Full name</label>
            <input id="fullName" [(ngModel)]="form.fullName" placeholder="Asha Kimaro" />
          </div>
          <div class="field">
            <label for="email">Email</label>
            <input id="email" type="email" [(ngModel)]="form.email" placeholder="asha@tirdo.go.tz" />
          </div>
        </div>
        <div class="grid-2">
          <div class="field">
            <label for="phone">Phone</label>
            <input id="phone" [(ngModel)]="form.phone" placeholder="+255700000000" />
          </div>
          <div class="field">
            <label for="role">Role</label>
            <select id="role" [(ngModel)]="form.role">
              @for (role of staffRoles; track role.value) {
                <option [value]="role.value">{{ role.label }}</option>
              }
            </select>
          </div>
        </div>
        <div class="field">
          <label for="password">Temporary password</label>
          <input id="password" type="password" [(ngModel)]="form.password" autocomplete="new-password" />
          <div class="hint">At least 8 characters. Share it with the person, then ask them to change it.</div>
        </div>
        <button class="btn btn-primary" [disabled]="busy()" (click)="create()">
          @if (busy()) { <span class="spinner"></span> Saving… } @else { Create account }
        </button>
      </div>
    }

    @if (loading()) {
      <div class="card load-card">Loading accounts…</div>
    } @else if (rows().length === 0) {
      <div class="card empty"><p>No accounts yet.</p></div>
    } @else {
      <div class="card table-card">
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Role</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (user of rows(); track user.id) {
              <tr>
                <td class="name">{{ user.fullName }}</td>
                <td>{{ user.email }}</td>
                <td>
                  <select [name]="'role-' + user.id" [ngModel]="primaryRole(user)"
                          [disabled]="isSelf(user) || savingId() === user.id"
                          (ngModelChange)="changeRole(user, $event)">
                    @for (role of allRoles; track role.value) {
                      <option [value]="role.value">{{ role.label }}</option>
                    }
                  </select>
                </td>
                <td>
                  <span class="pill" [class]="user.enabled ? 'pill-active' : 'pill-danger'">
                    {{ user.enabled ? 'Active' : 'Disabled' }}
                  </span>
                </td>
                <td class="actions">
                  @if (!isSelf(user)) {
                    <button class="btn btn-ghost sm" [disabled]="savingId() === user.id"
                            (click)="toggleEnabled(user)">
                      {{ user.enabled ? 'Disable' : 'Enable' }}
                    </button>
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }
  `,
  styles: [`
    .page-head { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 1.4rem; gap: 1rem; }
    .page-head h1 { font-size: 2rem; margin-bottom: 0.2rem; }
    .page-head p { margin: 0; font-size: 0.95rem; max-width: 520px; }
    .panel { padding: 1.4rem 1.6rem; margin-bottom: 1.2rem; }
    .form-card { max-width: 720px; }
    .panel h3 { font-size: 1.1rem; margin-bottom: 0.9rem; }
    .load-card { padding: 2rem; color: var(--ink-faint); }
    .empty { padding: 2.5rem; text-align: center; }
    .empty p { color: var(--ink-faint); margin: 0; }
    .table-card { padding: 0; overflow: auto; }
    table { width: 100%; border-collapse: collapse; }
    thead th { text-align: left; font-size: 0.76rem; font-weight: 700; letter-spacing: 0.04em; text-transform: uppercase; color: var(--ink-faint); padding: 0.9rem 1.2rem; border-bottom: 1px solid var(--line); background: var(--paper-2); }
    tbody td { padding: 0.75rem 1.2rem; border-bottom: 1px solid var(--line); font-size: 0.92rem; vertical-align: middle; }
    tbody tr:last-child td { border-bottom: none; }
    .name { font-weight: 600; }
    select {
      font-family: var(--font-body);
      font-size: 0.88rem;
      color: var(--ink);
      background: var(--card);
      border: 1px solid var(--line-2);
      border-radius: var(--radius-sm);
      padding: 0.4rem 0.55rem;
    }
    .actions { text-align: right; white-space: nowrap; }
    .sm { padding: 0.35rem 0.7rem; font-size: 0.82rem; }
  `]
})
export class UsersComponent implements OnInit {
  private api = inject(UserApi);
  private auth = inject(AuthService);

  protected readonly staffRoles: RoleOption[] = [
    { value: 'MANAGER', label: 'Manager' },
    { value: 'EXPERT', label: 'Expert' },
    { value: 'FINANCE', label: 'Finance' },
    { value: 'ADMIN', label: 'Administrator' }
  ];
  protected readonly allRoles: RoleOption[] = [
    ...this.staffRoles,
    { value: 'CLIENT', label: 'Client' }
  ];

  protected rows = signal<UserAccount[]>([]);
  protected loading = signal(true);
  protected busy = signal(false);
  protected savingId = signal<number | null>(null);
  protected showForm = signal(false);
  protected msg = signal<string | null>(null);
  protected err = signal<string | null>(null);

  protected form = { fullName: '', email: '', phone: '', password: '', role: 'MANAGER' };

  ngOnInit(): void { this.load(); }

  isSelf(user: UserAccount): boolean {
    return user.email.toLowerCase() === this.auth.email().toLowerCase();
  }

  primaryRole(user: UserAccount): string {
    const role = user.roles[0] ?? '';
    return role.replace(/^ROLE_/, '');
  }

  changeRole(user: UserAccount, role: string): void {
    if (!role || role === this.primaryRole(user)) return;
    this.clear();
    this.savingId.set(user.id);
    this.api.updateRole(user.id, role).subscribe({
      next: () => {
        this.savingId.set(null);
        this.msg.set(`${user.fullName} is now ${this.label(role)}. They must sign in again.`);
        this.load();
      },
      error: (e) => {
        this.savingId.set(null);
        this.err.set(e?.error?.message || 'The role could not be changed.');
        this.load();
      }
    });
  }

  toggleEnabled(user: UserAccount): void {
    this.clear();
    this.savingId.set(user.id);
    this.api.setEnabled(user.id, !user.enabled).subscribe({
      next: () => {
        this.savingId.set(null);
        this.msg.set(user.enabled
          ? `${user.fullName} can no longer sign in.`
          : `${user.fullName} can sign in again.`);
        this.load();
      },
      error: (e) => {
        this.savingId.set(null);
        this.err.set(e?.error?.message || 'The account status could not be changed.');
      }
    });
  }

  create(): void {
    this.clear();
    const fullName = this.form.fullName.trim();
    const email = this.form.email.trim();
    if (!fullName || !email || this.form.password.length < 8) {
      this.err.set('Enter a name, email, and a password of at least 8 characters.');
      return;
    }
    const body: CreateStaffRequest = {
      email,
      password: this.form.password,
      fullName,
      phone: this.form.phone.trim() || undefined,
      roles: [this.form.role]
    };
    this.busy.set(true);
    this.api.createStaff(body).subscribe({
      next: () => {
        this.busy.set(false);
        this.msg.set('Account created. Ask them to sign in and change the temporary password.');
        this.form = { fullName: '', email: '', phone: '', password: '', role: 'MANAGER' };
        this.showForm.set(false);
        this.load();
      },
      error: (e) => {
        this.busy.set(false);
        if (e?.status === 403) this.err.set('Only an administrator can create accounts.');
        else if (e?.status === 409) this.err.set('An account with that email already exists.');
        else this.err.set(e?.error?.message || 'The account could not be created.');
      }
    });
  }

  private load(): void {
    this.loading.set(true);
    this.api.list().subscribe({
      next: (res) => {
        this.rows.set(res.data.content);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.err.set(e?.status === 403
          ? 'Only an administrator can manage users.'
          : 'Accounts could not be loaded.');
      }
    });
  }

  private label(role: string): string {
    return this.allRoles.find(item => item.value === role)?.label ?? role;
  }

  private clear(): void {
    this.msg.set(null);
    this.err.set(null);
  }
}
