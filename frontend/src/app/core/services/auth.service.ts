import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, of } from 'rxjs';
import { User } from '../models/user.model';

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

  loginWithGoogle(): void {
    window.location.href = '/oauth2/authorization/google';
  }

  loginWithMicrosoft(): void {
    window.location.href = '/oauth2/authorization/microsoft';
  }

  private redirectToLogin(): void {
    this.currentUser.set(null);
    window.location.href = '/login';
  }
}
