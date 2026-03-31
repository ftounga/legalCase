import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let router: jasmine.SpyObj<Router>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;

  beforeEach(() => {
    router = jasmine.createSpyObj('Router', ['navigate'], { url: '/case-files/abc' });
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: router },
        { provide: MatSnackBar, useValue: snackBar }
      ]
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    sessionStorage.removeItem('auth.returnUrl');
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.removeItem('auth.returnUrl');
  });

  // T-01 : 401 → snackbar + navigate /login + sessionStorage renseigné
  it('401 → snackbar "session expirée", navigate /login, sessionStorage renseigné', () => {
    http.get('/api/test').subscribe({ error: () => {} });

    const req = httpMock.expectOne('/api/test');
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(snackBar.open).toHaveBeenCalledWith(
      jasmine.stringContaining('expir'),
      'Fermer',
      jasmine.objectContaining({ panelClass: ['snack-error'] })
    );
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
    expect(sessionStorage.getItem('auth.returnUrl')).toBe('/case-files/abc');
  });

  // non-401 → pas de snackbar, pas de redirect
  it('erreur non-401 → snackbar non appelé, navigate non appelé', () => {
    http.get('/api/test').subscribe({ error: () => {} });

    const req = httpMock.expectOne('/api/test');
    req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });

    expect(snackBar.open).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
