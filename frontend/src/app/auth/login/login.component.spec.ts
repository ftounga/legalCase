import { TestBed } from '@angular/core/testing';
import { ComponentFixture } from '@angular/core/testing';
import { LoginComponent } from './login.component';
import { AuthService } from '../../core/services/auth.service';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let authSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authSpy = jasmine.createSpyObj('AuthService', ['loginWithGoogle', 'loginWithMicrosoft']);
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        { provide: AuthService, useValue: authSpy },
        provideAnimationsAsync()
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('clic Google → appelle loginWithGoogle()', () => {
    const buttons = fixture.nativeElement.querySelectorAll('button');
    const googleBtn = Array.from(buttons).find((b: any) => b.textContent.includes('Google')) as HTMLElement;
    googleBtn.click();
    expect(authSpy.loginWithGoogle).toHaveBeenCalledTimes(1);
  });

  it('clic Microsoft → appelle loginWithMicrosoft()', () => {
    const buttons = fixture.nativeElement.querySelectorAll('button');
    const msBtn = Array.from(buttons).find((b: any) => b.textContent.includes('Microsoft')) as HTMLElement;
    msBtn.click();
    expect(authSpy.loginWithMicrosoft).toHaveBeenCalledTimes(1);
  });
});
