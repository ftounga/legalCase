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
});
