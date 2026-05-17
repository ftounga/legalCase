import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { ConclusionsService } from './conclusions.service';
import { ConclusionResponse } from '../models/conclusion.model';

describe('ConclusionsService', () => {
  let service: ConclusionsService;
  let httpMock: HttpTestingController;

  const CASE_ID = 'case-1';
  const GET_URL = `/api/v1/case-files/${CASE_ID}/conclusions`;
  const GENERATE_URL = `/api/v1/case-files/${CASE_ID}/conclusions/generate`;

  function doneResponse(): ConclusionResponse {
    return {
      id: 'conc-1',
      caseFileId: CASE_ID,
      status: 'DONE',
      content: 'POUR : M. X\n\nFAITS ET PROCÉDURE\n…',
      jurisdictionLabel: 'Conseil de prud\'hommes',
      stageLabel: 'Bureau de jugement (fond)',
      positionLabel: 'Demandeur (salarié)',
      modelUsed: 'claude-sonnet-4-6',
      generatedAt: '2026-05-18T10:00:00Z',
      errorMessage: null,
      createdAt: '2026-05-18T09:55:00Z',
      updatedAt: '2026-05-18T10:00:00Z',
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ConclusionsService],
    });
    service = TestBed.inject(ConclusionsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getConclusion → GET sur l\'URL du contrat', () => {
    let received: ConclusionResponse | undefined;
    service.getConclusion(CASE_ID).subscribe((res) => (received = res));

    const req = httpMock.expectOne(GET_URL);
    expect(req.request.method).toBe('GET');
    req.flush(doneResponse());

    expect(received!.status).toBe('DONE');
    expect(received!.content).toContain('FAITS ET PROCÉDURE');
  });

  it('generate → POST sur l\'URL /generate avec un corps vide', () => {
    let received: { status: string } | undefined;
    service.generate(CASE_ID).subscribe((res) => (received = res));

    const req = httpMock.expectOne(GENERATE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush({ status: 'PENDING' });

    expect(received!.status).toBe('PENDING');
  });
});
