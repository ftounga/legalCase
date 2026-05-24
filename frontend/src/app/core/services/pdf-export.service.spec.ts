import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PdfExportService } from './pdf-export.service';

// SF-98-51 — stub des modules `pdfmake` chargés dynamiquement par le service,
// pour intercepter `createPdf(...).download(...)` sans générer de vrai PDF.
const pdfDownloadSpy = jest.fn();
const pdfCreateSpy = jest.fn(() => ({ download: pdfDownloadSpy }));
jest.mock('pdfmake/build/pdfmake', () => ({
  __esModule: true,
  default: { vfs: {}, createPdf: (...args: unknown[]) => pdfCreateSpy(...args) },
}));
jest.mock('pdfmake/build/vfs_fonts', () => ({
  __esModule: true,
  default: { pdfMake: { vfs: {} } },
}));
import { CaseAnalysisResult } from '../models/case-analysis.model';
import { CaseFile } from '../models/case-file.model';
import { RetainedPisteAlignment } from '../models/retained-piste-alignment.model';
import { ProcedureCheckAlignment } from '../models/procedure-check-alignment.model';
import { PieceManquanteAlignment } from '../models/piece-manquante-alignment.model';
import { RisqueAlignment } from '../models/risque-alignment.model';
import { AiQuestionAlignment } from '../models/ai-question-alignment.model';

const mockCaseFile: Partial<CaseFile> = { id: '1', title: 'Affaire Dupont c/ SA Renault' };

const mockSynthesis: CaseAnalysisResult = {
  id: 'syn-1',
  version: 2,
  analysisType: 'ENRICHED',
  status: 'COMPLETED',
  timeline: [
    { date: '01/01/2024', evenement: 'Licenciement notifié' },
    { date: '15/01/2024', evenement: 'Lettre de contestation envoyée' },
  ],
  faits: [
    { texte: 'Salarié embauché en 2018', source: null, extrait: null },
    { texte: 'Licencié sans cause réelle', source: null, extrait: null },
  ],
  pointsJuridiques: [
    { texte: 'Article L1232-1 Code du travail', source: null, extrait: null },
    { texte: 'Absence de cause réelle et sérieuse', source: null, extrait: null },
  ],
  risques: [
    { texte: 'Condamnation à indemnités', source: null, extrait: null },
    { texte: 'Risque de requalification', source: null, extrait: null },
  ],
  questionsOuvertes: ['Convention collective applicable ?'],
  piecesManquantes: [],
  riskLevel: null,
  riskScore: null,
  modelUsed: 'claude-3',
  updatedAt: '2026-03-24T10:00:00Z',
};

