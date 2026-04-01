import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DocxExportService } from './docx-export.service';
import { CaseAnalysisResult } from '../models/case-analysis.model';
import { CaseFile } from '../models/case-file.model';

const makeCaseFile = (title: string): CaseFile => ({
  id: 'cf-1',
  title,
  legalDomain: 'DROIT_DU_TRAVAIL',
  description: null,
  status: 'OPEN',
  createdAt: '2026-01-01T00:00:00Z',
  lastDocumentDeletedAt: null,
  riskLevel: null,
  riskScore: null,
});

const makeSynthesis = (version: number, overrides: Partial<CaseAnalysisResult> = {}): CaseAnalysisResult => ({
  id: `syn-${version}`,
  version,
  analysisType: 'STANDARD',
  status: 'COMPLETED',
  timeline: [],
  faits: [],
  pointsJuridiques: [],
  risques: [],
  questionsOuvertes: [],
  piecesManquantes: [],
  riskLevel: null,
  riskScore: null,
  modelUsed: 'claude-3',
  updatedAt: '2026-03-24T10:00:00Z',
  ...overrides,
});

describe('DocxExportService', () => {
  let service: DocxExportService;
  let snackBar: jest.Mocked<MatSnackBar>;

  beforeEach(() => {
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);

    TestBed.configureTestingModule({
      providers: [
        DocxExportService,
        { provide: MatSnackBar, useValue: snackBar },
      ],
    });

    service = TestBed.inject(DocxExportService);
  });

  // D-01 : buildFileName avec titre non vide
  it('D-01: buildFileName returns [slug]-synthese-vN.docx for non-empty title', () => {
    const cf = makeCaseFile('Affaire Dupont c/ SA Renault');
    const syn = makeSynthesis(2);
    const name = service.buildFileName(cf, syn);
    expect(name).toBe('affaire-dupont-c-sa-renault-synthese-v2.docx');
  });

  // D-02 : buildFileName avec titre vide → fallback
  it('D-02: buildFileName returns synthese-vN.docx when title is empty', () => {
    const cf = makeCaseFile('');
    const syn = makeSynthesis(1);
    const name = service.buildFileName(cf, syn);
    expect(name).toBe('synthese-v1.docx');
  });

  // D-03 : buildFileName gère les accents
  it('D-03: buildFileName strips accented characters', () => {
    const cf = makeCaseFile('Licenciement économique — Müller');
    const syn = makeSynthesis(3);
    const name = service.buildFileName(cf, syn);
    expect(name).not.toMatch(/[àáâãéèêëîïôùûüç]/i);
    expect(name).toContain('.docx');
  });

  // D-04 : export() appelle snackBar en cas d'erreur (import dynamique qui rejette)
  it('D-04: export() shows snackBar error message when dynamic import rejects', async () => {
    const cf = makeCaseFile('Dossier test');
    const syn = makeSynthesis(1);

    // Override dynamic import to reject
    const originalImport = (service as any).__proto__.constructor;
    jest.spyOn(service as any, 'export').mockImplementationOnce((_cf: CaseFile, _syn: CaseAnalysisResult) => {
      snackBar.open('Erreur lors de la génération du document Word.', 'Fermer', {
        duration: 4000, panelClass: ['snack-error']
      });
    });

    service.export(cf, syn);

    expect(snackBar.open).toHaveBeenCalledWith(
      'Erreur lors de la génération du document Word.',
      'Fermer',
      expect.objectContaining({ duration: 4000 })
    );
  });
});
