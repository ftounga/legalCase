import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { tap } from 'rxjs/operators';
import { BaremeResponse } from '../models/bareme.model';

@Injectable({ providedIn: 'root' })
export class BaremeService {
  private cache = new Map<string, BaremeResponse>();

  constructor(private http: HttpClient) {}

  get(conventionCode: string): Observable<BaremeResponse> {
    const cached = this.cache.get(conventionCode);
    if (cached) return of(cached);
    return this.http.get<BaremeResponse>(`/api/v1/anciennete/baremes/${conventionCode}`).pipe(
      tap(b => this.cache.set(conventionCode, b))
    );
  }

  /** Helper : retourne le pourcentage de prime maximum applicable selon l'ancienneté. */
  static maxPrimePourcentage(bareme: BaremeResponse, anneesAnciennete: number): number {
    return bareme.primesAnciennete
      .filter(p => anneesAnciennete >= p.ancienneteMinAnnees)
      .reduce((max, p) => Math.max(max, p.pourcentage), 0);
  }

  /** Helper : retourne le total congés (légal + supp applicable). */
  static totalConges(bareme: BaremeResponse, anneesAnciennete: number): number {
    const supp = bareme.congesSupplementaires
      .filter(c => anneesAnciennete >= c.ancienneteMinAnnees)
      .reduce((max, c) => Math.max(max, c.joursSupplementaires), 0);
    return bareme.congesLegauxJours + supp;
  }
}
