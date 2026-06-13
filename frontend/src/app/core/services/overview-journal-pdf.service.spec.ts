import { OverviewJournalPdfService } from './overview-journal-pdf.service';
import { OverviewResponse } from '../models/overview.model';

/** Récupère récursivement tous les `text` d'un nœud pdfmake (string ou tableau). */
function collectText(node: any, acc: string[] = []): string[] {
  if (node == null) return acc;
  if (typeof node === 'string') {
    acc.push(node);
  } else if (Array.isArray(node)) {
    node.forEach(n => collectText(n, acc));
  } else if (typeof node === 'object') {
    if (node.text !== undefined) collectText(node.text, acc);
    if (node.ul !== undefined) collectText(node.ul, acc);
    if (node.content !== undefined) collectText(node.content, acc);
  }
  return acc;
}

function response(): OverviewResponse {
  return {
    pilotage: {
      currentPhaseLabel: 'Mise en état',
      awaitingParty: 'OURS',
      nextDeadline: { label: 'Conclusions', dueDate: '2026-02-01', daysUntil: 10, urgency: 'SOON' },
      sante: { admissibility: 'Recevable', prescriptionStatus: 'OK', riskLevelAvocat: 'MOYEN' },
      analysisStale: false, pendingPiecesCount: 0, toolsCalculated: 2, toolsVisible: 5,
    },
    attention: [
      { type: 'ECHEANCE', label: 'Échéance proche', urgency: 'HIGH',
        action: { kind: 'VALIDATE_DEADLINE', targetTab: 'suivi' } },
    ],
    attentionTotal: 1,
    fil: [
      { date: '2025-12-01', voie: 'PROCEDURE', acteur: 'SYSTEME', titre: 'Assignation',
        detail: 'TJ Paris', temps: 'PASSE', urgency: null, attachments: [], action: null },
      { date: '2026-03-01', voie: 'ECHEANCE', acteur: null, titre: 'Audience',
        detail: null, temps: 'FUTUR', urgency: null, attachments: [], action: null },
    ],
  };
}

describe('OverviewJournalPdfService', () => {
  let service: OverviewJournalPdfService;
  let snackBar: { open: jest.Mock };

  beforeEach(() => {
    snackBar = { open: jest.fn() };
    service = new OverviewJournalPdfService(snackBar as any);
  });

  it('buildDocument contient le titre et les 3 sections (Pilotage / Attention / Fil)', () => {
    const doc = service.buildDocument(response(), 'Dossier Durand') as any;
    const texts = collectText(doc.content).join(' | ');
    expect(texts).toContain('Dossier Durand');
    expect(texts).toContain('Pilotage');
    expect(texts).toContain('Ce qui requiert votre attention');
    expect(texts).toContain('Fil du dossier');
    // Contenu fidèle : phase, échéance, événements passé + futur.
    expect(texts).toContain('Mise en état');
    expect(texts).toContain('Échéance proche');
    expect(texts).toContain('Assignation');
    expect(texts).toContain('Audience');
    expect(texts).toContain('01/12/2025'); // date formatée dd/MM/yyyy
  });

  it('nommage du fichier = journal-{caseFileId}.pdf', () => {
    expect(service.buildFileName('cf-123')).toBe('journal-cf-123.pdf');
  });

  it('attention vide → message « à jour »', () => {
    const r = response();
    r.attention = [];
    const texts = collectText((service.buildDocument(r, 'X') as any).content).join(' | ');
    expect(texts).toContain('à jour');
  });
});
