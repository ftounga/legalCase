import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { ConclusionsService } from './conclusions.service';
import {
  ConclusionResponse,
  ConclusionVersionSummary,
} from '../models/conclusion.model';

describe('ConclusionsService', () => {
  let service: ConclusionsService;
  let httpMock: HttpTestingController;

  const CASE_ID = 'case-1';
  const VERSION_ID = 'conc-2';
  const GET_URL = `/api/v1/case-files/${CASE_ID}/conclusions`;
  const GENERATE_URL = `/api/v1/case-files/${CASE_ID}/conclusions/generate`;
  const VERSIONS_URL = `/api/v1/case-files/${CASE_ID}/conclusions/versions`;
  const VERSION_URL = `${VERSIONS_URL}/${VERSION_ID}`;
  const LIFECYCLE_URL = `${VERSION_URL}/lifecycle`;

  function doneResponse(): ConclusionResponse {
    return {
      id: 'conc-1',
      caseFileId: CASE_ID,
      status: 'DONE',
      versionNumber: 1,
      lifecycleStatus: 'DRAFT',
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
    let received: { status: string; versionNumber: number } | undefined;
    service.generate(CASE_ID).subscribe((res) => (received = res));

    const req = httpMock.expectOne(GENERATE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush({ status: 'PENDING', versionNumber: 2 });

    expect(received!.status).toBe('PENDING');
    expect(received!.versionNumber).toBe(2);
  });

  // ── SF-98-52 — versioning ───────────────────────────────────────────────

  it('listVersions → GET sur l\'URL /conclusions/versions', () => {
    let received: ConclusionVersionSummary[] | undefined;
    service.listVersions(CASE_ID).subscribe((res) => (received = res));

    const req = httpMock.expectOne(VERSIONS_URL);
    expect(req.request.method).toBe('GET');
    req.flush([
      {
        id: 'conc-2',
        versionNumber: 2,
        lifecycleStatus: 'DRAFT',
        status: 'DONE',
        generatedAt: '2026-05-18T11:00:00Z',
        createdAt: '2026-05-18T10:55:00Z',
      },
    ]);

    expect(received!.length).toBe(1);
    expect(received![0].versionNumber).toBe(2);
  });

  it('getVersion → GET sur l\'URL /conclusions/versions/{id}', () => {
    let received: ConclusionResponse | undefined;
    service
      .getVersion(CASE_ID, VERSION_ID)
      .subscribe((res) => (received = res));

    const req = httpMock.expectOne(VERSION_URL);
    expect(req.request.method).toBe('GET');
    req.flush(doneResponse());

    expect(received!.status).toBe('DONE');
  });

  it('updateLifecycle → PATCH sur /lifecycle avec le corps attendu', () => {
    let received: ConclusionResponse | undefined;
    service
      .updateLifecycle(CASE_ID, VERSION_ID, 'VALIDATED')
      .subscribe((res) => (received = res));

    const req = httpMock.expectOne(LIFECYCLE_URL);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ lifecycleStatus: 'VALIDATED' });
    req.flush({ ...doneResponse(), lifecycleStatus: 'VALIDATED' });

    expect(received!.lifecycleStatus).toBe('VALIDATED');
  });
});
