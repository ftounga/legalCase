import { Component, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatTabsModule } from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule, MatButtonModule, MatIconModule, MatDividerModule,
    MatTabsModule, MatFormFieldModule, MatInputModule, MatProgressSpinnerModule
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  loginForm: FormGroup;
  registerForm: FormGroup;
  forgotForm: FormGroup;

  loginError = signal('');
  registerSuccess = signal(false);
  registerError = signal('');
  forgotSuccess = signal(false);
  forgotVisible = signal(false);
  loading = signal(false);

  constructor(
    private fb: FormBuilder,
    private router: Router,
    readonly auth: AuthService
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });

    this.registerForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.maxLength(100)]],
      lastName: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
      password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]]
    });

    this.forgotForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  submitLogin(): void {
    if (this.loginForm.invalid) return;
    this.loading.set(true);
    this.loginError.set('');
    this.auth.loginLocal(this.loginForm.value).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/case-files']);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        if (err.status === 403) {
          this.loginError.set('Veuillez valider votre email avant de vous connecter.');
        } else {
          this.loginError.set('Identifiants invalides.');
        }
      }
    });
  }

  submitRegister(): void {
    if (this.registerForm.invalid) return;
    this.loading.set(true);
    this.registerError.set('');
    this.registerSuccess.set(false);
    this.auth.register(this.registerForm.value).subscribe({
      next: () => {
        this.loading.set(false);
        this.registerSuccess.set(true);
        this.registerForm.reset();
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        if (err.status === 409) {
          this.registerError.set('Cet email est déjà utilisé.');
        } else {
          this.registerError.set('Une erreur est survenue. Veuillez réessayer.');
        }
      }
    });
  }

  submitForgot(): void {
    if (this.forgotForm.invalid) return;
    this.loading.set(true);
    this.auth.forgotPassword(this.forgotForm.value).subscribe({
      next: () => {
        this.loading.set(false);
        this.forgotSuccess.set(true);
        this.forgotForm.reset();
      },
      error: () => {
        this.loading.set(false);
        this.forgotSuccess.set(true); // fail-silent côté UI
      }
    });
  }

  toggleForgot(): void {
    this.forgotVisible.set(!this.forgotVisible());
    this.forgotSuccess.set(false);
    this.forgotForm.reset();
  }
}
