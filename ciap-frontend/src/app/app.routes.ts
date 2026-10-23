import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { staffGuard } from './core/guards/staff.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./features/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: '',
    loadComponent: () => import('./layout/shell.component').then(m => m.ShellComponent),
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
      {
        path: 'requests/new',
        loadComponent: () => import('./features/requests/new-request.component').then(m => m.NewRequestComponent)
      },
      {
        path: 'requests',
        loadComponent: () => import('./features/requests/request-list.component').then(m => m.RequestListComponent)
      },
      {
        path: 'staff/requests',
        canActivate: [staffGuard],
        loadComponent: () => import('./features/staff/staff-queue.component').then(m => m.StaffQueueComponent)
      },
      {
        path: 'staff/requests/:id',
        canActivate: [staffGuard],
        loadComponent: () => import('./features/staff/request-detail.component').then(m => m.RequestDetailComponent)
      },
      {
        path: 'staff/requests/:id/quote',
        canActivate: [staffGuard],
        loadComponent: () => import('./features/staff/quotation-builder.component').then(m => m.QuotationBuilderComponent)
      },
      {
        path: 'staff/requests/:id/project-new',
        canActivate: [staffGuard],
        loadComponent: () => import('./features/staff/project-new.component').then(m => m.ProjectNewComponent)
      },
      {
        path: 'staff/projects/:id',
        canActivate: [staffGuard],
        loadComponent: () => import('./features/staff/project-workspace.component').then(m => m.ProjectWorkspaceComponent)
      },
      {
        path: 'staff/consultants',
        canActivate: [staffGuard],
        loadComponent: () => import('./features/staff/consultants.component').then(m => m.ConsultantsComponent)
      },
      {
        path: 'knowledge',
        loadComponent: () => import('./features/content/knowledge.component').then(m => m.KnowledgeComponent)
      },
      {
        path: 'training',
        loadComponent: () => import('./features/content/training.component').then(m => m.TrainingComponent)
      }
    ]
  },
  { path: '**', redirectTo: '' }
];
