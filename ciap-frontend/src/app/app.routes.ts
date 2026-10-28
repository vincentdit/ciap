import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { staffGuard } from './core/guards/staff.guard';
import { adminGuard } from './core/guards/admin.guard';
import { financeGuard } from './core/guards/finance.guard';
import { hodGuard } from './core/guards/hod.guard';

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
        path: 'finance',
        canActivate: [financeGuard],
        loadComponent: () => import('./features/finance/finance.component').then(m => m.FinanceComponent)
      },
      {
        path: 'department',
        canActivate: [hodGuard],
        loadComponent: () => import('./features/department/department.component').then(m => m.DepartmentComponent)
      },
      {
        path: 'staff/departments',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/department/departments-admin.component').then(m => m.DepartmentsAdminComponent)
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
        path: 'staff/users',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/staff/users.component').then(m => m.UsersComponent)
      },
      {
        path: 'knowledge',
        loadComponent: () => import('./features/content/knowledge.component').then(m => m.KnowledgeComponent)
      },
      {
        path: 'training',
        loadComponent: () => import('./features/content/training.component').then(m => m.TrainingComponent)
      },
      {
        path: 'notices',
        loadComponent: () => import('./features/account/notices.component').then(m => m.NoticesComponent)
      },
      {
        path: 'profile',
        loadComponent: () => import('./features/account/profile.component').then(m => m.ProfileComponent)
      },
      {
        path: 'account/password',
        loadComponent: () => import('./features/account/change-password.component').then(m => m.ChangePasswordComponent)
      }
    ]
  },
  { path: '**', redirectTo: '' }
];
