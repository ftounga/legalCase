import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, shareReplay, catchError } from 'rxjs';

export interface ConventionOption {
  code: string;
  label: string;
  country: string;
}

/**
 * SF-129-01 : charge la liste des conventions collectives depuis le backend
 * (~49 CCN FR + 3 CP BE seedées dans legal_referentials). Cache simple pour
 * éviter de re-fetch à chaque montée de composant.
 *
 * Fallback : si l'endpoint échoue, renvoie la liste minimale hardcodée pour
 * garder l'app fonctionnelle.
 */
@Injectable({ providedIn: 'root' })
export class ConventionReferentialService {
  private cache$?: Observable<ConventionOption[]>;

  private readonly FALLBACK: ConventionOption[] = [
    { code: 'METALLURGIE', label: 'Métallurgie (IDCC 3248)', country: 'FRANCE' },
    { code: 'COMMERCE',    label: 'Commerce de détail (IDCC 2216)', country: 'FRANCE' },
    { code: 'BTP',         label: 'BTP (IDCC 1596)', country: 'FRANCE' },
    { code: 'HCR',         label: 'Hôtels, cafés, restaurants (IDCC 1979)', country: 'FRANCE' },
    { code: 'SYNTEC',      label: 'Syntec (IDCC 1486)', country: 'FRANCE' },
    { code: 'CP200',       label: 'CP 200 — Employés', country: 'BELGIQUE' },
    { code: 'CP124',       label: 'CP 124 — Construction', country: 'BELGIQUE' },
    { code: 'CP302',       label: 'CP 302 — Hôtellerie', country: 'BELGIQUE' },
  ];

  constructor(private http: HttpClient) {}

  list(): Observable<ConventionOption[]> {
    if (!this.cache$) {
      this.cache$ = this.http.get<ConventionOption[]>('/api/v1/referentials/conventions').pipe(
        catchError(() => of(this.FALLBACK)),
        shareReplay(1)
      );
    }
    return this.cache$;
  }

  /**
   * SF-129-01 : normalise un code convention retourné par l'IA en un entry_key exploitable.
   * - "IDCC_3043" → "IDCC_3043" (canonique)
   * - "3043" → "IDCC_3043"
   * - "IDCC 3043" → "IDCC_3043"
   * - "METALLURGIE" → "IDCC_3248" (alias legacy)
   * - "CP200" → "CP200" (BE inchangé)
   * - "NETTOYAGE" → "NETTOYAGE" (descriptif sans mapping — fallback caller)
   */
  static normalizeCode(raw: string | null | undefined): string | null {
    if (!raw) return null;
    const trimmed = raw.trim().toUpperCase();
    if (!trimmed) return null;

    if (/^IDCC_\d{4}$/.test(trimmed)) return trimmed;
    if (trimmed.startsWith('IDCC')) {
      const digits = trimmed.substring(4).replace(/\D/g, '');
      if (digits.length >= 1 && digits.length <= 4) return 'IDCC_' + digits.padStart(4, '0');
    }
    if (/^\d{1,4}$/.test(trimmed)) return 'IDCC_' + trimmed.padStart(4, '0');
    if (/^CP\d{2,4}$/.test(trimmed)) return trimmed;

    const LEGACY: Record<string, string> = {
      'METALLURGIE': 'IDCC_3248',
      'COMMERCE':    'IDCC_2216',
      'BTP':         'IDCC_1596',
      'HCR':         'IDCC_1979',
      'SYNTEC':      'IDCC_1486',
    };
    return LEGACY[trimmed] ?? trimmed;
  }
}
