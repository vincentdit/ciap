import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ConsultantApi, CreateConsultantRequest } from '../../core/api/consultant.api';
import {
  ConsultantResponse, AvailabilityStatus, ExpertCategory, EXPERT_CATEGORIES
} from '../../core/models/api.models';

@Component({
  selector: 'app-consultants',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <header class="page-head">
      <div>
        <h1>Consultants</h1>
        <p class="muted">Expert roster and availability.</p>
      </div>
      <button class="btn btn-primary" (click)="showForm.set(!showForm())">
        {{ showForm() ? 'Close' : '+ New consultant' }}
      </button>
    </header>

    @if (msg()) { <div class="alert alert-ok">{{ msg() }}</div> }
    @if (err()) { <div class="alert alert-error">{{ err() }}</div> }

    @if (showForm()) {
      <div class="card panel form-card">
        <h3>Register consultant</h3>
        <div class="grid-2">
          <div class="field">
            <label>Full name *</label>
            <input [(ngModel)]="f.fullName" placeholder="Dr. Asha Kimaro" />
          </div>
          <div class="field">
            <label>Category *</label>
            <select [(ngModel)]="f.category">
              @for (c of categories; track c.value) { <option [value]="c.value">{{ c.label }}</option> }
            </select>
          </div>
        </div>
        <div class="field">
          <label>Skills</label>
          <input [(ngModel)]="f.skills" placeholder="e.g. process optimisation, ISO auditing" />
        </div>
        <div class="grid-2">
          <div class="field">
            <label>Years of experience</label>
            <input type="number" min="0" [(ngModel)]="f.yearsExperience" />
          </div>
          <div class="field">
            <label>Billing rate / hour (TZS)</label>
            <input type="number" min="0" step="1000" [(ngModel)]="f.billingRatePerHour" />
          </div>
        </div>
        <button class="btn btn-primary" [disabled]="busy() || !f.fullName.trim()" (click)="create()">
          @if (busy()) { <span class="spinner"></span> Saving… } @else { Save consultant }
        </button>
      </div>
    }

    @if (loading()) {
      <div class="card load-card">Loading consultants…</div>
    } @else if (rows().length === 0) {
      <div class="card empty"><p>No consultants registered yet.</p></div>
    } @else {
      <div class="card table-card">
        <table>
          <thead>
            <tr><th>Name</th><th>Category</th><th>Experience</th><th>Availability</th><th class="num">Active</th></tr>
          </thead>
          <tbody>
            @for (c of rows(); track c.id) {
              <tr>
                <td class="name">{{ c.fullName }}</td>
                <td>{{ catLabel(c.category) }}</td>
                <td>{{ c.yearsExperience ?? '—' }} {{ c.yearsExperience != null ? 'yrs' : '' }}</td>
                <td><span class="pill" [class]="availPill(c.availabilityStatus)">{{ availLabel(c.availabilityStatus) }}</span></td>
                <td class="num">{{ c.activeAssignments }}</td>
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
    .page-head p { margin: 0; font-size: 0.95rem; }
    .panel { padding: 1.4rem 1.6rem; margin-bottom: 1.2rem; }
    .form-card { max-width: 640px; }
    .panel h3 { font-size: 1.1rem; margin-bottom: 0.9rem; }
    .load-card { padding: 2rem; color: var(--ink-faint); }
    .empty { padding: 2.5rem; text-align: center; }
    .empty p { color: var(--ink-faint); margin: 0; }
    .table-card { padding: 0; overflow: hidden; }
    table { width: 100%; border-collapse: collapse; }
    thead th { text-align: left; font-size: 0.76rem; font-weight: 700; letter-spacing: 0.04em; text-transform: uppercase; color: var(--ink-faint); padding: 0.9rem 1.2rem; border-bottom: 1px solid var(--line); background: var(--paper-2); }
    th.num, td.num { text-align: right; }
    tbody td { padding: 0.9rem 1.2rem; border-bottom: 1px solid var(--line); font-size: 0.92rem; }
    tbody tr:last-child td { border-bottom: none; }
    tbody tr:hover { background: var(--teal-50); }
    .name { font-weight: 500; }
  `]
})
export class ConsultantsComponent implements OnInit {
  private api = inject(ConsultantApi);

  protected rows = signal<ConsultantResponse[]>([]);
  protected loading = signal(true);
  protected busy = signal(false);
  protected showForm = signal(false);
  protected msg = signal<string | null>(null);
  protected err = signal<string | null>(null);

  protected categories = EXPERT_CATEGORIES;
  protected f: CreateConsultantRequest = {
    fullName: '', category: 'ENGINEER', skills: '', yearsExperience: undefined, billingRatePerHour: undefined
  };

  ngOnInit(): void { this.load(); }

  private load(): void {
    this.loading.set(true);
    this.api.list(0, 200).subscribe({
      next: (res) => { this.rows.set(res.data.content); this.loading.set(false); },
      error: (e) => {
        this.loading.set(false);
        this.err.set(e?.status === 403 ? 'Your role cannot view consultants.' : 'Could not load consultants.');
      }
    });
  }

  create(): void {
    if (!this.f.fullName.trim()) return;
    this.busy.set(true);
    this.clear();
    const body: CreateConsultantRequest = {
      fullName: this.f.fullName.trim(),
      category: this.f.category,
      skills: this.f.skills || undefined,
      yearsExperience: this.f.yearsExperience != null ? Number(this.f.yearsExperience) : undefined,
      billingRatePerHour: this.f.billingRatePerHour != null ? Number(this.f.billingRatePerHour) : undefined
    };
    this.api.create(body).subscribe({
      next: () => {
        this.busy.set(false);
        this.msg.set('Consultant registered.');
        this.f = { fullName: '', category: 'ENGINEER', skills: '', yearsExperience: undefined, billingRatePerHour: undefined };
        this.showForm.set(false);
        this.load();
      },
      error: (e) => {
        this.busy.set(false);
        if (e?.status === 403) this.err.set('Only managers and admins can register consultants.');
        else this.err.set(e?.error?.message || 'Could not register the consultant.');
      }
    });
  }

  catLabel(c: ExpertCategory): string {
    return this.categories.find(x => x.value === c)?.label ?? c;
  }
  availLabel(a: AvailabilityStatus): string { return a.replace(/_/g, ' ').toLowerCase(); }
  availPill(a: AvailabilityStatus): string {
    if (a === 'AVAILABLE') return 'pill-active';
    if (a === 'PARTIALLY_AVAILABLE') return 'pill-warn';
    if (a === 'ON_LEAVE') return 'pill-neutral';
    return 'pill-danger';
  }

  private clear(): void { this.msg.set(null); this.err.set(null); }
}
