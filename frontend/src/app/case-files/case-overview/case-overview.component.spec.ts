import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { of, throwError } from 'rxjs';
import { CaseOverviewComponent, CaseOverviewSnack, OverviewNavigation } from './case-overview.component';
import { OverviewService } from '../../core/services/overview.service';
import { DocumentService } from '../../core/services/document.service';
import { CaseFileService } from '../../core/services/case-file.service';
import { AiQuestionAnswerService } from '../../core/services/ai-question-answer.service';
import { PieceManquanteStatusService } from '../../core/services/piece-manquante-status.service';
import { OverviewJournalPdfService } from '../../core/services/overview-journal-pdf.service';
import { AttentionItem, OverviewResponse, TimelineEvent } from '../../core/models/overview.model';

/** Construit un événement de fil minimal. */
function event(p: Partial<TimelineEvent>): TimelineEvent {
  return {
    date: '2026-01-01',
    voie: 'PROCEDURE',
    acteur: 'SYSTEME',
    titre: 'Événement',
    detail: null,
    temps: 'PASSE',
    urgency: null,
    attachments: [],
    action: null,
    ...p,
  };
}

/** Réponse non vide par défaut. */
function fullResponse(over: Partial<OverviewResponse> = {}): OverviewResponse {
  return {
    pilotage: {
      currentPhaseLabel: 'Mise en état',
      awaitingParty: 'OURS',
      nextDeadline: { label: 'Conclusions', dueDate: '2026-02-01', daysUntil: 10, urgency: 'MEDIUM' },
      sante: { admissibility: 'Recevable', prescriptionStatus: 'OK', riskLevelAvocat: 'MOYEN' },
      analysisStale: false,
      pendingPiecesCount: 1,
      toolsCalculated: 2,
      toolsVisible: 5,
    },
    attention: [
      { type: 'ECHEANCE', label: 'Échéance proche', urgency: 'HIGH', action: { kind: 'VALIDATE_DEADLINE', targetTab: 'suivi', anchor: 'section-deadlines' } },
    ],
    attentionTotal: 1,
    fil: [
      event({ date: '2025-12-01', titre: 'Assignation', temps: 'PASSE', voie: 'PROCEDURE' }),
      event({ date: '2026-03-01', titre: 'Audience', temps: 'FUTUR', voie: 'ECHEANCE' }),
    ],
    ...over,
  };
}

