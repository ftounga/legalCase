import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TractionOnePagerInput } from './traction-onepager.model';

/**
 * F-187 SF-187-02 — Service Angular qui consomme l'endpoint
 * `POST /api/v1/super-admin/traction-onepager/pdf` (SF-187-01).
 *
 * - `generatePdf` retourne un Blob PDF.
 * - `triggerDownload` crée un anchor `<a download>` programmatique pour
 *   déclencher le téléchargement côté navigateur.
 */
@Injectable({ providedIn: 'root' })
export class TractionOnePagerService {
  private readonly url = '/api/v1/super-admin/traction-onepager/pdf';

  constructor(private http: HttpClient) {}

  /** POST le formulaire et retourne le PDF binaire. */
  generatePdf(input: TractionOnePagerInput): Observable<Blob> {
    return this.http.post(this.url, input, { responseType: 'blob' });
  }

  /**
   * Déclenche le téléchargement du blob côté navigateur en créant un anchor
   * `<a download="filename">` programmatique. Pattern identique à
   * `AuditLogScreenComponent.exportCsv` et `TimeReportComponent.exportCsv`.
   */
  triggerDownload(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  }

  /** Compose le nom de fichier `legalcase-traction-YYYY-MM-DD.pdf` (date locale). */
  buildFilename(date: Date = new Date()): string {
    const yyyy = date.getFullYear();
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const dd = String(date.getDate()).padStart(2, '0');
    return `legalcase-traction-${yyyy}-${mm}-${dd}.pdf`;
  }
}
