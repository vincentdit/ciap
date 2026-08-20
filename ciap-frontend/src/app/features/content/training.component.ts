import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TrainingApi } from '../../core/api/training.api';
import { AuthService } from '../../core/auth/auth.service';
import {
  EventResponse, CreateEventRequest, TrainingMode, TRAINING_MODES
} from '../../core/models/api.models';

@Component({
  selector: 'app-training',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <header class="page-head">
      <div>
        <h1>Training &amp; webinars</h1>
        <p class="muted">Upcoming sessions offered by TIRDO.</p>
      </div>
      @if (canManage()) {
        <button class="btn btn-primary" (click)="toggleForm()">{{ showForm() ? 'Close' : '+ New event' }}</button>
      }
    </header>

    @if (msg()) { <div class="alert alert-ok">{{ msg() }}</div> }
    @if (err()) { <div class="alert alert-error">{{ err() }}</div> }

    @if (showForm()) {
      <div class="card panel form-card">
        <h3>Schedule an event</h3>
        <div class="field"><label>Title *</label><input [(ngModel)]="f.title" /></div>
        <div class="field"><label>Description</label><textarea [(ngModel)]="f.description" rows="3"></textarea></div>
        <div class="grid-2">
          <div class="field"><label>Mode *</label>
            <select [(ngModel)]="f.mode">
              @for (m of modes; track m.value) { <option [value]="m.value">{{ m.label }}</option> }
            </select>
          </div>
          <div class="field"><label>Capacity (0 = unlimited)</label><input type="number" min="0" [(ngModel)]="f.capacity" /></div>
        </div>
        <div class="field"><label>Location / joining link</label><input [(ngModel)]="f.location" /></div>
        <div class="grid-2">
          <div class="field"><label>Starts *</label><input type="datetime-local" [(ngModel)]="startsLocal" /></div>
          <div class="field"><label>Ends</label><input type="datetime-local" [(ngModel)]="endsLocal" /></div>
        </div>
        <div class="field"><label>Publish now?</label>
          <select [(ngModel)]="f.published">
            <option [ngValue]="false">Save as draft</option>
            <option [ngValue]="true">Publish</option>
          </select>
        </div>
        <button class="btn btn-primary" [disabled]="busy() || !f.title.trim() || !startsLocal" (click)="create()">
          @if (busy()) { <span class="spinner"></span> Saving… } @else { Save event }
        </button>
      </div>
    }

    @if (loading()) {
      <div class="card load-card">Loading events…</div>
    } @else if (events().length === 0) {
      <div class="card empty"><p>No events scheduled.</p></div>
    } @else {
      <div class="event-list">
        @for (e of events(); track e.id) {
          <div class="card ev-card">
            <div class="ev-main">
              <div class="ev-head">
                <span class="pill pill-active">{{ modeLabel(e.mode) }}</span>
                @if (!e.published) { <span class="pill pill-warn">Draft</span> }
              </div>
              <h3>{{ e.title }}</h3>
              @if (e.description) { <p class="ev-desc">{{ e.description }}</p> }
              <div class="ev-meta">
                <span>{{ e.startsAt | date:'medium' }}</span>
                @if (e.location) { <span>· {{ e.location }}</span> }
                <span>· {{ e.registeredCount }}{{ e.capacity > 0 ? '/' + e.capacity : '' }} registered</span>
              </div>
            </div>
            <div class="ev-action">
              @if (e.registered) {
                <span class="reg-tag">✓ Registered</span>
                <button class="btn btn-ghost sm" [disabled]="busyId() === e.id" (click)="cancel(e)">Cancel</button>
              } @else {
                <button class="btn btn-primary sm" [disabled]="busyId() === e.id || isFull(e)" (click)="register(e)">
                  {{ isFull(e) ? 'Full' : 'Register' }}
                </button>
              }
              @if (canManage() && !e.published) {
                <button class="btn btn-ghost sm" (click)="publish(e)">Publish</button>
              }
            </div>
          </div>
        }
      </div>
    }
  `,
  styles: [`
    .page-head { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 1.4rem; gap: 1rem; }
    .page-head h1 { font-size: 2rem; margin-bottom: 0.2rem; }
    .page-head p { margin: 0; font-size: 0.95rem; }
    .panel { padding: 1.4rem 1.6rem; margin-bottom: 1.2rem; }
    .form-card { max-width: 680px; }
    .panel h3 { font-size: 1.1rem; margin-bottom: 0.9rem; }
    .load-card { padding: 2rem; color: var(--ink-faint); }
    .empty { padding: 2.5rem; text-align: center; }
    .empty p { color: var(--ink-faint); margin: 0; }
    .event-list { display: flex; flex-direction: column; gap: 1rem; }
    .ev-card { padding: 1.3rem 1.5rem; display: flex; justify-content: space-between; gap: 1.5rem; align-items: center; }
    .ev-head { display: flex; gap: 0.4rem; margin-bottom: 0.5rem; }
    .ev-card h3 { font-size: 1.15rem; margin-bottom: 0.35rem; }
    .ev-desc { font-size: 0.9rem; color: var(--ink-soft); line-height: 1.5; margin: 0 0 0.6rem; }
    .ev-meta { font-size: 0.84rem; color: var(--ink-faint); display: flex; gap: 0.4rem; flex-wrap: wrap; }
    .ev-action { display: flex; flex-direction: column; gap: 0.5rem; align-items: flex-end; flex-shrink: 0; }
    .btn.sm { padding: 0.45rem 0.85rem; font-size: 0.85rem; }
    .reg-tag { font-size: 0.85rem; color: var(--teal-700); font-weight: 600; }
    @media (max-width: 640px) { .ev-card { flex-direction: column; align-items: stretch; } .ev-action { align-items: stretch; } }
  `]
})
export class TrainingComponent implements OnInit {
  private api = inject(TrainingApi);
  private auth = inject(AuthService);

  protected events = signal<EventResponse[]>([]);
  protected loading = signal(true);
  protected busy = signal(false);
  protected busyId = signal<number | null>(null);
  protected showForm = signal(false);
  protected msg = signal<string | null>(null);
  protected err = signal<string | null>(null);

  protected modes = TRAINING_MODES;
  protected f: CreateEventRequest = { title: '', description: '', mode: 'ONLINE', location: '', startsAt: '', capacity: 0, published: false };
  protected startsLocal = '';
  protected endsLocal = '';

  ngOnInit(): void { this.load(); }

  canManage(): boolean { return this.auth.hasAnyRole('ADMIN', 'MANAGER'); }
  toggleForm(): void { this.showForm.set(!this.showForm()); }
  isFull(e: EventResponse): boolean { return e.capacity > 0 && e.registeredCount >= e.capacity; }

  private load(): void {
    this.loading.set(true);
    const src = this.canManage() ? this.api.listAll() : this.api.listPublished();
    src.subscribe({
      next: (res) => { this.events.set(res.data.content); this.loading.set(false); },
      error: () => { this.err.set('Could not load events.'); this.loading.set(false); }
    });
  }

  register(e: EventResponse): void {
    this.busyId.set(e.id);
    this.clear();
    this.api.register(e.id).subscribe({
      next: () => { this.busyId.set(null); this.msg.set('Registered for ' + e.title + '.'); this.load(); },
      error: (er) => {
        this.busyId.set(null);
        this.err.set(er?.status === 422 ? (er?.error?.message || 'This event is full.')
          : er?.status === 409 ? 'You are already registered.' : 'Could not register.');
      }
    });
  }

  cancel(e: EventResponse): void {
    this.busyId.set(e.id);
    this.clear();
    this.api.cancel(e.id).subscribe({
      next: () => { this.busyId.set(null); this.msg.set('Registration cancelled.'); this.load(); },
      error: () => { this.busyId.set(null); this.err.set('Could not cancel.'); }
    });
  }

  publish(e: EventResponse): void {
    this.api.publish(e.id).subscribe({
      next: () => { this.msg.set('Event published.'); this.load(); },
      error: () => this.err.set('Could not publish.')
    });
  }

  create(): void {
    if (!this.f.title.trim() || !this.startsLocal) return;
    this.busy.set(true);
    this.clear();
    const body: CreateEventRequest = {
      title: this.f.title.trim(),
      description: this.f.description || undefined,
      mode: this.f.mode,
      location: this.f.location || undefined,
      startsAt: new Date(this.startsLocal).toISOString(),
      endsAt: this.endsLocal ? new Date(this.endsLocal).toISOString() : undefined,
      capacity: this.f.capacity != null ? Number(this.f.capacity) : 0,
      published: this.f.published
    };
    this.api.create(body).subscribe({
      next: () => {
        this.busy.set(false);
        this.msg.set('Event saved.');
        this.f = { title: '', description: '', mode: 'ONLINE', location: '', startsAt: '', capacity: 0, published: false };
        this.startsLocal = ''; this.endsLocal = '';
        this.showForm.set(false);
        this.load();
      },
      error: (e) => { this.busy.set(false); this.err.set(e?.error?.message || 'Could not save the event.'); }
    });
  }

  modeLabel(m: TrainingMode): string {
    return this.modes.find(x => x.value === m)?.label ?? m;
  }

  private clear(): void { this.msg.set(null); this.err.set(null); }
}
