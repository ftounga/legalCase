import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { MENTIONS_LEGALES, POLITIQUE_CONFIDENTIALITE, CGU } from './legal/legal-content';
import { NotFoundComponent } from './not-found/not-found.component';

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
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () => import('./dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
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
        path: 'workspace/time-report',
        loadComponent: () => import('./workspace/time-report/time-report.component')
          .then(m => m.TimeReportComponent)
      },
      {
        path: 'referentials',
        loadComponent: () => import('./referentials/referentials.component')
          .then(m => m.ReferentialsComponent)
      },
      {
        path: 'search',
        loadComponent: () => import('./search/search.component')
          .then(m => m.SearchComponent)
      },
      {
        path: 'super-admin',
        loadComponent: () => import('./super-admin/super-admin.component')
          .then(m => m.SuperAdminComponent)
      },
      {
        path: 'super-admin/backlog',
        loadComponent: () => import('./super-admin/backlog/super-admin-backlog.component')
          .then(m => m.SuperAdminBacklogComponent)
      },
      {
        path: 'super-admin/blog',
        loadComponent: () => import('./super-admin/blog/super-admin-blog.component')
          .then(m => m.SuperAdminBlogComponent)
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
  {
    path: 'share/:token',
    loadComponent: () => import('./public-share/public-share.component')
      .then(m => m.PublicShareComponent)
  },
  {
    path: 'contact',
    loadComponent: () => import('./contact/contact.component').then(m => m.ContactComponent)
  },
  {
    path: 'blog',
    loadComponent: () => import('./blog/blog-list-page/blog-list-page.component')
      .then(m => m.BlogListPageComponent)
  },
  {
    path: 'blog/:slug',
    loadComponent: () => import('./blog/blog-article-page/blog-article-page.component')
      .then(m => m.BlogArticlePageComponent)
  },
  {
    path: 'mentions-legales',
    loadComponent: () => import('./legal/legal-page.component').then(m => m.LegalPageComponent),
    data: { title: 'Mentions légales', sections: MENTIONS_LEGALES }
  },
  {
    path: 'privacy',
    loadComponent: () => import('./legal/legal-page.component').then(m => m.LegalPageComponent),
    data: { title: 'Politique de confidentialité', sections: POLITIQUE_CONFIDENTIALITE }
  },
  {
    path: 'cgu',
    loadComponent: () => import('./legal/legal-page.component').then(m => m.LegalPageComponent),
    data: { title: 'Conditions Générales d\'Utilisation', sections: CGU }
  },
  { path: '**', component: NotFoundComponent }
];
