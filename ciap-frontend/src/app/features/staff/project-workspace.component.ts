import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ProjectApi } from '../../core/api/project.api';
import { AuthService } from '../../core/auth/auth.service';
import {
  ProjectResponse, ProjectStatus, TaskStatus, TaskResponse
} from '../../core/models/api.models';

@Component({
  selector: 'app-project-workspace',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    @if (loading()) {
      <div class="card load-card">Loading project…</div>
    } @else if (loadError()) {
      <div class="alert alert-error">{{ loadError() }}</div>
    } @else if (project(); as p) {
      <a [routerLink]="['/staff/requests', p.requestId]" class="back">← Back to request {{ p.requestReference }}</a>

      <header class="detail-head">
        <div>
          <div class="ref-line">{{ p.projectCode }}</div>
          <h1>{{ p.name }}</h1>
        </div>
        <span class="pill big" [class]="projPill(p.status)">{{ projLabel(p.status) }}</span>
      </header>

      @if (msg()) { <div class="alert alert-ok">{{ msg() }}</div> }
      @if (err()) { <div class="alert alert-error">{{ err() }}</div> }

      <!-- Progress -->
      <section class="card panel">
        <div class="prog-head">
          <h3>Progress</h3>
          <span class="prog-pct">{{ p.progressPercent }}%</span>
        </div>
        <div class="bar"><div class="fill" [style.width.%]="p.progressPercent"></div></div>
        <dl class="meta">
          <dt>Lead consultant</dt><dd>{{ p.leadConsultantName || '—' }}</dd>
          <dt>Planned</dt><dd>{{ p.plannedStartDate || '—' }} → {{ p.plannedEndDate || '—' }}</dd>
        </dl>

        @if (canManage()) {
          <div class="proj-actions">
            @if (p.status === 'PLANNING') {
              <button class="btn btn-primary sm" [disabled]="busy()" (click)="act('start')">Start project</button>
            }
            @if (p.status === 'ACTIVE') {
              <button class="btn btn-ghost sm" [disabled]="busy()" (click)="act('hold')">Put on hold</button>
              <button class="btn btn-primary sm" [disabled]="busy()" (click)="act('complete')">Mark complete</button>
            }
            @if (p.status === 'ON_HOLD') {
              <button class="btn btn-primary sm" [disabled]="busy()" (click)="act('start')">Resume</button>
            }
            @if (p.status !== 'COMPLETED' && p.status !== 'CANCELLED') {
              <button class="btn btn-danger sm" [disabled]="busy()" (click)="act('cancel')">Cancel</button>
            }
          </div>
        }
      </section>

      <!-- Tasks -->
      <section class="card panel">
        <h3>Tasks</h3>
        @if (p.tasks.length === 0) {
          <p class="muted small">No tasks yet.</p>
        } @else {
          <ul class="task-list">
            @for (t of p.tasks; track t.id) {
              <li>
                <div class="t-info">
                  <span class="t-title" [class.done]="t.status === 'DONE'">{{ t.title }}</span>
                  @if (t.assigneeName) { <span class="t-assignee">{{ t.assigneeName }}</span> }
                  @if (t.dueDate) { <span class="t-due">due {{ t.dueDate }}</span> }
                </div>
                @if (canWork()) {
                  <select class="t-status" [ngModel]="t.status" (ngModelChange)="changeTaskStatus(t, $event)">
                    @for (s of taskStatuses; track s) { <option [value]="s">{{ taskLabel(s) }}</option> }
                  </select>
                } @else {
                  <span class="pill" [class]="taskPill(t.status)">{{ taskLabel(t.status) }}</span>
                }
              </li>
            }
          </ul>
        }

        @if (canWork() && active(p.status)) {
          <div class="add-row">
            <input [(ngModel)]="newTaskTitle" placeholder="New task title" />
            <input type="date" [(ngModel)]="newTaskDue" />
            <button class="btn btn-ghost sm" [disabled]="busy() || !newTaskTitle.trim()" (click)="addTask()">+ Add</button>
          </div>
        }
      </section>

      <!-- Milestones -->
      <section class="card panel">
        <h3>Milestones</h3>
        @if (p.milestones.length === 0) {
          <p class="muted small">No milestones yet.</p>
        } @else {
          <ul class="ms-list">
            @for (m of p.milestones; track m.id) {
              <li>
                <div class="ms-info">
                  <span class="ms-check" [class.on]="m.achieved">{{ m.achieved ? '✓' : '○' }}</span>
                  <span [class.done]="m.achieved">{{ m.title }}</span>
                  @if (m.dueDate) { <span class="ms-due">{{ m.dueDate }}</span> }
                </div>
                @if (canWork() && !m.achieved && active(p.status)) {
                  <button class="btn btn-ghost sm" [disabled]="busy()" (click)="achieve(m.id)">Mark achieved</button>
                }
              </li>
            }
          </ul>
        }

        @if (canWork() && active(p.status)) {
          <div class="add-row">
            <input [(ngModel)]="newMsTitle" placeholder="New milestone title" />
            <input type="date" [(ngModel)]="newMsDue" />
            <button class="btn btn-ghost sm" [disabled]="busy() || !newMsTitle.trim()" (click)="addMilestone()">+ Add</button>
          </div>
        }
      </section>
    }
  `,
  styles: [`
    .back { display: inline-block; margin-bottom: 1.2rem; font-size: 0.9rem; }
    .load-card { padding: 2rem; color: var(--ink-faint); }
    .detail-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; margin-bottom: 1.4rem; }
    .ref-line { font-size: 0.82rem; font-weight: 600; color: var(--ink-faint); margin-bottom: 0.3rem; }
    .detail-head h1 { font-size: 1.8rem; }
    .pill.big { font-size: 0.78rem; padding: 0.35rem 0.8rem; }

    .panel { padding: 1.3rem 1.5rem; margin-bottom: 1.2rem; max-width: 760px; }
    .panel h3 { font-size: 1.1rem; margin-bottom: 0.9rem; }

    .prog-head { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 0.5rem; }
    .prog-pct { font-family: var(--font-display); font-size: 1.4rem; font-weight: 600; color: var(--teal-700); }
    .bar { height: 8px; background: var(--paper-2); border-radius: 100px; overflow: hidden; margin-bottom: 1rem; }
    .fill { height: 100%; background: var(--teal-600); border-radius: 100px; transition: width 0.3s ease; }

    .meta { display: grid; grid-template-columns: 140px 1fr; gap: 0.5rem 1rem; margin: 0 0 1rem; }
    .meta dt { color: var(--ink-faint); font-size: 0.86rem; }
    .meta dd { margin: 0; font-size: 0.92rem; font-weight: 500; }

    .proj-actions { display: flex; gap: 0.6rem; flex-wrap: wrap; padding-top: 0.6rem; border-top: 1px solid var(--line); }
    .btn.sm { padding: 0.45rem 0.8rem; font-size: 0.85rem; }
    .btn-danger { background: var(--danger-bg); color: var(--danger); }
    .btn-danger:hover:not(:disabled) { background: #f0d4cf; }

    .small { font-size: 0.88rem; }

    .task-list, .ms-list { list-style: none; margin: 0 0 0.8rem; padding: 0; }
    .task-list li, .ms-list li { display: flex; justify-content: space-between; align-items: center; gap: 1rem; padding: 0.6rem 0; border-bottom: 1px solid var(--line); }
    .task-list li:last-child, .ms-list li:last-child { border-bottom: none; }
    .t-info, .ms-info { display: flex; align-items: center; gap: 0.7rem; flex-wrap: wrap; }
    .t-title { font-weight: 500; }
    .t-title.done, .done { text-decoration: line-through; color: var(--ink-faint); }
    .t-assignee { font-size: 0.8rem; color: var(--teal-700); background: var(--teal-50); padding: 0.1rem 0.5rem; border-radius: 100px; }
    .t-due, .ms-due { font-size: 0.8rem; color: var(--ink-faint); }
    .t-status { padding: 0.4rem 0.6rem; border: 1px solid var(--line-2); border-radius: var(--radius-sm); font-family: var(--font-body); font-size: 0.85rem; }
    .ms-check { color: var(--ink-faint); font-weight: 700; }
    .ms-check.on { color: var(--teal-600); }

    .add-row { display: flex; gap: 0.5rem; padding-top: 0.8rem; border-top: 1px solid var(--line); flex-wrap: wrap; }
    .add-row input { padding: 0.5rem 0.6rem; border: 1px solid var(--line-2); border-radius: var(--radius-sm); font-family: var(--font-body); font-size: 0.88rem; }
    .add-row input:first-child { flex: 1; min-width: 160px; }
  `]
})
export class ProjectWorkspaceComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private api = inject(ProjectApi);
  private auth = inject(AuthService);

  protected loading = signal(true);
  protected loadError = signal<string | null>(null);
  protected busy = signal(false);
  protected msg = signal<string | null>(null);
  protected err = signal<string | null>(null);
  protected project = signal<ProjectResponse | null>(null);

  protected readonly taskStatuses: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'BLOCKED', 'DONE'];

  protected newTaskTitle = '';
  protected newTaskDue = '';
  protected newMsTitle = '';
  protected newMsDue = '';

  private id!: number;

  ngOnInit(): void {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.api.get(this.id).subscribe({
      next: (res) => { this.project.set(res.data); this.loading.set(false); },
      error: (e) => {
        this.loadError.set(e?.status === 404 ? 'Project not found.' : 'Could not load the project.');
        this.loading.set(false);
      }
    });
  }

  canManage(): boolean { return this.auth.hasAnyRole('MANAGER', 'ADMIN'); }
  canWork(): boolean { return this.auth.hasAnyRole('MANAGER', 'ADMIN', 'EXPERT'); }
  active(s: ProjectStatus): boolean { return s === 'ACTIVE' || s === 'PLANNING'; }

  act(which: 'start' | 'hold' | 'complete' | 'cancel'): void {
    this.busy.set(true); this.clear();
    const call = which === 'start' ? this.api.start(this.id)
      : which === 'hold' ? this.api.hold(this.id)
      : which === 'complete' ? this.api.complete(this.id)
      : this.api.cancel(this.id);
    call.subscribe({
      next: (res) => { this.project.set(res.data); this.busy.set(false); this.msg.set('Project updated.'); },
      error: (e) => { this.err.set(this.errText(e)); this.busy.set(false); }
    });
  }

  changeTaskStatus(t: TaskResponse, status: TaskStatus): void {
    if (status === t.status) return;
    this.busy.set(true); this.clear();
    this.api.updateTaskStatus(this.id, t.id, status).subscribe({
      next: (res) => { this.project.set(res.data); this.busy.set(false); },
      error: (e) => { this.err.set(this.errText(e)); this.busy.set(false); this.load(); }
    });
  }

  addTask(): void {
    if (!this.newTaskTitle.trim()) return;
    this.busy.set(true); this.clear();
    this.api.addTask(this.id, { title: this.newTaskTitle.trim(), dueDate: this.newTaskDue || undefined })
      .subscribe({
        next: () => { this.newTaskTitle = ''; this.newTaskDue = ''; this.busy.set(false); this.load(); },
        error: (e) => { this.err.set(this.errText(e)); this.busy.set(false); }
      });
  }

  addMilestone(): void {
    if (!this.newMsTitle.trim()) return;
    this.busy.set(true); this.clear();
    this.api.addMilestone(this.id, { title: this.newMsTitle.trim(), dueDate: this.newMsDue || undefined })
      .subscribe({
        next: () => { this.newMsTitle = ''; this.newMsDue = ''; this.busy.set(false); this.load(); },
        error: (e) => { this.err.set(this.errText(e)); this.busy.set(false); }
      });
  }

  achieve(milestoneId: number): void {
    this.busy.set(true); this.clear();
    this.api.achieveMilestone(this.id, milestoneId).subscribe({
      next: () => { this.busy.set(false); this.load(); },
      error: (e) => { this.err.set(this.errText(e)); this.busy.set(false); }
    });
  }

  private errText(e: any): string {
    if (e?.status === 403) return 'Your role is not permitted to perform this action.';
    if (e?.status === 422) return e?.error?.message || 'That action is not allowed in the current state.';
    return e?.error?.message || 'The action failed. Please try again.';
  }
  private clear(): void { this.msg.set(null); this.err.set(null); }

  projLabel(s: ProjectStatus): string { return s.replace(/_/g, ' ').toLowerCase(); }
  projPill(s: ProjectStatus): string {
    if (s === 'COMPLETED') return 'pill-done';
    if (s === 'CANCELLED') return 'pill-danger';
    if (s === 'ON_HOLD') return 'pill-warn';
    if (s === 'PLANNING') return 'pill-neutral';
    return 'pill-active';
  }
  taskLabel(s: TaskStatus): string { return s.replace(/_/g, ' ').toLowerCase(); }
  taskPill(s: TaskStatus): string {
    if (s === 'DONE') return 'pill-done';
    if (s === 'BLOCKED') return 'pill-danger';
    if (s === 'IN_PROGRESS') return 'pill-active';
    return 'pill-neutral';
  }
}
