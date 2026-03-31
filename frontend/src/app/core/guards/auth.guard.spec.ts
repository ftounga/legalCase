import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { authGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';
import { WorkspaceService } from '../services/workspace.service';
import { of, throwError } from 'rxjs';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { runInInjectionContext } from '@angular/core';

describe('authGuard', () => {
  let router: jasmine.SpyObj<Router>;
  let authService: jasmine.SpyObj<AuthService>;
  let workspaceService: jasmine.SpyObj<WorkspaceService>;

  beforeEach(() => {
    router = jasmine.createSpyObj('Router', ['createUrlTree']);
    authService = jasmine.createSpyObj('AuthService', ['loadCurrentUser']);
    workspaceService = jasmine.createSpyObj('WorkspaceService', ['getCurrentWorkspace']);
    router.createUrlTree.and.callFake((commands: any[]) => commands[0] as any);

    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: router },
        { provide: AuthService, useValue: authService },
        { provide: WorkspaceService, useValue: workspaceService }
      ]
    });
  });

  function runGuard() {
    return TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );
  }

  // T-04 : user non authentifié → redirect /login
  it('user non authentifié → redirect /login', (done) => {
    authService.loadCurrentUser.and.returnValue(of(null));

    (runGuard() as any).subscribe((result: any) => {
      expect(result).toBe('/login');
      done();
    });
  });

  // T-05 : user authentifié + workspace existant → accès autorisé
  it('user authentifié + workspace existant → true', (done) => {
    authService.loadCurrentUser.and.returnValue(of({ email: 'test@example.com' } as any));
    workspaceService.getCurrentWorkspace.and.returnValue(of({} as any));

    (runGuard() as any).subscribe((result: any) => {
      expect(result).toBeTrue();
      done();
    });
  });

  // T-06 : user authentifié + pas de workspace (404) → redirect /onboarding
  it('user authentifié + 404 workspace → redirect /onboarding', (done) => {
    authService.loadCurrentUser.and.returnValue(of({ email: 'test@example.com' } as any));
    workspaceService.getCurrentWorkspace.and.returnValue(throwError(() => ({ status: 404 })));

    (runGuard() as any).subscribe((result: any) => {
      expect(result).toBe('/onboarding');
      done();
    });
  });

  // T-07 : user authentifié + erreur non-404 → accès autorisé (fail-open)
  it('user authentifié + erreur réseau → true (fail-open)', (done) => {
    authService.loadCurrentUser.and.returnValue(of({ email: 'test@example.com' } as any));
    workspaceService.getCurrentWorkspace.and.returnValue(throwError(() => ({ status: 500 })));

    (runGuard() as any).subscribe((result: any) => {
      expect(result).toBeTrue();
      done();
    });
  });

  // T-04 (SF-81) : user authentifié + returnUrl en sessionStorage → redirect returnUrl + sessionStorage effacé
  it('user authentifié + returnUrl sessionStorage → redirect returnUrl et sessionStorage effacé', (done) => {
    sessionStorage.setItem('auth.returnUrl', '/case-files/abc');
    authService.loadCurrentUser.and.returnValue(of({ email: 'test@example.com' } as any));

    (runGuard() as any).subscribe((result: any) => {
      expect(result).toBe('/case-files/abc');
      expect(sessionStorage.getItem('auth.returnUrl')).toBeNull();
      done();
    });
  });

  // T-05 (SF-81) : user authentifié + pas de returnUrl → comportement normal (workspace check)
  it('user authentifié + pas de returnUrl → true (comportement normal)', (done) => {
    sessionStorage.removeItem('auth.returnUrl');
    authService.loadCurrentUser.and.returnValue(of({ email: 'test@example.com' } as any));
    workspaceService.getCurrentWorkspace.and.returnValue(of({} as any));

    (runGuard() as any).subscribe((result: any) => {
      expect(result).toBeTrue();
      done();
    });
  });
});
