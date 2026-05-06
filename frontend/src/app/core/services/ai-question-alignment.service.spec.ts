import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';

import { AiQuestionAlignmentService } from './ai-question-alignment.service';
import { AiQuestionAlignment } from '../models/ai-question-alignment.model';

describe('AiQuestionAlignmentService — F-196 SF-196-02', () => {
  let service: AiQuestionAlignmentService;
  let http: HttpTestingController;

  const CASE_FILE_ID = '44444444-4444-4444-4444-444444444444';
  const URL = `/api/v1/case-files/${CASE_FILE_ID}/ai-questions-alignment`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AiQuestionAlignmentService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(AiQuestionAlignmentService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('CA-01 — GET 200 retourne le tableau d\'alignement (3 statuts couverts)', () => {
    const payload: AiQuestionAlignment[] = [
      {
        questionId: 'q-1',
        answerText: 'Le contrat de travail est annexé à la lettre de mission.',
        pieceLibelleDeduit: 'Contrat de travail',
        statutDeduction: 'PIECE_OBTENUE',
      },
      {
        questionId: 'q-2',
        answerText: 'Aucune fiche de paie disponible, employeur en défaut.',
        pieceLibelleDeduit: 'Bulletins de salaire',
        statutDeduction: 'PIECE_MANQUANTE',
      },
      {
        questionId: 'q-3',
        answerText: 'Le délai de prescription a été clarifié à 3 ans.',
        pieceLibelleDeduit: null,
        statutDeduction: 'INFO_ONLY',
      },
    ];
    let received: AiQuestionAlignment[] | undefined;
    service.getForCaseFile(CASE_FILE_ID).subscribe(v => (received = v));
    const req = http.expectOne(URL);
    expect(req.request.method).toBe('GET');
    req.flush(payload);
    expect(received).toEqual(payload);
    expect(received).toHaveLength(3);
  });

  it('CA-01 — GET 200 sur tableau vide retourne []', () => {
    let received: AiQuestionAlignment[] | undefined;
    service.getForCaseFile(CASE_FILE_ID).subscribe(v => (received = v));
    http.expectOne(URL).flush([]);
    expect(received).toEqual([]);
  });

  it('CA-03 fail-open — GET 404 retourne []', () => {
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    let received: AiQuestionAlignment[] | undefined;
    service.getForCaseFile(CASE_FILE_ID).subscribe(v => (received = v));
    http.expectOne(URL).flush(null, { status: 404, statusText: 'Not Found' });
    expect(received).toEqual([]);
    warnSpy.mockRestore();
  });

  it('CA-03 fail-open — GET 500 retourne [] + console.warn', () => {
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    let received: AiQuestionAlignment[] | undefined;
    service.getForCaseFile(CASE_FILE_ID).subscribe(v => (received = v));
    http.expectOne(URL).flush(null, { status: 500, statusText: 'Server Error' });
    expect(received).toEqual([]);
    expect(warnSpy).toHaveBeenCalled();
    warnSpy.mockRestore();
  });

  it('CA-03 fail-open — GET timeout retourne []', () => {
    // Network error (timeout-like) — RxJS timeout opérator déclenchera
    // catchError avec un TimeoutError en cas de retard > 5 s ; ici on simule
    // une erreur réseau pour tester la même branche `catchError`.
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    let received: AiQuestionAlignment[] | undefined;
    service.getForCaseFile(CASE_FILE_ID).subscribe(v => (received = v));
    http.expectOne(URL).error(new ProgressEvent('error'));
    expect(received).toEqual([]);
    warnSpy.mockRestore();
  });
});
