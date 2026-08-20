import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProjectApi } from '../../core/api/project.api';
import { ConsultantApi } from '../../core/api/consultant.api';
import { CreateProjectRequest, ConsultantResponse } from '../../core/models/api.models';

@Component({
  selector: 'app-project-new',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <a [routerLink]="['/staff/requests', requestId]" class="back">← Back to request</a>

    <header class="page-head">
      <h1>Create delivery project</h1>
      <p class="muted">Spin up a project to manage execution of this engagement.</p>
    </header>

    @if (error()) { <div class="alert alert-error">{{ error() }}</div> }

    <div class="card panel">
      <div class="field">
        <label>Project name *</label>
        <input [(ngModel)]="name" placeholder="e.g. Bottling line delivery" [class.invalid]="showErr() && !name.trim()" />
        @if (showErr() && !name.trim()) { <div class="error-text">A name is required.</div> }
      </div>

      <div class="field">
        <label>Description</label>
        <textarea [(ngModel)]="description" placeholder="Scope and objectives"></textarea>
      </div>

      <div class="grid-2">
        <div class="field">
          <label>Planned start</label>
          <input type="date" [(ngModel)]="plannedStartDate" />
        </div>
        <div class="field">
          <label>Planned end</label>
          <input type="date" [(ngModel)]="plannedEndDate" />
        </div>
      </div>

      <div class="field">
        <label>Lead consultant</label>
        <select [(ngModel)]="leadConsultantId">
          <option [ngValue]="null">— None / decide later —</option>
          @for (c of consultants(); track c.id) {
            <option [ngValue]="c.id">{{ c.fullName }}</option>
          }
        </select>
        <div class="hint">Defaults to the request's assigned consultant if left blank on the server.</div>
      </div>

      <button class="btn btn-primary" [disabled]="busy()" (click)="submit()">
        @if (busy()) { <span class="spinner"></span> Creating… } @else { Create project }
      </button>
    </div>
  `,
  styles: [`
    .back { display: inline-block; margin-bottom: 1.2rem; font-size: 0.9rem; }
    .page-head { margin-bottom: 1.4rem; }
    .page-head h1 { font-size: 1.9rem; margin-bottom: 0.2rem; }
    .page-head p { margin: 0; font-size: 0.92rem; }
    .panel { padding: 1.5rem 1.7rem; max-width: 600px; }
  `]
})
export class ProjectNewComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private api = inject(ProjectApi);
  private consultantApi = inject(ConsultantApi);

  protected requestId!: number;
  protected name = '';
  protected description = '';
  protected plannedStartDate = '';
  protected plannedEndDate = '';
  protected leadConsultantId: number | null = null;

  protected consultants = signal<ConsultantResponse[]>([]);
  protected busy = signal(false);
  protected error = signal<string | null>(null);
  protected showErr = signal(false);

  ngOnInit(): void {
    this.requestId = Number(this.route.snapshot.paramMap.get('id'));
    this.consultantApi.list(0, 100).subscribe({
      next: (res) => this.consultants.set(res.data.content),
      error: () => { /* picker is optional; ignore */ }
    });
  }

  submit(): void {
    this.showErr.set(true);
    if (!this.name.trim()) return;
    this.busy.set(true);
    this.error.set(null);
    const body: CreateProjectRequest = {
      name: this.name.trim(),
      description: this.description || undefined,
      plannedStartDate: this.plannedStartDate || undefined,
      plannedEndDate: this.plannedEndDate || undefined,
      leadConsultantId: this.leadConsultantId ?? undefined
    };
    this.api.createForRequest(this.requestId, body).subscribe({
      next: (res) => { this.router.navigate(['/staff/projects', res.data.id]); },
      error: (e) => {
        this.busy.set(false);
        if (e?.status === 422) this.error.set(e?.error?.message || 'A project cannot be created from this request yet.');
        else if (e?.status === 409) this.error.set('A project already exists for this request.');
        else if (e?.status === 403) this.error.set('Only managers can create projects.');
        else this.error.set('Could not create the project. Please try again.');
      }
    });
  }
}
