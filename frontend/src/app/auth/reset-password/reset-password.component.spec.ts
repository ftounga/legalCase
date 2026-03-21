import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ResetPasswordComponent } from './reset-password.component';
import { AuthService } from '../../core/services/auth.service';

function makeRoute(token: string) {
  return {
    snapshot: { queryParamMap: { get: (key: string) => key === 'token' ? token : null } }
  };
}

describe('ResetPasswordComponent', () => {
  let fixture: ComponentFixture<ResetPasswordComponent>;
  let component: ResetPasswordComponent;
  let authSpy: jasmine.SpyObj<AuthService>;

  async function setup(token = 'reset-token') {
    authSpy = jasmine.createSpyObj('AuthService', ['resetPassword']);

    await TestBed.configureTestingModule({
      imports: [ResetPasswordComponent, NoopAnimationsModule],
      providers: [
        { provide: AuthService, useValue: authSpy },
        provideRouter([]),
        { provide: ActivatedRoute, useValue: makeRoute(token) }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ResetPasswordComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  // T-14 : soumission avec mots de passe concordants → resetPassword() appelé
  it('soumission mots de passe concordants → resetPassword() appelé', fakeAsync(async () => {
    await setup();
    authSpy.resetPassword.and.returnValue(of({ message: 'Réinitialisé.' }));
    component.form.setValue({ newPassword: 'newpassword', confirmPassword: 'newpassword' });
    component.submit();
    tick();
    expect(authSpy.resetPassword).toHaveBeenCalledWith({ token: 'reset-token', newPassword: 'newpassword' });
  }));

  // T-15 : mots de passe ne concordent pas → erreur client, pas d'appel API
  it('mots de passe discordants → pas d\'appel API', fakeAsync(async () => {
    await setup();
    component.form.setValue({ newPassword: 'password1', confirmPassword: 'password2' });
    component.submit();
    tick();
    expect(authSpy.resetPassword).not.toHaveBeenCalled();
    expect(component.form.hasError('mismatch')).toBeTrue();
  }));

  // T-16 : succès → message de succès affiché
  it('succès → affiche message de succès', fakeAsync(async () => {
    await setup();
    authSpy.resetPassword.and.returnValue(of({ message: 'Réinitialisé.' }));
    component.form.setValue({ newPassword: 'newpassword', confirmPassword: 'newpassword' });
    component.submit();
    tick();
    fixture.detectChanges();
    expect(component.success()).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('réinitialisé');
  }));
});
