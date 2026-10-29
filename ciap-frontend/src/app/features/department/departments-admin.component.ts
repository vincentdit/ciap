import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DepartmentApi } from '../../core/api/department.api';
import { RequestApi } from '../../core/api/request.api';
import { UserApi, UserAccount } from '../../core/api/user.api';
import { DepartmentSummary, RequestResponse } from '../../core/models/api.models';

@Component({
  selector: 'app-departments-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <header class="page-head">
      <div>
        <h1>HOD management</h1>
        <p class="muted">Each service line has one Head of Department. Requests that cannot be routed stay here until you choose a department.</p>
      </div>
    </header>

    @if (msg()) { <div class="alert alert-ok">{{ msg() }}</div> }
    @if (err()) { <div class="alert alert-error">{{ err() }}</div> }

    @if (loading()) {
      <div class="card panel">Loading departments…</div>
    } @else {
      <section class="card panel">
        <h2>Heads of department</h2>
        <table>
          <thead>
            <tr><th>Department</th><th>Head</th><th>Appoint</th></tr>
          </thead>
          <tbody>
            @for (dept of departments(); track dept.id) {
              <tr>
                <td>{{ dept.name }}</td>
                <td>{{ dept.hodName || 'None' }}</td>
                <td>
                  <select [ngModel]="choice(dept.id)" (ngModelChange)="setChoice(dept.id, $event)">
                    <option [ngValue]="null">Select a Head of Department…</option>
                    @for (user of hodUsers(); track user.id) {
                      <option [ngValue]="user.id">{{ user.fullName }} — {{ user.email }}</option>
                    }
                  </select>
                  <button type="button" class="btn btn-primary sm" [disabled]="busy() || !choice(dept.id)"
                          (click)="appoint(dept)">Appoint</button>
                </td>
              </tr>
            }
          </tbody>
        </table>
        @if (hodUsers().length === 0) {
          <p class="muted">Create a staff account with the Head of Department role before appointing one.</p>
        }
      </section>

      <section class="card panel">
        <h2>Needs a route</h2>
        @if (unrouted().length === 0) {
          <p class="muted">Every open request is attached to a department.</p>
        } @else {
          <ul class="rows">
            @for (row of unrouted(); track row.id) {
              <li>
                <div>
                  <strong>{{ row.referenceNo }}</strong> — {{ row.title }}
                  <span class="pill pill-warn">{{ row.routingStatus }}</span>
                </div>
                <div class="route-row">
                  <select [(ngModel)]="routeChoice[row.id]">
                    <option [ngValue]="null">Choose department…</option>
                    @for (dept of departments(); track dept.id) {
                      <option [ngValue]="dept.id">{{ dept.name }}</option>
                    }
                  </select>
                  <button type="button" class="btn btn-ghost sm" [disabled]="busy() || !routeChoice[row.id]"
                          (click)="route(row)">Route</button>
                  <a [routerLink]="['/staff/requests', row.id]">Open</a>
                </div>
              </li>
            }
          </ul>
        }
      </section>
    }
  `,
  styles: [`
    .page-head { margin-bottom: 1.2rem; }
    .panel { margin-bottom: 1rem; }
    .panel h2 { margin-top: 0; }
    table { width: 100%; border-collapse: collapse; }
    th, td { text-align: left; padding: 0.65rem 0.4rem; border-bottom: 1px solid var(--line); vertical-align: middle; }
    select { margin-right: 0.4rem; padding: 0.4rem; }
    .sm { padding: 0.35rem 0.7rem; }
    .rows { list-style: none; padding: 0; margin: 0; }
    .rows li { padding: 0.8rem 0; border-bottom: 1px solid var(--line); }
    .route-row { display: flex; gap: 0.5rem; align-items: center; margin-top: 0.4rem; flex-wrap: wrap; }
  `]
})
export class DepartmentsAdminComponent implements OnInit {
  private departmentsApi = inject(DepartmentApi);
  private requests = inject(RequestApi);
  private users = inject(UserApi);

  protected loading = signal(true);
  protected busy = signal(false);
  protected msg = signal<string | null>(null);
  protected err = signal<string | null>(null);
  protected departments = signal<DepartmentSummary[]>([]);
  protected hodUsers = signal<UserAccount[]>([]);
  protected unrouted = signal<RequestResponse[]>([]);
  protected choices: Record<number, number | null> = {};
  protected routeChoice: Record<number, number | null> = {};

  ngOnInit(): void { this.reload(); }

  choice(id: number): number | null { return this.choices[id] ?? null; }
  setChoice(id: number, userId: number | null): void { this.choices[id] = userId; }

  appoint(dept: DepartmentSummary): void {
    const userId = this.choice(dept.id);
    if (!userId) return;
    this.busy.set(true);
    this.err.set(null);
    this.departmentsApi.appoint(dept.id, userId).subscribe({
      next: () => {
        this.msg.set(dept.name + ' now has a Head of Department.');
        this.busy.set(false);
        this.reload();
      },
      error: (err) => {
        this.err.set(err?.error?.message || 'The appointment was not saved.');
        this.busy.set(false);
      }
    });
  }

  route(row: RequestResponse): void {
    const departmentId = this.routeChoice[row.id];
    if (!departmentId) return;
    this.busy.set(true);
    this.err.set(null);
    this.requests.route(row.id, departmentId).subscribe({
      next: () => {
        this.msg.set(row.referenceNo + ' was routed.');
        this.busy.set(false);
        this.reload();
      },
      error: (err) => {
        this.err.set(err?.error?.message || 'The request was not routed.');
        this.busy.set(false);
      }
    });
  }

  private reload(): void {
    this.loading.set(true);
    this.departmentsApi.list().subscribe({
      next: (res) => {
        this.departments.set(res.data);
        this.loading.set(false);
      },
      error: () => {
        this.err.set('Could not load departments.');
        this.loading.set(false);
      }
    });
    this.users.list(0, 100).subscribe({
      next: (res) => this.hodUsers.set(res.data.content.filter(user => user.enabled && user.roles.includes('HOD'))),
      error: () => this.hodUsers.set([])
    });
    this.requests.list(0, 100).subscribe({
      next: (res) => this.unrouted.set(res.data.content.filter(row =>
        row.routingStatus === 'UNMAPPED' || row.routingStatus === 'NO_HOD')),
      error: () => this.unrouted.set([])
    });
  }
}
