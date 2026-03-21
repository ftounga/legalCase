import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { VerifyEmailComponent } from './verify-email.component';
import { AuthService } from '../../core/services/auth.service';

function makeRoute(token: string) {
  return {
    snapshot: { queryParamMap: { get: (key: string) => key === 'token' ? token : null } }
  };
}

describe('VerifyEmailComponent', () => {
  let fixture: ComponentFixture<VerifyEmailComponent>;
  let component: VerifyEmailComponent;
  let authSpy: jasmine.SpyObj<AuthService>;

  async function setup(token: string, serviceResult: any) {
    authSpy = jasmine.createSpyObj('AuthService', ['verifyEmail']);
    authSpy.verifyEmail.and.returnValue(serviceResult);

    await TestBed.configureTestingModule({
      imports: [VerifyEmailComponent, NoopAnimationsModule],
      providers: [
        { provide: AuthService, useValue: authSpy },
        provideRouter([]),
        { provide: ActivatedRoute, useValue: makeRoute(token) }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(VerifyEmailComponent);
    component = fixture.componentInstance;
  }

  // T-11 : au chargement → verifyEmail() appelé avec le token de l'URL
  it('au chargement → verifyEmail() appelé avec le token', fakeAsync(async () => {
    await setup('my-token', of({ message: 'Email validé.' }));
    fixture.detectChanges();
    tick();
    expect(authSpy.verifyEmail).toHaveBeenCalledWith('my-token');
  }));

  // T-12 : succès → message de succès affiché
  it('succès → affiche message de succès', fakeAsync(async () => {
    await setup('valid-token', of({ message: 'Email validé.' }));
    fixture.detectChanges();
    tick();
    fixture.detectChanges();
    expect(component.success()).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Email validé');
  }));

  // T-13 : erreur → message d'erreur affiché
  it('erreur → affiche message d\'erreur', fakeAsync(async () => {
    await setup('bad-token', throwError(() => ({ status: 400 })));
    fixture.detectChanges();
    tick();
    fixture.detectChanges();
    expect(component.success()).toBeFalse();
    expect(component.errorMessage()).toContain('invalide');
  }));
});
