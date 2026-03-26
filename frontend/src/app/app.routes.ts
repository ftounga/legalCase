import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./landing/landing.component').then(m => m.LandingComponent),
    pathMatch: 'full'
  },
  {
    path: 'login',
    loadComponent: () => import('./auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: '',
    loadComponent: () => import('./layout/shell/shell.component').then(m => m.ShellComponent),
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'case-files', pathMatch: 'full' },
      {
        path: 'case-files',
        loadComponent: () => import('./case-files/case-files-list/case-files-list.component')
          .then(m => m.CaseFilesListComponent)
      },
      {
        path: 'case-files/:id',
        loadComponent: () => import('./case-files/case-file-detail/case-file-detail.component')
          .then(m => m.CaseFileDetailComponent)
      },
      {
        path: 'case-files/:id/synthesis',
        loadComponent: () => import('./case-files/synthesis/synthesis.component')
          .then(m => m.SynthesisComponent)
      },
      {
        path: 'case-files/:id/diff',
        loadComponent: () => import('./case-files/analysis-diff/analysis-diff.component')
          .then(m => m.AnalysisDiffComponent)
      },
      {
        path: 'workspace/members',
        loadComponent: () => import('./workspace/workspace-members/workspace-members.component')
          .then(m => m.WorkspaceMembersComponent)
      },
      {
        path: 'workspace/billing',
        loadComponent: () => import('./workspace/workspace-billing/workspace-billing.component')
          .then(m => m.WorkspaceBillingComponent)
      },
      {
        path: 'workspace/admin',
        loadComponent: () => import('./workspace/workspace-admin/workspace-admin.component')
          .then(m => m.WorkspaceAdminComponent)
      },
      {
        path: 'workspace/audit-logs',
        loadComponent: () => import('./workspace/audit-log-screen/audit-log-screen.component')
          .then(m => m.AuditLogScreenComponent)
      },
      {
        path: 'super-admin',
        loadComponent: () => import('./super-admin/super-admin.component')
          .then(m => m.SuperAdminComponent)
      }
    ]
  },
  {
    path: 'onboarding',
    loadComponent: () => import('./onboarding/onboarding.component')
      .then(m => m.OnboardingComponent)
  },
  {
    path: 'invite',
    loadComponent: () => import('./invite-accept/invite-accept.component')
      .then(m => m.InviteAcceptComponent)
  },
  {
    path: 'verify-email',
    loadComponent: () => import('./auth/verify-email/verify-email.component')
      .then(m => m.VerifyEmailComponent)
  },
  {
    path: 'reset-password',
    loadComponent: () => import('./auth/reset-password/reset-password.component')
      .then(m => m.ResetPasswordComponent)
  },
  { path: '**', redirectTo: '' }
];
