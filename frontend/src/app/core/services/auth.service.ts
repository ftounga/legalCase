import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, of } from 'rxjs';
import { User } from '../models/user.model';

export interface LoginRequest { email: string; password: string; }
export interface RegisterRequest { firstName: string; lastName: string; email: string; password: string; }
export interface ForgotPasswordRequest { email: string; }
export interface ResetPasswordRequest { token: string; newPassword: string; }

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = '/api';
  readonly currentUser = signal<User | null>(null);

  constructor(private http: HttpClient) {}

  loadCurrentUser(): Observable<User | null> {
    return this.http.get<User>(`${this.apiUrl}/me`).pipe(
      tap(user => this.currentUser.set(user)),
      catchError(() => {
        this.currentUser.set(null);
        return of(null);
      })
    );
  }

  logout(): void {
    this.http.post(`${this.apiUrl}/logout`, {}).subscribe({
      complete: () => this.redirectToLogin(),
      error: () => this.redirectToLogin()
    });
  }

  loginLocal(request: LoginRequest): Observable<User> {
    return this.http.post<User>(`${this.apiUrl}/v1/auth/login`, request).pipe(
      tap(user => this.currentUser.set(user))
    );
  }

  register(request: RegisterRequest): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/v1/auth/register`, request);
  }

  forgotPassword(request: ForgotPasswordRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/v1/auth/forgot-password`, request);
  }

  verifyEmail(token: string): Observable<{ message: string }> {
    return this.http.get<{ message: string }>(`${this.apiUrl}/v1/auth/verify-email`, { params: { token } });
  }

  resetPassword(request: ResetPasswordRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/v1/auth/reset-password`, request);
  }

  loginWithGoogle(): void {
    window.location.href = '/oauth2/authorization/google';
  }

  private redirectToLogin(): void {
    this.currentUser.set(null);
    window.location.href = '/login';
  }
}