describe('CaseOverviewComponent', () => {
  let fixture: ComponentFixture<CaseOverviewComponent>;
  let component: CaseOverviewComponent;
  let overviewServiceSpy: jest.Mocked<OverviewService>;
  let documentServiceSpy: jest.Mocked<DocumentService>;
  let caseFileServiceSpy: jest.Mocked<CaseFileService>;
  let answerServiceSpy: jest.Mocked<AiQuestionAnswerService>;
  let pieceServiceSpy: jest.Mocked<PieceManquanteStatusService>;
  let journalPdfSpy: jest.Mocked<OverviewJournalPdfService>;
  let snackErrors: string[];

  async function setup(response: OverviewResponse | Error = fullResponse()): Promise<void> {
    overviewServiceSpy = { getOverview: jest.fn() } as any;
    if (response instanceof Error) {
      overviewServiceSpy.getOverview.mockReturnValue(throwError(() => response));
    } else {
      overviewServiceSpy.getOverview.mockReturnValue(of(response));
    }
    documentServiceSpy = {
      downloadUrl: jest.fn().mockReturnValue('/api/v1/case-files/cf1/documents/d1/download'),
      preview: jest.fn().mockReturnValue(of({} as any)),
      list: jest.fn().mockReturnValue(of([
        { id: 'doc1', caseFileId: 'cf1', originalFilename: 'contrat.pdf', contentType: 'application/pdf', fileSize: 1, createdAt: '2026-06-01' },
      ] as any)),
    } as any;
    caseFileServiceSpy = { exportZip: jest.fn().mockReturnValue(of(new Blob())) } as any;
    answerServiceSpy = { submitAnswer: jest.fn().mockReturnValue(of(undefined)) } as any;
    pieceServiceSpy = { updateStatus: jest.fn().mockReturnValue(of({} as any)) } as any;
    journalPdfSpy = { export: jest.fn() } as any;
    snackErrors = [];

    await TestBed.configureTestingModule({
      imports: [CaseOverviewComponent],
      providers: [
        { provide: OverviewService, useValue: overviewServiceSpy },
        { provide: DocumentService, useValue: documentServiceSpy },
        { provide: CaseFileService, useValue: caseFileServiceSpy },
        { provide: AiQuestionAnswerService, useValue: answerServiceSpy },
        { provide: PieceManquanteStatusService, useValue: pieceServiceSpy },
        { provide: OverviewJournalPdfService, useValue: journalPdfSpy },
        { provide: CaseOverviewSnack, useValue: { error: (m: string) => snackErrors.push(m) } },
        provideAnimationsAsync(),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CaseOverviewComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'cf1';
    fixture.detectChanges();
  }

  const q = (sel: string) => fixture.nativeElement.querySelector(sel);
  const qa = (sel: string) => Array.from(fixture.nativeElement.querySelectorAll(sel));

  it('charge l\'agrégat au ngOnInit et rend les 3 registres', async () => {
    await setup();
    expect(overviewServiceSpy.getOverview).toHaveBeenCalledWith('cf1');
    expect(q('[data-testid="overview-pilotage"]')).not.toBeNull();
    expect(q('[data-testid="overview-fil"]')).not.toBeNull();
    expect(q('[data-testid="overview-deep"]')).not.toBeNull();
  });

  it('affiche le repère Aujourd\'hui séparant passé et futur', async () => {
    await setup();
    expect(q('[data-testid="overview-today"]')).not.toBeNull();
    expect(component.pastEvents().length).toBe(1);
    expect(component.futureEvents().length).toBe(1);
  });

  it('replie le passé au-delà de 8 événements', async () => {
    const many = Array.from({ length: 12 }, (_, i) =>
      event({ date: `2025-01-${(i % 28) + 1}`, titre: `E${i}`, temps: 'PASSE' }));
    await setup(fullResponse({ fil: many }));
    expect(component.pastEvents().length).toBe(12);
    expect(component.visiblePastEvents().length).toBe(component.PAST_THRESHOLD);
    expect(component.hiddenPastCount()).toBe(12 - component.PAST_THRESHOLD);
    expect(q('[data-testid="past-collapsed"]')).not.toBeNull();

    component.togglePast();
    fixture.detectChanges();
    expect(component.visiblePastEvents().length).toBe(12);
    expect(component.hiddenPastCount()).toBe(0);
  });

  it('tronque le bloc attention à 5 items avec « +N autres »', async () => {
    const attention = Array.from({ length: 8 }, (_, i) => ({
      type: 'PIECE_MANQUANTE' as const, label: `P${i}`, urgency: 'LOW',
      action: { kind: 'NAVIGATE' as const, targetTab: 'dossier' as const },
    }));
    await setup(fullResponse({ attention, attentionTotal: 8 }));
    expect(component.visibleAttention().length).toBe(component.ATTENTION_MAX);
    expect(component.hiddenAttentionCount()).toBe(8 - component.ATTENTION_MAX);
    expect(q('[data-testid="attention-more"]')).not.toBeNull();
  });

  it('filtre le fil par voie via les chips (défaut = Tout)', async () => {
    await setup(fullResponse({
      fil: [
        event({ titre: 'Proc', voie: 'PROCEDURE', temps: 'PASSE' }),
        event({ titre: 'Ech', voie: 'ECHANGE', temps: 'PASSE' }),
      ],
    }));
    expect(component.voieFilter()).toBe('ALL');
    expect(component.pastEvents().length).toBe(2);

    component.selectVoie('ECHANGE');
    fixture.detectChanges();
    expect(component.pastEvents().length).toBe(1);
    expect(component.pastEvents()[0].titre).toBe('Ech');
  });

  it('accordéon pièces fermé par défaut, déplié appelle preview puis download', async () => {
    const evt = event({
      titre: 'Vague', voie: 'PIECES', temps: 'PASSE',
      attachments: [{ documentId: 'd1', filename: 'piece.pdf' }],
    });
    await setup(fullResponse({ fil: [evt] }));

    expect(component.isEventExpanded(evt)).toBe(false);
    expect(q('[data-testid="event-attachments"]')).toBeNull();

    component.toggleEvent(evt);
    fixture.detectChanges();
    expect(component.isEventExpanded(evt)).toBe(true);
    expect(q('[data-testid="event-attachments"]')).not.toBeNull();

    component.previewDocument({ documentId: 'd1', filename: 'piece.pdf' });
    expect(documentServiceSpy.preview).toHaveBeenCalledWith('cf1', 'd1');
    expect(component.downloadHref('d1')).toContain('/download');
    expect(documentServiceSpy.downloadUrl).toHaveBeenCalledWith('cf1', 'd1');
  });

  it('une action émet un routage vers l\'onglet/ancre cible', async () => {
    await setup();
    const navs: OverviewNavigation[] = [];
    component.navigate.subscribe(n => navs.push(n));

    component.runAction({ kind: 'VALIDATE_DEADLINE', targetTab: 'suivi', anchor: 'section-deadlines' });
    expect(navs).toEqual([{ targetTab: 'suivi', anchor: 'section-deadlines', route: null }]);
  });

  it('SF-289-08 — « Relancer l\'analyse » émet relaunchAnalysis et NE navigue PAS', async () => {
    await setup();
    const navs: OverviewNavigation[] = [];
    let relaunched = 0;
    component.navigate.subscribe(n => navs.push(n));
    component.relaunchAnalysis.subscribe(() => relaunched++);

    // Reproduit le contrat backend post-fix : aucune URL d'API dans `route`.
    component.runAction({ kind: 'RELAUNCH_ANALYSIS', targetTab: 'analyse', anchor: null, route: null });

    expect(relaunched).toBe(1);
    expect(navs).toEqual([]); // pas de navigateByUrl vers /api/.../re-analyze
  });

  it('un raccourci émet un routage ; l\'export appelle le service', async () => {
    await setup();
    const navs: OverviewNavigation[] = [];
    component.navigate.subscribe(n => navs.push(n));

    component.goToShortcut('decision', 'section-outils-decisionnels');
    expect(navs[0]).toEqual({ targetTab: 'decision', anchor: 'section-outils-decisionnels', route: null });

    component.exportZip();
    expect(caseFileServiceSpy.exportZip).toHaveBeenCalledWith('cf1');
  });

  it('état vide honnête sur dossier neuf', async () => {
    await setup({
      pilotage: {
        currentPhaseLabel: null, awaitingParty: null, nextDeadline: null,
        sante: { admissibility: null, prescriptionStatus: null, riskLevelAvocat: null },
        analysisStale: false, pendingPiecesCount: 0, toolsCalculated: 0, toolsVisible: 0,
      },
      attention: [], attentionTotal: 0, fil: [],
    });
    expect(component.isEmpty()).toBe(true);
    expect(q('[data-testid="overview-empty"]')).not.toBeNull();
  });

  it('erreur de chargement → message non bloquant + relance', async () => {
    await setup(new Error('500'));
    expect(component.loadError()).toBe(true);
    expect(q('[data-testid="overview-error"]')).not.toBeNull();

    // Relance : si l'appel réussit cette fois, le contenu s'affiche.
    overviewServiceSpy.getOverview.mockReturnValue(of(fullResponse()));
    component.loadOverview();
    fixture.detectChanges();
    expect(component.loadError()).toBe(false);
    expect(q('[data-testid="overview-pilotage"]')).not.toBeNull();
  });

  // ── SF-289-05 — drawer d'aperçu ──

  it('aperçu ouvre le drawer et pointe l\'URL inline sur le flux /download', async () => {
    await setup();
    component.previewDocument({ documentId: 'd1', filename: 'piece.pdf' });
    fixture.detectChanges();
    expect(component.previewDoc()?.documentId).toBe('d1');
    expect(documentServiceSpy.preview).toHaveBeenCalledWith('cf1', 'd1');
    expect(component.previewError()).toBe(false);
    expect(component.previewUrl()).not.toBeNull();
    expect(documentServiceSpy.downloadUrl).toHaveBeenCalledWith('cf1', 'd1');
    expect(q('[data-testid="overview-preview-frame"]')).not.toBeNull();
  });

  it('aperçu d\'une pièce supprimée (404) → drawer ouvert avec message, pas d\'onglet blanc', async () => {
    await setup();
    documentServiceSpy.preview.mockReturnValue(throwError(() => new Error('404')));
    component.previewDocument({ documentId: 'gone', filename: 'x.pdf' });
    fixture.detectChanges();
    expect(component.previewDoc()?.documentId).toBe('gone');
    expect(component.previewError()).toBe(true);
    expect(component.previewUrl()).toBeNull();
    expect(q('[data-testid="overview-preview-error"]')).not.toBeNull();
  });

  it('ferme le drawer (retour au fil)', async () => {
    await setup();
    component.previewDocument({ documentId: 'd1', filename: 'piece.pdf' });
    component.closePreview();
    expect(component.previewDoc()).toBeNull();
    expect(component.previewUrl()).toBeNull();
    expect(component.previewError()).toBe(false);
  });

  // ── SF-289-02 — actions inline (question IA / pièce manquante) ──

  function question(over: Partial<AttentionItem> = {}): AttentionItem {
    return {
      type: 'QUESTION_IA', label: '1 question sans réponse', urgency: 'SOON',
      action: { kind: 'ANSWER_QUESTION', targetTab: 'analyse', anchor: 'ai-questions' },
      questionId: 'q1', pieceLibelle: null, ...over,
    };
  }
  function piece(over: Partial<AttentionItem> = {}): AttentionItem {
    return {
      type: 'PIECE_MANQUANTE', label: 'Pièce à obtenir : Contrat', urgency: 'SOON',
      action: { kind: 'MARK_PIECE', targetTab: 'analyse', anchor: 'pieces-manquantes' },
      questionId: null, pieceLibelle: 'Contrat de travail', ...over,
    };
  }

  it('QUESTION_IA avec id : ouvre le champ inline puis POST la réponse + refresh', async () => {
    await setup(fullResponse({ attention: [question()], attentionTotal: 1 }));
    const item = component.visibleAttention()[0];
    expect(component.isInlineActionable(item)).toBe(true);

    component.onAttentionAction(item, 0);
    fixture.detectChanges();
    expect(component.answerOpenIndex()).toBe(0);
    expect(q('[data-testid="attention-answer"]')).not.toBeNull();

    component.answerDraft.set('Ma réponse');
    overviewServiceSpy.getOverview.mockClear();
    component.submitAnswer(item, 0);

    expect(answerServiceSpy.submitAnswer).toHaveBeenCalledWith('q1', 'Ma réponse');
    expect(overviewServiceSpy.getOverview).toHaveBeenCalledWith('cf1'); // refresh
    expect(component.answerOpenIndex()).toBe(-1);
  });

  it('QUESTION_IA réponse en échec → snack non bloquant, champ conservé', async () => {
    await setup(fullResponse({ attention: [question()], attentionTotal: 1 }));
    const item = component.visibleAttention()[0];
    component.onAttentionAction(item, 0);
    component.answerDraft.set('Ma réponse');
    answerServiceSpy.submitAnswer.mockReturnValue(throwError(() => new Error('500')));
    overviewServiceSpy.getOverview.mockClear();

    component.submitAnswer(item, 0);
    expect(snackErrors.length).toBe(1);
    expect(overviewServiceSpy.getOverview).not.toHaveBeenCalled();
  });

  // SF-289-07 — plusieurs questions sans réponse → inline une à une (le backend
  // porte la 1ʳᵉ : id + texte), avec affichage du texte de la question courante.
  it('QUESTION_IA multiple : inline (1ʳᵉ question), texte affiché, pas de routage', async () => {
    const multi = question({
      label: '3 questions sans réponse', questionId: 'q1', questionText: 'Le préavis a-t-il été exécuté ?',
    });
    await setup(fullResponse({ attention: [multi], attentionTotal: 1 }));
    const item = component.visibleAttention()[0];
    expect(component.isInlineActionable(item)).toBe(true);

    const navs: OverviewNavigation[] = [];
    component.navigate.subscribe(n => navs.push(n));
    component.onAttentionAction(item, 0);
    fixture.detectChanges();

    expect(navs.length).toBe(0); // pas de routage
    expect(component.answerOpenIndex()).toBe(0);
    expect(q('[data-testid="attention-answer-question"]')?.textContent?.trim())
      .toBe('Le préavis a-t-il été exécuté ?');
  });

  // SF-194-04 — le clic « Marquer obtenue » ouvre d'abord le sélecteur de document
  // (charge la liste), puis la confirmation PUT OBTENUE en liant le documentId choisi.
  it('PIECE_MANQUANTE : ouvre le sélecteur de document puis PUT OBTENUE + documentId + refresh', async () => {
    await setup(fullResponse({ attention: [piece()], attentionTotal: 1 }));
    const item = component.visibleAttention()[0];
    expect(component.isInlineActionable(item)).toBe(true);

    overviewServiceSpy.getOverview.mockClear();
    component.onAttentionAction(item, 0);
    fixture.detectChanges();

    expect(component.pieceDocOpenIndex()).toBe(0);
    expect(documentServiceSpy.list).toHaveBeenCalledWith('cf1');
    expect(q('[data-testid="attention-piece-doc"]')).not.toBeNull();
    expect(pieceServiceSpy.updateStatus).not.toHaveBeenCalled(); // pas encore confirmé

    component.selectedDocId.set('doc1');
    component.markPieceObtained(item, 0);

    expect(pieceServiceSpy.updateStatus).toHaveBeenCalledWith('cf1', 'Contrat de travail', 'OBTENUE', { documentId: 'doc1' });
    expect(overviewServiceSpy.getOverview).toHaveBeenCalledWith('cf1'); // refresh
    expect(component.pieceDocOpenIndex()).toBe(-1);
  });

  it('PIECE_MANQUANTE : confirmation sans document lié → PUT OBTENUE documentId null', async () => {
    await setup(fullResponse({ attention: [piece()], attentionTotal: 1 }));
    const item = component.visibleAttention()[0];
    component.onAttentionAction(item, 0);
    component.markPieceObtained(item, 0);

    expect(pieceServiceSpy.updateStatus).toHaveBeenCalledWith('cf1', 'Contrat de travail', 'OBTENUE', { documentId: null });
  });

  it('PIECE_MANQUANTE en échec → snack non bloquant, pas de refresh, sélecteur conservé', async () => {
    await setup(fullResponse({ attention: [piece()], attentionTotal: 1 }));
    const item = component.visibleAttention()[0];
    pieceServiceSpy.updateStatus.mockReturnValue(throwError(() => new Error('500')));
    component.onAttentionAction(item, 0);
    overviewServiceSpy.getOverview.mockClear();

    component.markPieceObtained(item, 0);
    expect(snackErrors.length).toBe(1);
    expect(overviewServiceSpy.getOverview).not.toHaveBeenCalled();
  });

  // ── SF-289-04 — export PDF du journal ──

  it('export journal appelle le générateur PDF avec l\'overview + caseFileId', async () => {
    await setup();
    component.caseTitle = 'Dossier Durand';
    component.exportJournal();
    expect(journalPdfSpy.export).toHaveBeenCalledWith(component.overview(), 'Dossier Durand', 'cf1');
  });

  it('export journal désactivé si overview non chargé', async () => {
    await setup(new Error('500'));
    expect(component.overview()).toBeNull();
    component.exportJournal();
    expect(journalPdfSpy.export).not.toHaveBeenCalled();
  });
});
