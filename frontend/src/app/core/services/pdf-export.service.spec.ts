import { TestBed } from '@angular/core/testing';
import { PdfExportService } from './pdf-export.service';
import { CaseAnalysisResult } from '../models/case-analysis.model';
import { CaseFile } from '../models/case-file.model';

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

  beforeEach(() => {
    TestBed.configureTestingModule({});
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
});
