import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';

import { PieceManquanteAlignmentService } from './piece-manquante-alignment.service';
import { PieceManquanteAlignment } from '../models/piece-manquante-alignment.model';

describe('PieceManquanteAlignmentService — F-194 SF-194-02', () => {
  let service: PieceManquanteAlignmentService;
  let http: HttpTestingController;

  const CASE_FILE_ID = '33333333-3333-3333-3333-333333333333';
  const URL = `/api/v1/case-files/${CASE_FILE_ID}/pieces-manquantes-alignment`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        PieceManquanteAlignmentService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(PieceManquanteAlignmentService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('CA-06 — GET 200 retourne le tableau d\'alignement', () => {
    const payload: PieceManquanteAlignment[] = [
      {
        pieceLibelle: 'Contrat de travail',
        statut: 'OBTENUE',
        toolIdsCibles: ['F-DT-04-fiche-prudhomale', 'F-DT-08-licenciement-validity'],
        destinataire: null,
        raisonNonApp: null,
      },
      {
        pieceLibelle: 'Acte de mariage',
        statut: 'NON_APPLICABLE',
        toolIdsCibles: ['F-FA-07-checklist-divorce'],
        destinataire: null,
        raisonNonApp: 'Concubinage simple',
      },
    ];
    let received: PieceManquanteAlignment[] | undefined;
    service.getForCaseFile(CASE_FILE_ID).subscribe(v => (received = v));
    const req = http.expectOne(URL);
    expect(req.request.method).toBe('GET');
    req.flush(payload);
    expect(received).toEqual(payload);
  });

  it('CA-11 fail-open — GET 404 retourne []', () => {
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    let received: PieceManquanteAlignment[] | undefined;
    service.getForCaseFile(CASE_FILE_ID).subscribe(v => (received = v));
    http.expectOne(URL).flush(null, { status: 404, statusText: 'Not Found' });
    expect(received).toEqual([]);
    warnSpy.mockRestore();
  });

  it('CA-11 fail-open — GET 500 retourne [] + warn', () => {
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    let received: PieceManquanteAlignment[] | undefined;
    service.getForCaseFile(CASE_FILE_ID).subscribe(v => (received = v));
    http.expectOne(URL).flush(null, { status: 500, statusText: 'Server Error' });
    expect(received).toEqual([]);
    expect(warnSpy).toHaveBeenCalled();
    warnSpy.mockRestore();
  });
});
