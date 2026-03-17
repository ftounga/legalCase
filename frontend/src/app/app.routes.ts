import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
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
      }
    ]
  },
  { path: '**', redirectTo: 'login' }
];