describe('PdfExportService', () => {
  let service: PdfExportService;
  let snackBar: jest.Mocked<MatSnackBar>;

  beforeEach(() => {
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    TestBed.configureTestingModule({
      providers: [
        PdfExportService,
        { provide: MatSnackBar, useValue: snackBar },
      ],
    });
    service = TestBed.inject(PdfExportService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('buildDocument() should return a valid pdfmake document object', () => {
    const doc = service.buildDocument(mockCaseFile as CaseFile, mockSynthesis) as any;
    expect(doc).toBeTruthy();
    expect(doc.content).toBeDefined();
    expect(Array.isArray(doc.content)).toBe(true);
    expect(doc.pageSize).toBe('A4');
  });

  it('buildDocument() should include all non-empty sections', () => {
    const doc = service.buildDocument(mockCaseFile as CaseFile, mockSynthesis) as any;
    const contentStr = JSON.stringify(doc.content);
    expect(contentStr).toContain('Chronologie');
    expect(contentStr).toContain('Faits');
    expect(contentStr).toContain('Points juridiques');
    expect(contentStr).toContain('Risques');
    expect(contentStr).toContain('Questions ouvertes');
  });

  it('buildDocument() should omit sections with empty arrays', () => {
    const emptySynthesis: CaseAnalysisResult = {
      ...mockSynthesis,
      timeline: [],
      questionsOuvertes: [],
    };
    const doc = service.buildDocument(mockCaseFile as CaseFile, emptySynthesis) as any;
    const contentStr = JSON.stringify(doc.content);
    expect(contentStr).not.toContain('Chronologie');
    expect(contentStr).not.toContain('Questions ouvertes');
    expect(contentStr).toContain('Faits');
  });

  it('buildFileName() should slugify the title correctly', () => {
    const name = service.buildFileName('Affaire Dupont c/ SA Renault — 2024', mockSynthesis);
    expect(name).toMatch(/^synthese-affaire-dupont-c-sa-renault-2024-v2-\d{4}-\d{2}-\d{2}\.pdf$/);
  });

  it('buildFileName() should handle accented characters', () => {
    const name = service.buildFileName('Licenciement économique — Müller', mockSynthesis);
    expect(name).not.toMatch(/[àáâãéèêëîïôùûüç]/i);
    expect(name).toContain('synthese-');
    expect(name).toContain('.pdf');
  });

  it('buildFileName() should use "export" as fallback when title is empty', () => {
    const name = service.buildFileName('', mockSynthesis);
    expect(name).toMatch(/^synthese-export-v2-\d{4}-\d{2}-\d{2}\.pdf$/);
  });

  // --- exportChecklist ---

  const mockChecks = [
    { id: 'c1', ordre: 2, description: 'Vérifier la clause', statut: 'VERIFIED' as const, raison: 'Conforme selon contrat' },
    { id: 'c2', ordre: 1, description: 'Vérifier la prescription', statut: 'NON_COMPLIANT' as const, raison: null },
    { id: 'c3', ordre: 3, description: 'Contrôler délai', statut: 'TO_CHECK' as const, raison: undefined },
  ];

  it('buildChecklistDocument() should return a valid pdfmake document', () => {
    const doc = service.buildChecklistDocument(mockCaseFile as CaseFile, mockChecks) as any;
    expect(doc).toBeTruthy();
    expect(doc.pageSize).toBe('A4');
    expect(Array.isArray(doc.content)).toBe(true);
  });

  it('buildChecklistDocument() should sort checks by ordre', () => {
    const doc = service.buildChecklistDocument(mockCaseFile as CaseFile, mockChecks) as any;
    const contentStr = JSON.stringify(doc.content);
    const idx1 = contentStr.indexOf('Vérifier la prescription');
    const idx2 = contentStr.indexOf('Vérifier la clause');
    const idx3 = contentStr.indexOf('Contrôler délai');
    expect(idx1).toBeLessThan(idx2);
    expect(idx2).toBeLessThan(idx3);
  });

  it('buildChecklistDocument() should include "Raison IA" for checks with raison', () => {
    const doc = service.buildChecklistDocument(mockCaseFile as CaseFile, mockChecks) as any;
    const contentStr = JSON.stringify(doc.content);
    expect(contentStr).toContain('Raison IA');
    expect(contentStr).toContain('Conforme selon contrat');
  });

  it('buildChecklistDocument() should not include "Raison IA" for checks without raison', () => {
    const checksNoRaison = [
      { id: 'c1', ordre: 1, description: 'Un point', statut: 'TO_CHECK' as const, raison: null },
    ];
    const doc = service.buildChecklistDocument(mockCaseFile as CaseFile, checksNoRaison) as any;
    const contentStr = JSON.stringify(doc.content);
    expect(contentStr).not.toContain('Raison IA');
  });

  it('buildChecklistFileName() should slugify the title correctly', () => {
    const name = service.buildChecklistFileName('Affaire Dupont c/ SA Renault');
    expect(name).toMatch(/^checklist-affaire-dupont-c-sa-renault-\d{4}-\d{2}-\d{2}\.pdf$/);
  });

  it('buildChecklistFileName() should use "dossier" as fallback when title is empty', () => {
    const name = service.buildChecklistFileName('');
    expect(name).toMatch(/^checklist-dossier-\d{4}-\d{2}-\d{2}\.pdf$/);
  });

  // --- SF-DT-01-01 : section indemnités dans l'export PDF ---

  const mockEstimate = {
    indemnite: 8050,
    salaireReference: 2800,
    ancienneteAnnees: 6,
    ancienneteMois: 4,
    typeRupture: 'LICENCIEMENT',
    plafondMinMois: 3,
    plafondMaxMois: 7,
    donneesPartielles: false,
  };

  it('PDF-COMP-01: buildDocument() inclut section indemnités si compensationEstimate présent', () => {
    const synthWithComp = { ...mockSynthesis, compensationEstimate: mockEstimate };
    const doc = service.buildDocument(mockCaseFile as CaseFile, synthWithComp) as any;
    const contentStr = JSON.stringify(doc.content);
    expect(contentStr).toContain('Indemnités estimées');
    expect(contentStr).toContain('Licenciement');
  });

  it('PDF-COMP-02: buildDocument() n\'inclut pas section indemnités si compensationEstimate null', () => {
    const synthNoComp = { ...mockSynthesis, compensationEstimate: null };
    const doc = service.buildDocument(mockCaseFile as CaseFile, synthNoComp) as any;
    const contentStr = JSON.stringify(doc.content);
    expect(contentStr).not.toContain('Indemnités estimées');
  });

  it('PDF-COMP-03: buildDocument() inclut avertissement données partielles si donneesPartielles=true', () => {
    const partial = { ...mockEstimate, donneesPartielles: true };
    const synthPartial = { ...mockSynthesis, compensationEstimate: partial };
    const doc = service.buildDocument(mockCaseFile as CaseFile, synthPartial) as any;
    const contentStr = JSON.stringify(doc.content);
    expect(contentStr).toContain('Données partielles');
  });

  it('PDF-COMP-04: buildDocument() n\'inclut pas avertissement si donneesPartielles=false', () => {
    const synthComp = { ...mockSynthesis, compensationEstimate: mockEstimate };
    const doc = service.buildDocument(mockCaseFile as CaseFile, synthComp) as any;
    const contentStr = JSON.stringify(doc.content);
    expect(contentStr).not.toContain('Données partielles');
  });

  // --- SF-FA-02-02 : section pension alimentaire dans l'export PDF ---

  const mockPensionEstimate = {
    montantMin: 396, montantMax: 484, revenus: 2000, nbEnfants: 2,
    modeGarde: 'EXCLUSIVE' as const, pays: 'FRANCE' as const, donneesPartielles: false,
  };

  it('PDF-PA-01: buildDocument() inclut section pension alimentaire si pensionAlimentaireEstimate présent', () => {
    const synthWithPA = { ...mockSynthesis, pensionAlimentaireEstimate: mockPensionEstimate };
    const doc = service.buildDocument(mockCaseFile as CaseFile, synthWithPA) as any;
    const contentStr = JSON.stringify(doc.content);
    expect(contentStr).toContain('Pension alimentaire indicative');
  });

  it('PDF-PA-02: buildDocument() n\'inclut pas section pension si pensionAlimentaireEstimate null', () => {
    const synthNoPA = { ...mockSynthesis, pensionAlimentaireEstimate: null };
    const doc = service.buildDocument(mockCaseFile as CaseFile, synthNoPA) as any;
    const contentStr = JSON.stringify(doc.content);
    expect(contentStr).not.toContain('Pension alimentaire indicative');
  });

  // --- SF-FA-01-02 : section prestation compensatoire dans l'export PDF ---

  const mockPrestationEstimate = {
    montantMin: 36720, montantMax: 49680, ecartRevenus: 1500, dureeMarriage: 10,
    pays: 'FRANCE' as const, donneesPartielles: false,
  };

  it('PDF-PC-01: buildDocument() inclut section prestation compensatoire si estimate présent', () => {
    const synthWithPC = { ...mockSynthesis, prestationCompensatoireEstimate: mockPrestationEstimate };
    const doc = service.buildDocument(mockCaseFile as CaseFile, synthWithPC) as any;
    expect(JSON.stringify(doc.content)).toContain('Prestation compensatoire indicative');
  });

  it('PDF-PC-02: buildDocument() n\'inclut pas section prestation si estimate null', () => {
    const synthNoPC = { ...mockSynthesis, prestationCompensatoireEstimate: null };
    const doc = service.buildDocument(mockCaseFile as CaseFile, synthNoPC) as any;
    expect(JSON.stringify(doc.content)).not.toContain('Prestation compensatoire indicative');
  });

  // --- SF-DT-04-03 : export PDF fiche prud'homale ---

  const mockFiche = {
    id: 'f-1',
    demandeur: { nom: 'Dupont', prenom: 'Jean', adresse: '12 rue de la Paix', telephone: '0600000000', email: 'j@d.fr', profession: 'Salarié' },
    defendeur: { nom: 'Renault SAS', adresse: '75 avenue', siret: '12345678901234', representant: 'M. Martin' },
    demandes: [
      { label: 'Indemnité licenciement', montant: 5000 },
      { label: 'Préavis', montant: null },
    ],
    faitsTexte: 'Licenciement abusif sans cause réelle.',
    moyensDroitTexte: 'Art. L1235-3 Code du travail.',
    piecesList: [
      { numero: 1, nom: 'contrat.pdf' },
      { numero: 2, nom: 'lettre-licenciement.pdf' },
    ],
    updatedAt: '2026-04-04T10:00:00Z',
  };

  it('PDF-FICHE-01: buildPrudhomeFicheDocument() retourne un document pdfmake valide avec toutes les sections', () => {
    const doc = service.buildPrudhomeFicheDocument(mockFiche, 'Affaire Dupont') as any;
    const s = JSON.stringify(doc.content);
    expect(doc.pageSize).toBe('A4');
    expect(s).toContain('Demandeur');
    expect(s).toContain('Défendeur');
    expect(s).toContain('Demandes chiffrées');
    expect(s).toContain('Exposé des faits');
    expect(s).toContain('Moyens de droit');
    expect(s).toContain('Bordereau de pièces');
  });

  it('PDF-FICHE-02: tableau demandes contient les colonnes Intitulé / Montant', () => {
    const doc = service.buildPrudhomeFicheDocument(mockFiche, 'Affaire') as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('Intitulé');
    expect(s).toContain('Montant');
    expect(s).toContain('Indemnité licenciement');
  });

  it('PDF-FICHE-03: montant null affiché "—"', () => {
    const doc = service.buildPrudhomeFicheDocument(mockFiche, 'Affaire') as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('—');
  });

  it('PDF-FICHE-04: buildPrudhomeFicheFileName() slugifie le titre correctement', () => {
    const name = service.buildPrudhomeFicheFileName('Affaire Dupont c/ SA Renault');
    expect(name).toMatch(/^fiche-prudhomale-affaire-dupont-c-sa-renault-\d{4}-\d{2}-\d{2}\.pdf$/);
  });

  // --- SF-IM-01-03 : export PDF checklist immigration ---

  const mockImmigrationChecklist = {
    caseFileId: 'cf-1',
    titreType: 'VISA_ETUDIANT',
    country: 'FRANCE',
    pieces: [
      { label: 'Passeport', statut: 'PRESENT' as const },
      { label: 'Justificatif hébergement', statut: 'ABSENT' as const },
      { label: 'Lettre motivation', statut: 'INCONNU' as const },
    ],
  };

  it('IM-PDF-01: buildImmigrationChecklistDocument() retourne un document pdfmake valide', () => {
    const doc = service.buildImmigrationChecklistDocument(mockImmigrationChecklist, 'Dossier Test') as any;
    expect(doc).toBeTruthy();
    expect(doc.pageSize).toBe('A4');
    expect(Array.isArray(doc.content)).toBe(true);
  });

  it('IM-PDF-02: buildImmigrationChecklistDocument() contient le titre dossier, le type de titre et le pays', () => {
    const doc = service.buildImmigrationChecklistDocument(mockImmigrationChecklist, 'Dossier Test') as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('Dossier Test');
    expect(s).toContain('Visa étudiant');
    expect(s).toContain('France');
  });

  it('IM-PDF-03: buildImmigrationChecklistDocument() contient le résumé compteurs', () => {
    const doc = service.buildImmigrationChecklistDocument(mockImmigrationChecklist, 'Dossier Test') as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('1 présente(s)');
    expect(s).toContain('1 absente(s)');
    expect(s).toContain('1 inconnue(s)');
  });

  it('IM-PDF-04: buildImmigrationChecklistDocument() avec 0 pièces affiche "Aucune pièce"', () => {
    const empty = { ...mockImmigrationChecklist, pieces: [] };
    const doc = service.buildImmigrationChecklistDocument(empty, 'Dossier') as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('Aucune pièce');
  });

  it('IM-PDF-05: buildImmigrationChecklistFileName() génère le bon slug et la bonne date', () => {
    const name = service.buildImmigrationChecklistFileName('Dossier Immigration Test');
    expect(name).toMatch(/^checklist-pieces-dossier-immigration-test-\d{4}-\d{2}-\d{2}\.pdf$/);
  });

  // --- SF-FA-04-02 : liquidation communauté PDF ---

  it('PDF-LC-01: buildDocument() inclut section liquidation si liquidationCommunaute présent', () => {
    const synWithLiquidation: CaseAnalysisResult = {
      ...mockSynthesis,
      liquidationCommunaute: {
        regimeMatrimonial: 'COMMUNAUTE_LEGALE',
        actifCommun: [{ libelle: 'Résidence principale', valeur: 350000 }],
        biensPropresEpouxA: [],
        biensPropresEpouxB: [{ libelle: 'Appartement hérité', valeur: 120000 }],
        passifCommun: [{ libelle: 'Crédit immobilier', valeur: 80000 }],
      },
    };
    const doc = service.buildDocument(mockCaseFile as CaseFile, synWithLiquidation) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('Liquidation de communauté');
    expect(s).toContain('Résidence principale');
    expect(s).toContain('Communauté légale');
  });

  it('PDF-LC-02: buildDocument() n\'inclut pas section liquidation si liquidationCommunaute null', () => {
    const synWithoutLiquidation: CaseAnalysisResult = {
      ...mockSynthesis,
      liquidationCommunaute: null,
    };
    const doc = service.buildDocument(mockCaseFile as CaseFile, synWithoutLiquidation) as any;
    const s = JSON.stringify(doc.content);
    expect(s).not.toContain('Liquidation de communauté');
  });

  // --- F-192 SF-192-03 : section « Stratégies retenues » dans l'export PDF synthèse ---

  const pisteAligned: RetainedPisteAlignment = {
    pisteId: 'p-1',
    texte: 'Demander un titre Passeport Talent — Chercheur',
    baseJuridique: 'L.421-14 CESEDA',
    horizonTemporel: '3 mois',
    conditions: ['Diplôme master', 'Convention d\'accueil'],
    toolIdCible: 'F-IM-05-arbre-decisionnel-titre',
    matchStatus: 'ALIGNED',
  };

  const pisteDivergent: RetainedPisteAlignment = {
    pisteId: 'p-2',
    texte: 'Engager un recours hiérarchique',
    baseJuridique: 'L.214-1 CESEDA',
    horizonTemporel: '2 mois',
    conditions: [],
    toolIdCible: 'F-IM-06-recours',
    matchStatus: 'DIVERGENT',
  };

  const pisteNotAnalyzed: RetainedPisteAlignment = {
    pisteId: 'p-3',
    texte: 'Stratégie X',
    baseJuridique: null,
    horizonTemporel: null,
    conditions: [],
    toolIdCible: 'F-IM-05-arbre-decisionnel-titre',
    matchStatus: 'NOT_ANALYZED',
  };

  const pisteNoTargetTool: RetainedPisteAlignment = {
    pisteId: 'p-4',
    texte: 'Demander un divorce par consentement mutuel',
    baseJuridique: 'art. 230 Code civil',
    horizonTemporel: '6 mois',
    conditions: ['Accord des deux époux'],
    toolIdCible: null,
    matchStatus: 'NO_TARGET_TOOL',
  };

  const labelResolver = (toolId: string): string | null => {
    const labels: Record<string, string> = {
      'F-IM-05-arbre-decisionnel-titre': 'TITRE DE SÉJOUR RECOMMANDÉ',
      'F-IM-06-recours': 'RECOURS IMMIGRATION',
    };
    return labels[toolId] ?? null;
  };

  it('SF-192-03 CA-02: buildDocument(_, _, []) → aucune section Stratégies retenues', () => {
    const doc = service.buildDocument(mockCaseFile as CaseFile, mockSynthesis, []) as any;
    const s = JSON.stringify(doc.content);
    expect(s).not.toContain('Stratégies retenues');
  });

  it('SF-192-03 CA-02: buildDocument(_, _, undefined) → aucune section Stratégies retenues', () => {
    const doc = service.buildDocument(mockCaseFile as CaseFile, mockSynthesis) as any;
    const s = JSON.stringify(doc.content);
    expect(s).not.toContain('Stratégies retenues');
  });

  it('SF-192-03 CA-03: piste ALIGNED → badge ✅ Stratégie alignée avec l\'outil <label>', () => {
    const doc = service.buildDocument(mockCaseFile as CaseFile, mockSynthesis, [pisteAligned], labelResolver) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('🎯 Stratégies retenues');
    expect(s).toContain('Passeport Talent');
    expect(s).toContain('✅ Stratégie alignée avec l\'outil TITRE DE SÉJOUR RECOMMANDÉ');
  });

  it('SF-192-03 CA-04: piste DIVERGENT → badge ⚠️ Stratégie divergente avec l\'outil <label>', () => {
    const doc = service.buildDocument(mockCaseFile as CaseFile, mockSynthesis, [pisteDivergent], labelResolver) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('⚠️ Stratégie divergente avec l\'outil RECOURS IMMIGRATION');
  });

  it('SF-192-03 CA-05: piste NOT_ANALYZED → badge ⏳ Outil <label> non encore analysé', () => {
    const doc = service.buildDocument(mockCaseFile as CaseFile, mockSynthesis, [pisteNotAnalyzed], labelResolver) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('⏳ Outil TITRE DE SÉJOUR RECOMMANDÉ non encore analysé');
  });

  it('SF-192-03 CA-06: piste NO_TARGET_TOOL → texte affiché, AUCUN badge alignement', () => {
    const doc = service.buildDocument(mockCaseFile as CaseFile, mockSynthesis, [pisteNoTargetTool], labelResolver) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('🎯 Stratégies retenues');
    expect(s).toContain('Demander un divorce par consentement mutuel');
    expect(s).not.toContain('✅ Stratégie alignée');
    expect(s).not.toContain('⚠️ Stratégie divergente');
    expect(s).not.toContain('⏳ Outil');
  });

  it('SF-192-03 CA-01: section insérée APRÈS la page de garde, AVANT la timeline', () => {
    const synWithTimeline: CaseAnalysisResult = {
      ...mockSynthesis,
      timeline: [{ date: '01/01/2024', evenement: 'Événement test' }],
    };
    const doc = service.buildDocument(mockCaseFile as CaseFile, synWithTimeline, [pisteAligned], labelResolver) as any;
    const content: any[] = doc.content;
    const flat = JSON.stringify(content);
    const stratIdx = flat.indexOf('🎯 Stratégies retenues');
    const timelineIdx = flat.indexOf('Chronologie');
    const coverIdx = flat.indexOf('Synthèse'); // page de garde contient "Synthèse enrichie/initiale"
    expect(stratIdx).toBeGreaterThan(-1);
    expect(timelineIdx).toBeGreaterThan(-1);
    expect(coverIdx).toBeGreaterThan(-1);
    expect(coverIdx).toBeLessThan(stratIdx);
    expect(stratIdx).toBeLessThan(timelineIdx);
  });

  it('SF-192-03 CA-07: conditions affichées en liste à puces (ul pdfmake)', () => {
    const doc = service.buildDocument(mockCaseFile as CaseFile, mockSynthesis, [pisteAligned], labelResolver) as any;
    const flat = JSON.stringify(doc.content);
    expect(flat).toContain('"ul"');
    expect(flat).toContain('Diplôme master');
    expect(flat).toContain("Convention d'accueil");
  });

  it('SF-192-03 CA-08: base juridique en JetBrainsMono italique taille 9', () => {
    const doc = service.buildDocument(mockCaseFile as CaseFile, mockSynthesis, [pisteAligned], labelResolver) as any;
    const flat = JSON.stringify(doc.content);
    expect(flat).toContain('"font":"JetBrainsMono"');
    // L.421-14 CESEDA est rendu avec font JetBrainsMono + italics + fontSize 9
    expect(flat).toContain('L.421-14 CESEDA');
    // Recherche du bloc baseJuridique avec ses props caractéristiques
    expect(flat).toMatch(/"text":"L\.421-14 CESEDA","font":"JetBrainsMono","fontSize":9,"italics":true/);
  });

  it('SF-192-03: lookup label échoue → toolId brut affiché', () => {
    const noResolver = (_toolId: string): string | null => null;
    const doc = service.buildDocument(mockCaseFile as CaseFile, mockSynthesis, [pisteAligned], noResolver) as any;
    const flat = JSON.stringify(doc.content);
    expect(flat).toContain('F-IM-05-arbre-decisionnel-titre');
  });

  it('SF-192-03: plusieurs pistes → séparateur navy entre chaque', () => {
    const doc = service.buildDocument(mockCaseFile as CaseFile, mockSynthesis, [pisteAligned, pisteDivergent], labelResolver) as any;
    const flat = JSON.stringify(doc.content);
    expect(flat).toContain('Passeport Talent');
    expect(flat).toContain('Engager un recours hiérarchique');
    // Le séparateur est un canvas line — on vérifie qu'il y a bien au moins une ligne navy entre les pistes
    expect(flat).toContain('"type":"line"');
  });

  // --- F-193 SF-193-03 : section « Conformité procédurale validée par votre avocat » ---

  const checkAligned: ProcedureCheckAlignment = {
    checkId: 'c-1',
    libelle: 'Motif principal du séjour confirmé : TRAVAIL',
    critereCode: 'IM05_MOTIF',
    statut: 'VERIFIED',
    expectedValue: 'TRAVAIL',
    raison: null,
    toolIdCible: 'F-IM-05-arbre-decisionnel-titre',
    matchStatus: 'ALIGNED',
  };

  const checkNonCompliant: ProcedureCheckAlignment = {
    checkId: 'c-2',
    libelle: 'Lettre de licenciement notifiée hors délai',
    critereCode: 'LICENCIEMENT_NOTIFICATION',
    statut: 'NON_COMPLIANT',
    expectedValue: null,
    raison: 'Notification reçue 8 jours après l\'entretien préalable',
    toolIdCible: 'F-DT-08-validite-licenciement',
    matchStatus: 'NON_COMPLIANT_FLAG',
  };

  const checkToVerify: ProcedureCheckAlignment = {
    checkId: 'c-3',
    libelle: 'Convention collective applicable à confirmer',
    critereCode: null,
    statut: 'TO_CHECK',
    expectedValue: null,
    raison: null,
    toolIdCible: 'F-DT-09-comparateur-indemnites',
    matchStatus: 'TO_VERIFY_FLAG',
  };

  const checkNoTargetTool: ProcedureCheckAlignment = {
    checkId: 'c-4',
    libelle: 'Vérifier la compétence territoriale du conseil',
    critereCode: null,
    statut: 'VERIFIED',
    expectedValue: null,
    raison: null,
    toolIdCible: null,
    matchStatus: 'NO_TARGET_TOOL',
  };

  const checksLabelResolver = (toolId: string): string | null => {
    const labels: Record<string, string> = {
      'F-IM-05-arbre-decisionnel-titre': 'TITRE DE SÉJOUR RECOMMANDÉ',
      'F-DT-08-validite-licenciement': 'VALIDITÉ LICENCIEMENT',
      'F-DT-09-comparateur-indemnites': 'COMPARATEUR INDEMNITÉS',
    };
    return labels[toolId] ?? null;
  };

  it('SF-193-03 CA-02: buildDocument(_, _, [], _, []) → aucune section Conformité procédurale', () => {
    const doc = service.buildDocument(mockCaseFile as CaseFile, mockSynthesis, [], undefined, []) as any;
    const s = JSON.stringify(doc.content);
    expect(s).not.toContain('Conformité procédurale');
  });

  it('SF-193-03 CA-02: buildDocument(_, _, _, _, undefined) → aucune section Conformité procédurale', () => {
    const doc = service.buildDocument(mockCaseFile as CaseFile, mockSynthesis) as any;
    const s = JSON.stringify(doc.content);
    expect(s).not.toContain('Conformité procédurale');
  });

  it('SF-193-03 CA-04 (cas d\'erreur): tous les checks en NO_TARGET_TOOL → section incluse, libellés sans suffixe outil', () => {
    // Mini-spec § Cas d'erreur : « Tous les checks en NO_TARGET_TOOL →
    // Section incluse, listant les checks sans suffixe → <label outil> ».
    const doc = service.buildDocument(
      mockCaseFile as CaseFile,
      mockSynthesis,
      [],
      checksLabelResolver,
      [checkNoTargetTool],
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('Conformité procédurale');
    expect(s).toContain('Vérifier la compétence territoriale du conseil');
    // Aucun suffixe outil (le check n'a pas de toolIdCible)
    expect(s).not.toMatch(/→ [A-Z]/);
  });

  it('SF-193-03 CA-03: check ALIGNED → sous-bloc ✅ Vérifications confirmées + suffixe outil', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile,
      mockSynthesis,
      [],
      checksLabelResolver,
      [checkAligned],
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('🔍 Conformité procédurale validée par votre avocat');
    expect(s).toContain('✅ Vérifications confirmées');
    expect(s).toContain('Motif principal du séjour confirmé');
    expect(s).toContain('→ TITRE DE SÉJOUR RECOMMANDÉ');
  });

  it('SF-193-03 CA-04: check NON_COMPLIANT_FLAG → sous-bloc ❌ Points non conformes + raison', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile,
      mockSynthesis,
      [],
      checksLabelResolver,
      [checkNonCompliant],
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('❌ Points non conformes');
    expect(s).toContain('Lettre de licenciement notifiée hors délai');
    expect(s).toContain('Notification reçue 8 jours après');
    expect(s).toContain('→ VALIDITÉ LICENCIEMENT');
  });

  it('SF-193-03 CA-05: check TO_VERIFY_FLAG → sous-bloc ⏳ Points à vérifier', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile,
      mockSynthesis,
      [],
      checksLabelResolver,
      [checkToVerify],
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('⏳ Points à vérifier');
    expect(s).toContain('Convention collective applicable à confirmer');
    expect(s).toContain('→ COMPARATEUR INDEMNITÉS');
  });

  it('SF-193-03 CA-06: check NO_TARGET_TOOL parmi des ALIGNED → libellé sans suffixe outil', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile,
      mockSynthesis,
      [],
      checksLabelResolver,
      [checkAligned, checkNoTargetTool],
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('🔍 Conformité procédurale');
    expect(s).toContain('Vérifier la compétence territoriale du conseil');
    // Le check NO_TARGET_TOOL ne doit avoir aucun suffixe → ne pas trouver de "→ " devant son libellé
    // (le check ALIGNED en a un mais c'est l'autre item)
    expect(s).toContain('→ TITRE DE SÉJOUR RECOMMANDÉ');
  });

  it('SF-193-03 CA-07 mix: 3 sous-blocs simultanés (ALIGNED + NON_COMPLIANT + TO_VERIFY)', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile,
      mockSynthesis,
      [],
      checksLabelResolver,
      [checkAligned, checkNonCompliant, checkToVerify],
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('✅ Vérifications confirmées');
    expect(s).toContain('❌ Points non conformes');
    expect(s).toContain('⏳ Points à vérifier');
  });

  it('SF-193-03 CA-08: section Conformité procédurale insérée APRÈS Stratégies retenues et AVANT Timeline', () => {
    const synWithTimeline: CaseAnalysisResult = {
      ...mockSynthesis,
      timeline: [{ date: '01/01/2024', evenement: 'Événement test' }],
    };
    const doc = service.buildDocument(
      mockCaseFile as CaseFile,
      synWithTimeline,
      [pisteAligned],
      labelResolver,
      [checkAligned],
    ) as any;
    const flat = JSON.stringify(doc.content);
    const stratIdx = flat.indexOf('🎯 Stratégies retenues');
    const checksIdx = flat.indexOf('🔍 Conformité procédurale');
    const timelineIdx = flat.indexOf('Chronologie');
    expect(stratIdx).toBeGreaterThan(-1);
    expect(checksIdx).toBeGreaterThan(-1);
    expect(timelineIdx).toBeGreaterThan(-1);
    expect(stratIdx).toBeLessThan(checksIdx);
    expect(checksIdx).toBeLessThan(timelineIdx);
  });

  it('SF-193-03 CA-10 fail-open indépendant: pistes succès + checks vide → section pistes uniquement', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile,
      mockSynthesis,
      [pisteAligned],
      labelResolver,
      [],
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('🎯 Stratégies retenues');
    expect(s).not.toContain('🔍 Conformité procédurale');
  });

  it('SF-193-03 CA-10 fail-open indépendant: pistes vide + checks succès → section checks uniquement', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile,
      mockSynthesis,
      [],
      checksLabelResolver,
      [checkAligned],
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).not.toContain('🎯 Stratégies retenues');
    expect(s).toContain('🔍 Conformité procédurale');
  });

  it('SF-193-03 CA-11: suffixe outil rendu en JetBrainsMono italique 9', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile,
      mockSynthesis,
      [],
      checksLabelResolver,
      [checkAligned],
    ) as any;
    const flat = JSON.stringify(doc.content);
    expect(flat).toMatch(/"text":"→ TITRE DE SÉJOUR RECOMMANDÉ","font":"JetBrainsMono","fontSize":9,"italics":true/);
  });

  it('SF-193-03: lookup label échoue → toolIdCible brut affiché en suffixe', () => {
    const noResolver = (_toolId: string): string | null => null;
    const doc = service.buildDocument(
      mockCaseFile as CaseFile,
      mockSynthesis,
      [],
      noResolver,
      [checkAligned],
    ) as any;
    const flat = JSON.stringify(doc.content);
    expect(flat).toContain('→ F-IM-05-arbre-decisionnel-titre');
  });

  // --- F-194 SF-194-03 : section « 📎 Pièces à demander au client » ---

  const pieceADemander: PieceManquanteAlignment = {
    pieceLibelle: 'Bulletins de salaire des 12 derniers mois',
    statut: 'A_DEMANDER',
    toolIdsCibles: ['F-DT-09-comparateur-indemnites'],
    destinataire: null,
    raisonNonApp: null,
  };

  const pieceADemanderPrefecture: PieceManquanteAlignment = {
    pieceLibelle: 'Récépissé de demande de titre de séjour',
    statut: 'A_DEMANDER',
    toolIdsCibles: [],
    destinataire: 'Préfecture',
    raisonNonApp: null,
  };

  const pieceObtenue: PieceManquanteAlignment = {
    pieceLibelle: 'Contrat de travail',
    statut: 'OBTENUE',
    toolIdsCibles: [],
    destinataire: null,
    raisonNonApp: null,
  };

  const pieceNonApplicable: PieceManquanteAlignment = {
    pieceLibelle: 'Avenant temps partiel',
    statut: 'NON_APPLICABLE',
    toolIdsCibles: [],
    destinataire: null,
    raisonNonApp: 'Salarié à temps plein',
  };

  const piecesLabelResolver = (toolId: string): string | null => {
    const labels: Record<string, string> = {
      'F-DT-09-comparateur-indemnites': 'COMPARATEUR INDEMNITÉS',
    };
    return labels[toolId] ?? null;
  };

  it('SF-194-03 CA-02: buildDocument(_, _, _, _, _, []) → aucune section Pièces à demander', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], undefined, [], []
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).not.toContain('Pièces à demander au client');
  });

  it('SF-194-03 CA-02: buildDocument sans piecesAlignment → aucune section', () => {
    const doc = service.buildDocument(mockCaseFile as CaseFile, mockSynthesis) as any;
    const s = JSON.stringify(doc.content);
    expect(s).not.toContain('Pièces à demander au client');
  });

  it('SF-194-03 CA-02: piecesAlignment = null → aucune section (fail-open)', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], undefined, [], null as any
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).not.toContain('Pièces à demander au client');
  });

  it('SF-194-03: aucune pièce À_DEMANDER (que des OBTENUE) → section omise', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], undefined, [], [pieceObtenue]
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).not.toContain('Pièces à demander au client');
  });

  it('SF-194-03 CA-01: ≥ 1 pièce À_DEMANDER → section incluse avec titre + libellé pièce', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], piecesLabelResolver, [], [pieceADemander]
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('📎 Pièces à demander au client');
    expect(s).toContain('Bulletins de salaire des 12 derniers mois');
    expect(s).toContain('Pour avancer sur votre dossier');
  });

  it('SF-194-03 CA-03: layout tableau case à cocher (☐) + colonnes Pièce + Destinataire + Date butoir', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], piecesLabelResolver, [], [pieceADemander]
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('☐');
    expect(s).toContain('À fournir');
    expect(s).toContain('Pièce');
    expect(s).toContain('Destinataire');
    expect(s).toContain('Date butoir');
  });

  it('SF-194-03 CA-04: titre proéminent — fond or léger PIECES_BG + bordure or ACCENT', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], piecesLabelResolver, [], [pieceADemander]
    ) as any;
    const flat = JSON.stringify(doc.content);
    // Titre fontSize 18 bold + fillColor or léger
    expect(flat).toMatch(/"text":"📎 Pièces à demander au client","fontSize":18,"bold":true/);
    expect(flat).toContain('"fillColor":"#FBF4E2"');
  });

  it('SF-194-03 CA-05 destinataire renseigné: "Préfecture" affiché tel quel', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], piecesLabelResolver, [], [pieceADemanderPrefecture]
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('Préfecture');
  });

  it('SF-194-03 CA-05 destinataire absent: défaut "Client"', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], piecesLabelResolver, [], [pieceADemander]
    ) as any;
    const s = JSON.stringify(doc.content);
    // pieceADemander a destinataire null → fallback "Client"
    expect(s).toContain('Client');
  });

  it('SF-194-03 CA-06 date butoir: today + 14j formatée JJ/MM/AAAA en JetBrainsMono', () => {
    // Calcule la date butoir attendue (today + 14j) en local
    const expected = new Date();
    expected.setDate(expected.getDate() + 14);
    const dd = String(expected.getDate()).padStart(2, '0');
    const mm = String(expected.getMonth() + 1).padStart(2, '0');
    const yyyy = expected.getFullYear();
    const expectedStr = `${dd}/${mm}/${yyyy}`;

    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], piecesLabelResolver, [], [pieceADemander]
    ) as any;
    const flat = JSON.stringify(doc.content);
    expect(flat).toContain(expectedStr);
    // Le rendu de la date est en JetBrainsMono fontSize 9
    expect(flat).toMatch(new RegExp(`"text":"${expectedStr.replace(/\//g, '\\/')}","font":"JetBrainsMono","fontSize":9`));
  });

  it('SF-194-03 CA-07 sous-blocs compteurs: OBTENUE et NON_APPLICABLE affichés en sous-bloc petit', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile,
      mockSynthesis,
      [],
      piecesLabelResolver,
      [],
      [pieceADemander, pieceObtenue, pieceNonApplicable],
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('✅ Pièces déjà reçues : 1');
    expect(s).toContain('🚫 Pièces non applicables au dossier : 1');
    // Le libellé des OBTENUE / NON_APPLICABLE n'est PAS listé (compteur seulement)
    expect(s).not.toContain('Contrat de travail');
    expect(s).not.toContain('Avenant temps partiel');
  });

  it('SF-194-03 CA-07: aucune OBTENUE / NON_APPLICABLE → compteurs absents', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], piecesLabelResolver, [], [pieceADemander]
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).not.toContain('Pièces déjà reçues');
    expect(s).not.toContain('Pièces non applicables');
  });

  it('SF-194-03 CA-08 ordre: F-192 → F-193 → F-194 → Timeline', () => {
    const synWithTimeline: CaseAnalysisResult = {
      ...mockSynthesis,
      timeline: [{ date: '01/01/2024', evenement: 'Événement test' }],
    };
    const doc = service.buildDocument(
      mockCaseFile as CaseFile,
      synWithTimeline,
      [pisteAligned],
      labelResolver,
      [checkAligned],
      [pieceADemander],
    ) as any;
    const flat = JSON.stringify(doc.content);
    const stratIdx = flat.indexOf('🎯 Stratégies retenues');
    const checksIdx = flat.indexOf('🔍 Conformité procédurale');
    const piecesIdx = flat.indexOf('📎 Pièces à demander');
    const timelineIdx = flat.indexOf('Chronologie');
    expect(stratIdx).toBeGreaterThan(-1);
    expect(checksIdx).toBeGreaterThan(-1);
    expect(piecesIdx).toBeGreaterThan(-1);
    expect(timelineIdx).toBeGreaterThan(-1);
    expect(stratIdx).toBeLessThan(checksIdx);
    expect(checksIdx).toBeLessThan(piecesIdx);
    expect(piecesIdx).toBeLessThan(timelineIdx);
  });

  it('SF-194-03 CA-10 fail-open indépendant: F-192/F-193 vides + F-194 succès → section pièces uniquement', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], undefined, [], [pieceADemander]
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).not.toContain('🎯 Stratégies retenues');
    expect(s).not.toContain('🔍 Conformité procédurale');
    expect(s).toContain('📎 Pièces à demander');
  });

  it('SF-194-03 CA-10 fail-open indépendant: F-192 succès + F-194 vide → section pistes seulement', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [pisteAligned], labelResolver, [], []
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('🎯 Stratégies retenues');
    expect(s).not.toContain('📎 Pièces à demander');
  });

  it('SF-194-03 suffixe outil: lookup → label outil cible affiché en JetBrainsMono italique 9', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], piecesLabelResolver, [], [pieceADemander]
    ) as any;
    const flat = JSON.stringify(doc.content);
    expect(flat).toContain('→ COMPARATEUR INDEMNITÉS');
    expect(flat).toMatch(/"text":"→ COMPARATEUR INDEMNITÉS","font":"JetBrainsMono","fontSize":9,"italics":true/);
  });

  it('SF-194-03: lookup label échoue → toolId brut affiché en suffixe', () => {
    const noResolver = (_toolId: string): string | null => null;
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], noResolver, [], [pieceADemander]
    ) as any;
    const flat = JSON.stringify(doc.content);
    expect(flat).toContain('→ F-DT-09-comparateur-indemnites');
  });

  it('SF-194-03: pièce sans toolIdsCibles → aucun suffixe outil', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], piecesLabelResolver, [], [pieceADemanderPrefecture]
    ) as any;
    const flat = JSON.stringify(doc.content);
    // Pas de suffixe → on ne doit pas trouver de "→ " devant un identifiant en majuscules
    // (le check isolé sur ce libellé : pas de "→ " du tout puisque c'est l'unique pièce)
    expect(flat).toContain('Récépissé de demande de titre de séjour');
    expect(flat).not.toMatch(/→ [A-Z]/);
  });

  it('SF-194-03 CA-12: page break après la section', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], piecesLabelResolver, [], [pieceADemander]
    ) as any;
    const flat = JSON.stringify(doc.content);
    // Au moins un pageBreak: 'after' présent dans le flux après le titre pièces
    const piecesIdx = flat.indexOf('📎 Pièces à demander');
    const tail = flat.substring(piecesIdx);
    expect(tail).toContain('"pageBreak":"after"');
  });

  // ----------------------------------------------------------------------
  // F-195 SF-195-03 : section « ⚠️ Risques retenus par votre avocat »
  // ----------------------------------------------------------------------

  const risqueValideStandard: RisqueAlignment = {
    risqueLibelle: 'Requalification du contrat possible',
    statut: 'VALIDE',
    toolIdsCibles: ['F-DT-08-validite-licenciement'],
    raisonEcarte: null,
  };

  const risqueValideSansOutil: RisqueAlignment = {
    risqueLibelle: 'Risque retenu sans mapping outil',
    statut: 'VALIDE',
    toolIdsCibles: [],
    raisonEcarte: null,
  };

  const risqueValideHarcelement: RisqueAlignment = {
    risqueLibelle: 'Harcèlement moral subi par le salarié',
    statut: 'VALIDE',
    toolIdsCibles: ['F-DT-12-harcelement-licenciement-nul'],
    raisonEcarte: null,
  };

  const risqueValideExpulsion: RisqueAlignment = {
    risqueLibelle: 'OQTF imminente — risque d\'expulsion',
    statut: 'VALIDE',
    toolIdsCibles: ['F-IM-08'],
    raisonEcarte: null,
  };

  const risqueACreuser: RisqueAlignment = {
    risqueLibelle: 'Risque à creuser',
    statut: 'A_CREUSER',
    toolIdsCibles: [],
    raisonEcarte: null,
  };

  const risqueEcarte: RisqueAlignment = {
    risqueLibelle: 'Clause non-concurrence abusive',
    statut: 'ECARTE',
    toolIdsCibles: ['F-DT-24'],
    raisonEcarte: 'Clause non présente au contrat',
  };

  const risquesLabelResolver = (toolId: string): string | null => {
    const labels: Record<string, string> = {
      'F-DT-08-validite-licenciement': 'VALIDITÉ LICENCIEMENT',
      'F-DT-12-harcelement-licenciement-nul': 'HARCELEMENT LICENCIEMENT NUL',
      'F-IM-08': 'OQTF DECISIONS',
    };
    return labels[toolId] ?? null;
  };

  it('SF-195-03 CA-07 fail-open: buildDocument(_, _, _, _, _, _, []) → aucune section Risques retenus', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], undefined, [], [], []
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).not.toContain('Risques retenus par votre avocat');
  });

  it('SF-195-03 CA-07 fail-open: buildDocument sans risquesAlignment → aucune section', () => {
    const doc = service.buildDocument(mockCaseFile as CaseFile, mockSynthesis) as any;
    const s = JSON.stringify(doc.content);
    expect(s).not.toContain('Risques retenus par votre avocat');
  });

  it('SF-195-03 CA-07 fail-open: risquesAlignment = null → aucune section', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], undefined, [], [], null as any
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).not.toContain('Risques retenus par votre avocat');
  });

  it('SF-195-03 : aucun risque VALIDÉ (que des A_CREUSER + ECARTE) → section omise', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], undefined, [], [],
      [risqueACreuser, risqueEcarte],
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).not.toContain('Risques retenus par votre avocat');
  });

  it('SF-195-03 : ≥ 1 risque VALIDÉ → section incluse avec titre + libellé risque', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], risquesLabelResolver, [], [],
      [risqueValideStandard],
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('⚠️ Risques retenus par votre avocat');
    expect(s).toContain('Requalification du contrat possible');
  });

  it('SF-195-03 : titre fontSize 16 bold navy + liseré or par défaut', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], risquesLabelResolver, [], [],
      [risqueValideStandard],
    ) as any;
    const flat = JSON.stringify(doc.content);
    expect(flat).toMatch(/"text":"⚠️ Risques retenus par votre avocat","fontSize":16,"bold":true/);
  });

  it('SF-195-03 suffixe outil: lookup → label outil cible affiché en JetBrainsMono italique 9', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], risquesLabelResolver, [], [],
      [risqueValideStandard],
    ) as any;
    const flat = JSON.stringify(doc.content);
    expect(flat).toContain('→ VALIDITÉ LICENCIEMENT');
    expect(flat).toMatch(/"text":"→ VALIDITÉ LICENCIEMENT","font":"JetBrainsMono","fontSize":9,"italics":true/);
  });

  it('SF-195-03 : lookup label échoue → toolId brut affiché en suffixe', () => {
    const noResolver = (_toolId: string): string | null => null;
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], noResolver, [], [],
      [risqueValideStandard],
    ) as any;
    const flat = JSON.stringify(doc.content);
    expect(flat).toContain('→ F-DT-08-validite-licenciement');
  });

  it('SF-195-03 : risque VALIDÉ sans toolIdsCibles → aucun suffixe outil', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], risquesLabelResolver, [], [],
      [risqueValideSansOutil],
    ) as any;
    const flat = JSON.stringify(doc.content);
    expect(flat).toContain('Risque retenu sans mapping outil');
    expect(flat).not.toMatch(/→ [A-Z]/);
  });

  it('SF-195-03 keyword critique « harcèlement » → pictogramme 🔴 + liseré rouge ERROR', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], risquesLabelResolver, [], [],
      [risqueValideHarcelement],
    ) as any;
    const flat = JSON.stringify(doc.content);
    // pictogramme 🔴 (au lieu du ⚠️ par défaut)
    expect(flat).toContain('🔴');
    // liseré gauche en couleur ERROR (#C0392B) — palette critique
    expect(flat).toContain('"fillColor":"#C0392B"');
  });

  it('SF-195-03 keyword critique « expulsion » → pictogramme 🔴', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], risquesLabelResolver, [], [],
      [risqueValideExpulsion],
    ) as any;
    const flat = JSON.stringify(doc.content);
    expect(flat).toContain('🔴');
  });

  it('SF-195-03 risque VALIDÉ standard (non critique) → pictogramme ⚠️ + liseré or ACCENT', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], risquesLabelResolver, [], [],
      [risqueValideStandard],
    ) as any;
    const flat = JSON.stringify(doc.content);
    expect(flat).toContain('⚠️');
    // liseré gauche en or (#C9973A) — palette charte navy/or par défaut
    expect(flat).toContain('"fillColor":"#C9973A"');
    // pas de 🔴 sur ce risque non critique
    expect(flat).not.toContain('🔴');
  });

  it('SF-195-03 sous-bloc compteur ÉCARTÉS: rendu si ≥ 1 ECARTE', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], risquesLabelResolver, [], [],
      [risqueValideStandard, risqueEcarte],
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('❌ Risques écartés : 1');
    // libellé du risque écarté NON listé (compteur seul, cohérent F-194)
    expect(s).not.toContain('Clause non-concurrence abusive');
  });

  it('SF-195-03 : aucun ECARTE → compteur écartés absent', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], risquesLabelResolver, [], [],
      [risqueValideStandard],
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).not.toContain('Risques écartés');
  });

  it('SF-195-03 score: riskScore IA brut + riskScoreAvocat → sous-titre « Score validé avocat : Y / 100 (vs IA brut : X / 100) »', () => {
    const synWithScores: CaseAnalysisResult = {
      ...mockSynthesis,
      riskScore: 78,
      // Champ optionnel ajouté par SF-195-01 (lecture défensive côté frontend)
      // — typé via cast pour compatibilité avec le type V1.
      ...({ riskScoreAvocat: 52 } as any),
    };
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, synWithScores, [], risquesLabelResolver, [], [],
      [risqueValideStandard],
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('Score validé avocat : 52 / 100 (vs IA brut : 78 / 100)');
  });

  it('SF-195-03 score: riskScoreAvocat seul → sous-titre « Score validé avocat : Y / 100 »', () => {
    const synAvocatOnly: CaseAnalysisResult = {
      ...mockSynthesis,
      riskScore: null,
      ...({ riskScoreAvocat: 65 } as any),
    };
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, synAvocatOnly, [], risquesLabelResolver, [], [],
      [risqueValideStandard],
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('Score validé avocat : 65 / 100');
    expect(s).not.toContain('vs IA brut');
  });

  it('SF-195-03 score: aucun score disponible → sous-titre score absent', () => {
    const synNoScores: CaseAnalysisResult = {
      ...mockSynthesis,
      riskScore: null,
    };
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, synNoScores, [], risquesLabelResolver, [], [],
      [risqueValideStandard],
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).not.toContain('Score validé avocat');
    expect(s).not.toContain('Score IA brut');
  });

  it('SF-195-03 ordre sections: F-192 → F-193 → F-194 → F-195 → Timeline', () => {
    const synWithTimeline: CaseAnalysisResult = {
      ...mockSynthesis,
      timeline: [{ date: '01/01/2024', evenement: 'Événement test' }],
    };
    const doc = service.buildDocument(
      mockCaseFile as CaseFile,
      synWithTimeline,
      [pisteAligned],
      labelResolver,
      [checkAligned],
      [pieceADemander],
      [risqueValideStandard],
    ) as any;
    const flat = JSON.stringify(doc.content);
    const stratIdx = flat.indexOf('🎯 Stratégies retenues');
    const checksIdx = flat.indexOf('🔍 Conformité procédurale');
    const piecesIdx = flat.indexOf('📎 Pièces à demander');
    const risquesIdx = flat.indexOf('⚠️ Risques retenus par votre avocat');
    const timelineIdx = flat.indexOf('Chronologie');
    expect(stratIdx).toBeGreaterThan(-1);
    expect(checksIdx).toBeGreaterThan(-1);
    expect(piecesIdx).toBeGreaterThan(-1);
    expect(risquesIdx).toBeGreaterThan(-1);
    expect(timelineIdx).toBeGreaterThan(-1);
    expect(stratIdx).toBeLessThan(checksIdx);
    expect(checksIdx).toBeLessThan(piecesIdx);
    expect(piecesIdx).toBeLessThan(risquesIdx);
    expect(risquesIdx).toBeLessThan(timelineIdx);
  });

  it('SF-195-03 fail-open indépendant: F-192/F-193/F-194 vides + F-195 succès → section risques uniquement', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], undefined, [], [], [risqueValideStandard]
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).not.toContain('🎯 Stratégies retenues');
    expect(s).not.toContain('🔍 Conformité procédurale');
    expect(s).not.toContain('📎 Pièces à demander');
    expect(s).toContain('⚠️ Risques retenus par votre avocat');
  });

  it('SF-195-03 plusieurs risques VALIDÉS (mix critique / standard) + écartés → liste + compteur', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], risquesLabelResolver, [], [],
      [risqueValideHarcelement, risqueValideStandard, risqueEcarte],
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('Harcèlement moral subi par le salarié');
    expect(s).toContain('Requalification du contrat possible');
    expect(s).toContain('🔴'); // pictogramme critique pour harcèlement
    expect(s).toContain('⚠️'); // pictogramme standard pour l'autre
    expect(s).toContain('❌ Risques écartés : 1');
  });

  // ----------------------------------------------------------------------
  // F-253 SF-253-03 : section « 🔍 Risques à creuser » (avant F-195 retenus)
  // ----------------------------------------------------------------------

  const risqueACreuserHarcelement: RisqueAlignment = {
    risqueLibelle: 'Harcèlement moral — suspicion à creuser',
    statut: 'A_CREUSER',
    toolIdsCibles: ['F-DT-12-harcelement-licenciement-nul'],
    raisonEcarte: null,
  };

  it('SF-253-03 alignment vide → section absente (fail-open)', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], undefined, [], [], [],
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).not.toContain('🔍 Risques à creuser');
  });

  it('SF-253-03 aucun risque À_CREUSER (V+É seulement) → section absente', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], undefined, [], [],
      [risqueValideStandard, risqueEcarte],
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).not.toContain('🔍 Risques à creuser');
  });

  it('SF-253-03 ≥ 1 risque À_CREUSER → section présente avec titre + sous-titre singulier', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], undefined, [], [],
      [risqueACreuser],
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('🔍 Risques à creuser');
    expect(s).toContain('1 risque à arbitrer — arbitrage avocat en attente');
    expect(s).toContain('Risque à creuser');
  });

  it('SF-253-03 plusieurs risques À_CREUSER → sous-titre pluriel', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], risquesLabelResolver, [], [],
      [risqueACreuser, risqueACreuserHarcelement],
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('2 risques à arbitrer — arbitrage avocat en attente');
  });

  it('SF-253-03 risque À_CREUSER avec mapping outil → suffixe label résolu', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], risquesLabelResolver, [], [],
      [risqueACreuserHarcelement],
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('→ HARCELEMENT LICENCIEMENT NUL');
  });

  it('SF-253-03 cohabite avec F-195 (V+À_C+É) — 2 sections distinctes', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], risquesLabelResolver, [], [],
      [risqueValideStandard, risqueACreuser, risqueEcarte],
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('🔍 Risques à creuser');
    expect(s).toContain('⚠️ Risques retenus par votre avocat');
  });

  it('SF-253-03 ordre PDF : F-253 (à creuser) AVANT F-195 (validés)', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], undefined, [], [],
      [risqueValideStandard, risqueACreuser],
    ) as any;
    const flat = JSON.stringify(doc.content);
    const aCreuserIdx = flat.indexOf('🔍 Risques à creuser');
    const validesIdx = flat.indexOf('⚠️ Risques retenus par votre avocat');
    expect(aCreuserIdx).toBeGreaterThan(-1);
    expect(validesIdx).toBeGreaterThan(-1);
    expect(aCreuserIdx).toBeLessThan(validesIdx);
  });

  it('SF-253-03 pas de pictogramme critique 🔴 dans la section À_CREUSER (neutre par défaut)', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], undefined, [], [],
      [risqueACreuserHarcelement],
    ) as any;
    const content = doc.content;
    // Extraire UNIQUEMENT la section "Risques à creuser" (entre son header et
    // le prochain header de section ⚠️/❓/Chronologie).
    const flat = JSON.stringify(content);
    const startIdx = flat.indexOf('🔍 Risques à creuser');
    // Cherche le prochain titre de section après À_CREUSER (s'il existe).
    const nextIdx = ['⚠️ Risques retenus', '❓ Réponses', 'Chronologie']
      .map(t => flat.indexOf(t, startIdx))
      .filter(i => i > startIdx)
      .reduce((m, v) => Math.min(m, v), Number.MAX_SAFE_INTEGER);
    const sectionSlice = flat.slice(startIdx,
      nextIdx === Number.MAX_SAFE_INTEGER ? flat.length : nextIdx);
    expect(sectionSlice).not.toContain('🔴');
  });

  // ============================================================
  // F-196 SF-196-03 : section « ❓ Réponses aux questions complémentaires »
  // ============================================================

  const questionRepondueOui: AiQuestionAlignment = {
    questionId: 'q1',
    questionText: 'Avez-vous reçu la lettre de licenciement ?',
    answerText: 'oui',
    pieceLibelleDeduit: 'Lettre de licenciement',
    statutDeduction: 'PIECE_OBTENUE',
  };

  const questionRepondueNon: AiQuestionAlignment = {
    questionId: 'q2',
    questionText: 'Avez-vous le contrat de travail ?',
    answerText: 'non',
    pieceLibelleDeduit: 'Contrat de travail',
    statutDeduction: 'PIECE_MANQUANTE',
  };

  const questionInfoOnly: AiQuestionAlignment = {
    questionId: 'q3',
    questionText: 'Combien d\'années d\'ancienneté ?',
    answerText: '7 ans',
    pieceLibelleDeduit: null,
    statutDeduction: 'INFO_ONLY',
  };

  const questionNonRepondue: AiQuestionAlignment = {
    questionId: 'q4',
    questionText: 'Avez-vous des fiches de paie ?',
    answerText: null,
    pieceLibelleDeduit: null,
    statutDeduction: undefined,
  };

  it('SF-196-03: aucune question alignment → section omise (fail-open)', () => {
    const doc = service.buildDocument(mockCaseFile as CaseFile, mockSynthesis) as any;
    const s = JSON.stringify(doc.content);
    expect(s).not.toContain('❓ Réponses aux questions complémentaires');
  });

  it('SF-196-03: aiQuestionsAlignment vide → section omise', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], undefined, [], [], [], [],
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).not.toContain('❓ Réponses aux questions complémentaires');
  });

  it('SF-196-03: aucune question répondue → section omise', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], undefined, [], [], [], [questionNonRepondue],
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).not.toContain('❓ Réponses aux questions complémentaires');
  });

  it('SF-196-03: ≥ 1 question répondue OUI avec pièce déduite → section + Q&R + suffix pièce', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], undefined, [], [], [], [questionRepondueOui],
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('❓ Réponses aux questions complémentaires');
    expect(s).toContain('Avez-vous reçu la lettre de licenciement ?');
    expect(s).toContain('oui');
    expect(s).toContain('✅ Pièce confirmée');
    expect(s).toContain('Lettre de licenciement');
  });

  it('SF-196-03: question répondue NON avec pièce manquante → suffix « pièce à demander »', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], undefined, [], [], [], [questionRepondueNon],
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('Avez-vous le contrat de travail ?');
    expect(s).toContain('📩 Pièce à demander');
    expect(s).toContain('Contrat de travail');
  });

  it('SF-196-03: INFO_ONLY (pas de pièce déduite) → pas de suffix pièce', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], undefined, [], [], [], [questionInfoOnly],
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('Combien d\'années d\'ancienneté ?');
    expect(s).toContain('7 ans');
    expect(s).not.toContain('Pièce confirmée');
    expect(s).not.toContain('Pièce à demander');
    expect(s).not.toContain('Pièce déduite');
  });

  it('SF-196-03: mix répondues + non répondues → seules les répondues incluses', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], undefined, [], [], [],
      [questionRepondueOui, questionNonRepondue, questionRepondueNon],
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('Avez-vous reçu la lettre de licenciement ?');
    expect(s).toContain('Avez-vous le contrat de travail ?');
    expect(s).not.toContain('Avez-vous des fiches de paie ?');
  });

  it('SF-196-03: ordre canonique → questions APRÈS risques et AVANT chronologie', () => {
    const synWithTimeline: CaseAnalysisResult = {
      ...mockSynthesis,
      timeline: [{ date: '01/01/2024', evenement: 'Licenciement' }],
    };
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, synWithTimeline,
      [], undefined, [], [], [risqueValideStandard], [questionRepondueOui],
    ) as any;
    const flat = JSON.stringify(doc.content);
    const risquesIdx = flat.indexOf('⚠️ Risques retenus par votre avocat');
    const questionsIdx = flat.indexOf('❓ Réponses aux questions complémentaires');
    const timelineIdx = flat.indexOf('Chronologie');
    expect(risquesIdx).toBeGreaterThan(-1);
    expect(questionsIdx).toBeGreaterThan(-1);
    expect(timelineIdx).toBeGreaterThan(-1);
    expect(risquesIdx).toBeLessThan(questionsIdx);
    expect(questionsIdx).toBeLessThan(timelineIdx);
  });

  it('SF-196-03: question avec answerText vide ("   ") → exclue (filtre trim)', () => {
    const qEmpty: AiQuestionAlignment = {
      questionId: 'q5',
      questionText: 'Question',
      answerText: '   ',
      pieceLibelleDeduit: null,
    };
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], undefined, [], [], [], [qEmpty],
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).not.toContain('❓ Réponses aux questions complémentaires');
  });

  it('SF-196-03: question avec questionText manquant → fallback "(Question non disponible)"', () => {
    const qSansTexte: AiQuestionAlignment = {
      questionId: 'q6',
      questionText: undefined,
      answerText: 'réponse libre',
      pieceLibelleDeduit: null,
    };
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], undefined, [], [], [], [qSansTexte],
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('(Question non disponible)');
    expect(s).toContain('réponse libre');
  });

  // ─────────────────────────────────────────────────────────────────────────
  // F-JU-02 SF-JU-02-02 — Section « 📚 Jurisprudence applicable »
  // ─────────────────────────────────────────────────────────────────────────

  const jurisprudenceUnOutilDeuxArrets = {
    entries: [
      {
        toolId: 'f-dt-30-indemnite-licenciement-macron',
        brancheCalculId: 'default',
        citations: [
          {
            id: 'cit-1',
            arretRef: 'Cass. soc. 8 janv. 2025, n° 23-12.345',
            juridiction: 'Cour de cassation, chambre sociale',
            dateArret: '2025-01-08',
            numeroPourvoi: '23-12.345',
            lienLegifrance: 'https://legifrance.gouv.fr/x',
            chapeauOfficiel: 'Le barème Macron s\'applique sans exception.',
            lastVerifiedAt: '2026-05-01T00:00:00Z',
            confidenceScore: '0.95',
          },
          {
            id: 'cit-2',
            arretRef: 'Cass. soc. 11 mai 2022, n° 21-10.000',
            juridiction: 'Cour de cassation, chambre sociale',
            dateArret: '2022-05-11',
            numeroPourvoi: '21-10.000',
            lienLegifrance: 'https://legifrance.gouv.fr/y',
            chapeauOfficiel: 'Conventionnalité du barème confirmée.',
            lastVerifiedAt: '2026-05-01T00:00:00Z',
            confidenceScore: '0.90',
          },
        ],
      },
    ],
  };

  it('SF-JU-02-02: jurisprudenceApplicable absent (legacy) → section omise (fail-open)', () => {
    const doc = service.buildDocument(mockCaseFile as CaseFile, mockSynthesis) as any;
    const s = JSON.stringify(doc.content);
    expect(s).not.toContain('📚 Jurisprudence applicable');
  });

  it('SF-JU-02-02: entries vide → section omise', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], undefined, [], [], [], [],
      { entries: [] },
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).not.toContain('📚 Jurisprudence applicable');
  });

  it('SF-JU-02-02: entry avec citations vide → section omise (filtre défensif)', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], undefined, [], [], [], [],
      { entries: [{ toolId: 'f-dt-30', brancheCalculId: 'default', citations: [] }] },
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).not.toContain('📚 Jurisprudence applicable');
  });

  it('SF-JU-02-02: 1 outil + 2 arrêts → bloc bien formé avec arrêts et chapeaux', () => {
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], undefined, [], [], [], [],
      jurisprudenceUnOutilDeuxArrets,
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('📚 Jurisprudence applicable');
    expect(s).toContain('Arrêts structurants mappés');
    expect(s).toContain('Cass. soc. 8 janv. 2025, n° 23-12.345');
    expect(s).toContain('Cass. soc. 11 mai 2022, n° 21-10.000');
    expect(s).toContain('barème Macron');
    expect(s).toContain('Conventionnalité du barème confirmée.');
    // toolId brut affiché si pas de resolver
    expect(s).toContain('f-dt-30-indemnite-licenciement-macron');
    expect(s).toContain('branche : default');
  });

  it('SF-JU-02-02: toolLabelResolver fourni → label humain plutôt que toolId brut', () => {
    const resolver = (toolId: string): string | null =>
      toolId === 'f-dt-30-indemnite-licenciement-macron' ? 'Indemnité Macron (FR)' : null;

    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], resolver, [], [], [], [],
      jurisprudenceUnOutilDeuxArrets,
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('Indemnité Macron (FR)');
  });

  it('SF-JU-02-02: ordre canonique → jurisprudence APRÈS questions et AVANT chronologie', () => {
    const synWithTimeline: CaseAnalysisResult = {
      ...mockSynthesis,
      timeline: [{ date: '01/01/2024', evenement: 'Licenciement' }],
    };
    const questionRepondue: AiQuestionAlignment = {
      questionId: 'q1',
      questionText: 'Avez-vous le contrat ?',
      answerText: 'oui',
      pieceLibelleDeduit: null,
    };
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, synWithTimeline,
      [], undefined, [], [], [], [questionRepondue],
      jurisprudenceUnOutilDeuxArrets,
    ) as any;
    const flat = JSON.stringify(doc.content);
    const questionsIdx = flat.indexOf('❓ Réponses aux questions complémentaires');
    const jurisIdx = flat.indexOf('📚 Jurisprudence applicable');
    const timelineIdx = flat.indexOf('Chronologie');
    expect(questionsIdx).toBeGreaterThan(-1);
    expect(jurisIdx).toBeGreaterThan(-1);
    expect(timelineIdx).toBeGreaterThan(-1);
    expect(questionsIdx).toBeLessThan(jurisIdx);
    expect(jurisIdx).toBeLessThan(timelineIdx);
  });

  it('SF-JU-02-02: 2 outils distincts → 2 blocs (labels + arrêts respectifs)', () => {
    const deuxOutils = {
      entries: [
        {
          toolId: 'f-dt-30',
          brancheCalculId: 'default',
          citations: [{
            id: 'a',
            arretRef: 'Cass. soc. 1 janv. 2025',
            juridiction: 'CCass',
            dateArret: '2025-01-01',
            numeroPourvoi: '0',
            lienLegifrance: 'https://x',
            chapeauOfficiel: 'Chapeau A',
            lastVerifiedAt: '2026-05-01T00:00:00Z',
            confidenceScore: '0.8',
          }],
        },
        {
          toolId: 'f-dt-08',
          brancheCalculId: 'cause-reelle-serieuse',
          citations: [{
            id: 'b',
            arretRef: 'Cass. soc. 2 janv. 2025',
            juridiction: 'CCass',
            dateArret: '2025-01-02',
            numeroPourvoi: '0',
            lienLegifrance: 'https://y',
            chapeauOfficiel: 'Chapeau B',
            lastVerifiedAt: '2026-05-01T00:00:00Z',
            confidenceScore: '0.8',
          }],
        },
      ],
    };
    const doc = service.buildDocument(
      mockCaseFile as CaseFile, mockSynthesis, [], undefined, [], [], [], [],
      deuxOutils,
    ) as any;
    const s = JSON.stringify(doc.content);
    expect(s).toContain('f-dt-30');
    expect(s).toContain('f-dt-08');
    expect(s).toContain('Cass. soc. 1 janv. 2025');
    expect(s).toContain('Cass. soc. 2 janv. 2025');
    expect(s).toContain('Chapeau A');
    expect(s).toContain('Chapeau B');
    expect(s).toContain('branche : cause-reelle-serieuse');
  });

  // ─────────────────────────────────────────────────────────────────────────
  // SF-98-51 — Export PDF des conclusions
  // ─────────────────────────────────────────────────────────────────────────

  // PDF-CONC-01 : buildConclusionFileName avec titre non vide
  it('PDF-CONC-01: buildConclusionFileName returns [slug]-conclusions-vN.pdf', () => {
    expect(service.buildConclusionFileName('Affaire Dupont c/ SA Renault', 4))
      .toBe('affaire-dupont-c-sa-renault-conclusions-v4.pdf');
  });

  // PDF-CONC-02 : buildConclusionFileName avec titre vide → fallback
  it('PDF-CONC-02: buildConclusionFileName returns conclusions-vN.pdf when title empty', () => {
    expect(service.buildConclusionFileName('', 2)).toBe('conclusions-v2.pdf');
    expect(service.buildConclusionFileName('   ', 5)).toBe('conclusions-v5.pdf');
  });

  // PDF-CONC-03 : buildConclusionFileName retire les accents
  it('PDF-CONC-03: buildConclusionFileName strips accented characters', () => {
    const name = service.buildConclusionFileName('Licenciement économique — Müller', 1);
    expect(name).not.toMatch(/[àáâãéèêëîïôùûüç]/i);
    expect(name).toBe('licenciement-economique-muller-conclusions-v1.pdf');
  });

  // PDF-CONC-04 : buildConclusionDocument — en-têtes en titres, reste en paragraphes
  it('PDF-CONC-04: buildConclusionDocument styles section headings as titles', () => {
    const content =
      'POUR\nM. Dupont, salarié.\n\nFAITS ET PROCÉDURE\nLe contrat a été rompu.\n\nPAR CES MOTIFS\nPlaise au Conseil…';
    const doc = service.buildConclusionDocument(content) as any;
    expect(doc.pageSize).toBe('A4');
    expect(Array.isArray(doc.content)).toBe(true);

    const headings = doc.content.filter((b: any) => b.style === 'sectionTitle');
    const paragraphs = doc.content.filter((b: any) => b.style === 'paragraph');
    expect(headings.map((h: any) => h.text)).toEqual([
      'POUR', 'FAITS ET PROCÉDURE', 'PAR CES MOTIFS',
    ]);
    expect(paragraphs.length).toBeGreaterThan(0);
    expect(JSON.stringify(doc.content)).toContain('M. Dupont, salarié.');
  });

  // PDF-CONC-05 : buildConclusionDocument tolère un contenu vide
  it('PDF-CONC-05: buildConclusionDocument handles empty content', () => {
    const doc = service.buildConclusionDocument('') as any;
    expect(Array.isArray(doc.content)).toBe(true);
    expect(doc.content.length).toBe(1);
  });

  // PDF-CONC-06 : exportConclusion construit le document et déclenche le download
  it('PDF-CONC-06: exportConclusion builds the document and triggers the pdfmake download', async () => {
    pdfDownloadSpy.mockClear();
    pdfCreateSpy.mockClear();
    const buildSpy = jest.spyOn(service, 'buildConclusionDocument');

    service.exportConclusion('POUR\nM. Dupont.', 'Affaire Dupont', 4);
    // Laisse les imports dynamiques `pdfmake` se résoudre.
    await new Promise((r) => setTimeout(r, 0));
    await new Promise((r) => setTimeout(r, 0));

    expect(buildSpy).toHaveBeenCalledWith('POUR\nM. Dupont.');
    expect(pdfCreateSpy).toHaveBeenCalled();
    expect(pdfDownloadSpy).toHaveBeenCalledWith('affaire-dupont-conclusions-v4.pdf');
    expect(snackBar.open).not.toHaveBeenCalled();
  });

  // PDF-CONC-07 : exportConclusion affiche une snackbar si la génération échoue
  it('PDF-CONC-07: exportConclusion shows snackBar error when generation fails', async () => {
    // buildConclusionDocument lève → la chaîne d'import échoue → catch snackbar.
    jest.spyOn(service, 'buildConclusionDocument').mockImplementation(() => {
      throw new Error('boom');
    });

    service.exportConclusion('POUR\nTexte', 'Dossier', 1);
    // Laisse les imports dynamiques `pdfmake` se résoudre.
    await new Promise((r) => setTimeout(r, 0));
    await new Promise((r) => setTimeout(r, 0));

    expect(snackBar.open).toHaveBeenCalledWith(
      'Erreur lors de la génération du document PDF.',
      'Fermer',
      expect.objectContaining({ duration: 4000, panelClass: ['snack-error'] }),
    );
  });
});
