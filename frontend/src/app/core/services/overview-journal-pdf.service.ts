import { Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TDocumentDefinitions } from 'pdfmake/interfaces';
import { OverviewResponse, TimelineEvent } from '../models/overview.model';

/**
 * F-289 / SF-289-04 — Export PDF du journal du dossier (Vue d'ensemble).
 *
 * <p>Génération <b>100 % frontend</b> (aucun appel backend) via `pdfmake`,
 * import dynamique (mirror {@link PdfExportService}). Le document reprend les
 * 3 registres de la vue : Pilotage (phase / tour / couperet / santé),
 * Attention (to-do priorisée), Fil (passé + futur, dates en mono). Charte
 * sobre navy / or du DESIGN_SYSTEM. Un échec (import ou génération) affiche
 * une `MatSnackBar` non bloquante, sans téléchargement.</p>
 */
const NAVY = '#1A3A5C';
const GOLD = '#C9973A';
const TEXT = '#1C2B3A';
const MUTED = '#6B7A8D';

@Injectable({ providedIn: 'root' })
export class OverviewJournalPdfService {
  constructor(private snackBar: MatSnackBar) {}

  /** Déclenche la génération + le téléchargement `journal-{caseFileId}.pdf`. */
  export(overview: OverviewResponse, caseTitle: string, caseFileId: string): void {
    import('pdfmake/build/pdfmake')
      .then(pdfMakeModule =>
        import('pdfmake/build/vfs_fonts').then(vfsFontsModule => {
          const pdfMake = (pdfMakeModule.default || pdfMakeModule) as any;
          const vfsFonts = (vfsFontsModule.default || vfsFontsModule) as any;
          pdfMake.vfs = vfsFonts.pdfMake ? vfsFonts.pdfMake.vfs : vfsFonts.vfs;

          const docDefinition = this.buildDocument(overview, caseTitle) as TDocumentDefinitions;
          pdfMake.createPdf(docDefinition).download(this.buildFileName(caseFileId));
        }),
      )
      .catch(() => this.notifyError());
  }

  /**
   * Construit le `TDocumentDefinitions` du journal — extrait (testable sans
   * pdfmake) pour vérifier que les 3 sections attendues sont présentes.
   */
  buildDocument(overview: OverviewResponse, caseTitle: string): object {
    const body: object[] = [];

    body.push({ text: caseTitle || 'Dossier', style: 'h1' });
    body.push({ text: 'Journal du dossier', style: 'subtitle' });

    // ── ① Pilotage ──
    body.push({ text: 'Pilotage', style: 'h2' });
    const p = overview.pilotage;
    body.push({
      style: 'kv',
      ul: [
        `Phase : ${p.currentPhaseLabel || '—'}`,
        `À qui le tour : ${this.partyLabel(p.awaitingParty)}`,
        `Prochain couperet : ${this.deadlineLabel(p)}`,
        `Recevabilité : ${p.sante.admissibility || '—'}`,
        `Prescription : ${p.sante.prescriptionStatus || '—'}`,
        `Risque : ${p.sante.riskLevelAvocat || '—'}`,
      ],
    });

    // ── ② Attention ──
    body.push({ text: 'Ce qui requiert votre attention', style: 'h2' });
    if (!overview.attention?.length) {
      body.push({ text: 'Rien d’urgent. Le dossier est à jour.', style: 'muted' });
    } else {
      body.push({ ul: overview.attention.map(a => a.label), style: 'kv' });
    }

    // ── ③ Fil ──
    body.push({ text: 'Fil du dossier', style: 'h2' });
    const past = (overview.fil ?? []).filter(e => e.temps === 'PASSE');
    const future = (overview.fil ?? []).filter(e => e.temps !== 'PASSE');
    if (past.length === 0 && future.length === 0) {
      body.push({ text: 'Aucun événement.', style: 'muted' });
    } else {
      body.push({ text: 'Passé', style: 'h3' });
      body.push(...this.eventLines(past));
      body.push({ text: 'À venir', style: 'h3' });
      body.push(...this.eventLines(future));
    }

    return {
      pageSize: 'A4',
      pageMargins: [48, 56, 48, 56],
      content: body,
      defaultStyle: { font: 'Roboto', fontSize: 10, color: TEXT },
      styles: {
        h1: { fontSize: 20, bold: true, color: NAVY, margin: [0, 0, 0, 2] },
        subtitle: { fontSize: 12, color: GOLD, margin: [0, 0, 0, 14] },
        h2: { fontSize: 14, bold: true, color: NAVY, margin: [0, 14, 0, 6] },
        h3: { fontSize: 11, bold: true, color: GOLD, margin: [0, 8, 0, 4] },
        kv: { margin: [0, 0, 0, 4] },
        muted: { color: MUTED, italics: true },
        date: { font: 'Roboto', color: MUTED },
      },
    };
  }

  /** Une ligne par événement : « date — titre (détail) ». */
  private eventLines(events: TimelineEvent[]): object[] {
    if (events.length === 0) {
      return [{ text: '—', style: 'muted', margin: [0, 0, 0, 4] }];
    }
    return events.map(e => ({
      margin: [0, 0, 0, 3],
      text: [
        { text: `${this.formatDate(e.date)}  `, style: 'date' },
        { text: e.titre, bold: true },
        e.detail ? { text: ` — ${e.detail}`, color: MUTED } : { text: '' },
      ],
    }));
  }

  private partyLabel(party: string | null): string {
    if (party === 'OURS') return 'À vous de jouer';
    if (party === 'ADVERSE') return 'Partie adverse';
    return '—';
  }

  private deadlineLabel(p: OverviewResponse['pilotage']): string {
    const dl = p.nextDeadline;
    if (!dl) return 'Aucun délai imminent';
    return `${dl.label} (${this.formatDate(dl.dueDate)})`;
  }

  /** Date ISO → `dd/MM/yyyy` (robuste aux entrées vides). */
  private formatDate(iso: string | null | undefined): string {
    if (!iso) return '—';
    const d = new Date(iso);
    if (isNaN(d.getTime())) return iso;
    const dd = String(d.getDate()).padStart(2, '0');
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    return `${dd}/${mm}/${d.getFullYear()}`;
  }

  buildFileName(caseFileId: string): string {
    return `journal-${caseFileId}.pdf`;
  }

  private notifyError(): void {
    this.snackBar.open('Erreur lors de la génération du PDF du journal.', 'Fermer', {
      duration: 4000,
      panelClass: ['snack-error'],
    });
  }
}
