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
});
