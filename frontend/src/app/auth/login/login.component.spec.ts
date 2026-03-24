import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, RouterModule } from '@angular/router';
import { Component } from '@angular/core';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login.component';
import { AuthService } from '../../core/services/auth.service';

@Component({ template: '', standalone: true })
class StubComponent {}

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;
  let authSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authSpy = jasmine.createSpyObj('AuthService', [
      'loginWithGoogle', 'loginLocal', 'register', 'forgotPassword'
    ]);

    await TestBed.configureTestingModule({
      imports: [LoginComponent, NoopAnimationsModule],
      providers: [
        { provide: AuthService, useValue: authSpy },
        provideRouter([{ path: 'case-files', component: StubComponent }])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // T-01 : composant créé
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // T-02 : onglet "Se connecter" visible par défaut
  it('affiche l\'onglet Se connecter par défaut', () => {
    expect(fixture.nativeElement.textContent).toContain('Se connecter');
  });

  // T-03 : onglet "S'inscrire" visible
  it('affiche l\'onglet S\'inscrire', () => {
    expect(fixture.nativeElement.textContent).toContain("S'inscrire");
  });

  // T-04 : clic Google → loginWithGoogle()
  it('clic Google → appelle loginWithGoogle()', () => {
    const buttons = fixture.nativeElement.querySelectorAll('button');
    const googleBtn = Array.from(buttons).find((b: any) => b.textContent.includes('Google')) as HTMLElement;
    googleBtn.click();
    expect(authSpy.loginWithGoogle).toHaveBeenCalledTimes(1);
  });

  // T-05 : soumission login valide → loginLocal() appelé
  it('soumission login valide → loginLocal() appelé', fakeAsync(() => {
    authSpy.loginLocal.and.returnValue(of({ id: '1', email: 'a@a.com', firstName: 'A', lastName: 'B', provider: 'LOCAL', isSuperAdmin: false }));
    component.loginForm.setValue({ email: 'alice@example.com', password: 'password123' });
    component.submitLogin();
    tick();
    expect(authSpy.loginLocal).toHaveBeenCalledWith({ email: 'alice@example.com', password: 'password123' });
  }));

  // T-07 : erreur 401 → message "Identifiants invalides."
  it('erreur 401 login → affiche message identifiants invalides', fakeAsync(() => {
    authSpy.loginLocal.and.returnValue(throwError(() => ({ status: 401 })));
    component.loginForm.setValue({ email: 'alice@example.com', password: 'wrong' });
    component.submitLogin();
    tick();
    fixture.detectChanges();
    expect(component.loginError()).toBe('Identifiants invalides.');
    expect(fixture.nativeElement.textContent).toContain('Identifiants invalides.');
  }));

  // T-08 : erreur 403 → message email non vérifié
  it('erreur 403 login → affiche message email non vérifié', fakeAsync(() => {
    authSpy.loginLocal.and.returnValue(throwError(() => ({ status: 403 })));
    component.loginForm.setValue({ email: 'alice@example.com', password: 'password123' });
    component.submitLogin();
    tick();
    fixture.detectChanges();
    expect(component.loginError()).toContain('valider votre email');
  }));

  // T-09 : soumission inscription → register() appelé
  it('soumission inscription → register() appelé', fakeAsync(() => {
    authSpy.register.and.returnValue(of(undefined));
    component.registerForm.setValue({
      firstName: 'Alice', lastName: 'Dupont',
      email: 'alice@example.com', password: 'password123'
    });
    component.submitRegister();
    tick();
    expect(authSpy.register).toHaveBeenCalled();
    expect(component.registerSuccess()).toBeTrue();
  }));

  // T-10 : erreur 409 inscription → message email déjà utilisé
  it('erreur 409 inscription → affiche message email déjà utilisé', fakeAsync(() => {
    authSpy.register.and.returnValue(throwError(() => ({ status: 409 })));
    component.registerForm.setValue({
      firstName: 'Alice', lastName: 'Dupont',
      email: 'dup@example.com', password: 'password123'
    });
    component.submitRegister();
    tick();
    fixture.detectChanges();
    expect(component.registerError()).toBe('Cet email est déjà utilisé.');
  }));
});
