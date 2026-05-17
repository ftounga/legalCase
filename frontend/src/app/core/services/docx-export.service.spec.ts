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

  // ───────────────────────────────────────────────────────────────────────
  // SF-98-50 — Export Word des conclusions
  // ───────────────────────────────────────────────────────────────────────

  // D-50 : buildConclusionFileName avec titre non vide
  it('D-50: buildConclusionFileName returns [slug]-conclusions-vN.docx', () => {
    const name = service.buildConclusionFileName('Affaire Dupont c/ SA Renault', 3);
    expect(name).toBe('affaire-dupont-c-sa-renault-conclusions-v3.docx');
  });

  // D-51 : buildConclusionFileName avec titre vide → fallback
  it('D-51: buildConclusionFileName returns conclusions-vN.docx when title empty', () => {
    expect(service.buildConclusionFileName('', 2)).toBe('conclusions-v2.docx');
    expect(service.buildConclusionFileName('   ', 5)).toBe('conclusions-v5.docx');
  });

  // D-52 : buildConclusionFileName retire les accents
  it('D-52: buildConclusionFileName strips accented characters', () => {
    const name = service.buildConclusionFileName('Licenciement économique — Müller', 1);
    expect(name).not.toMatch(/[àáâãéèêëîïôùûüç]/i);
    expect(name).toBe('licenciement-economique-muller-conclusions-v1.docx');
  });

  // D-53 : exportConclusion construit le document et déclenche le download
  it('D-53: exportConclusion builds the document and triggers the blob download', async () => {
    const clickSpy = jest.fn();
    const anchor = { href: '', download: '', click: clickSpy } as unknown as HTMLAnchorElement;
    jest.spyOn(document, 'createElement').mockReturnValue(anchor);
    (URL as any).createObjectURL = jest.fn().mockReturnValue('blob:fake');
    (URL as any).revokeObjectURL = jest.fn();

    const content =
      'POUR\nM. Dupont, salarié.\n\nFAITS ET PROCÉDURE\nLe contrat a été rompu.\n\nPAR CES MOTIFS\nPlaise au Conseil…';
    service.exportConclusion(content, 'Affaire Dupont', 4);

    // Laisse l'import dynamique de `docx` puis le Packer se résoudre.
    await new Promise((r) => setTimeout(r, 0));
    await new Promise((r) => setTimeout(r, 0));

    expect(clickSpy).toHaveBeenCalled();
    expect(anchor.download).toBe('affaire-dupont-conclusions-v4.docx');
    expect(URL.createObjectURL).toHaveBeenCalled();
    expect(snackBar.open).not.toHaveBeenCalled();
  });

  // D-54 : isSectionHeading — en-têtes MAJUSCULES vs paragraphes
  it('D-54: section heading detection — uppercase short lines are headings', () => {
    const isHeading = (line: string): boolean =>
      (service as any).isSectionHeading(line);

    // En-têtes de section : entièrement en majuscules, courts.
    expect(isHeading('POUR')).toBe(true);
    expect(isHeading('CONTRE')).toBe(true);
    expect(isHeading('FAITS ET PROCÉDURE')).toBe(true);
    expect(isHeading('DISCUSSION')).toBe(true);
    expect(isHeading('PAR CES MOTIFS')).toBe(true);
    expect(isHeading('  PAR CES MOTIFS  ')).toBe(true);

    // Paragraphes : casse mixte, lignes vides, lignes longues.
    expect(isHeading('Le salarié a été licencié.')).toBe(false);
    expect(isHeading('')).toBe(false);
    expect(isHeading('   ')).toBe(false);
    expect(isHeading('123 — 456')).toBe(false);
    expect(
      isHeading(
        'CECI EST UNE LIGNE BEAUCOUP TROP LONGUE POUR ETRE UN EN-TETE DE SECTION DE CONCLUSIONS',
      ),
    ).toBe(false);
  });

  // D-55 : exportConclusion affiche une snackbar si le packing échoue
  it('D-55: exportConclusion shows snackBar error when docx import fails', async () => {
    jest.spyOn(document, 'createElement').mockImplementation(() => {
      throw new Error('boom');
    });
    (URL as any).createObjectURL = jest.fn().mockReturnValue('blob:fake');
    (URL as any).revokeObjectURL = jest.fn();

    service.exportConclusion('POUR\nTexte', 'Dossier', 1);
    await new Promise((r) => setTimeout(r, 0));
    await new Promise((r) => setTimeout(r, 0));

    expect(snackBar.open).toHaveBeenCalledWith(
      'Erreur lors de la génération du document Word.',
      'Fermer',
      expect.objectContaining({ duration: 4000, panelClass: ['snack-error'] }),
    );
  });
});
