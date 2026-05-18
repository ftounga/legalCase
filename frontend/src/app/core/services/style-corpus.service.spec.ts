import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { StyleCorpusService } from './style-corpus.service';
import { StyleCorpusDocumentSummary } from '../models/style-corpus.model';

describe('StyleCorpusService', () => {
  let service: StyleCorpusService;
  let httpMock: HttpTestingController;

  const WS = 'ws-1';
  const BASE = `/api/v1/workspaces/${WS}/style-corpus/documents`;

  function summary(
    overrides: Partial<StyleCorpusDocumentSummary> = {},
  ): StyleCorpusDocumentSummary {
    return {
      id: 'doc-1',
      originalFilename: 'conclusions-ref.pdf',
      status: 'DONE',
      active: true,
      createdAt: '2026-05-18T10:00:00Z',
      errorMessage: null,
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [StyleCorpusService],
    });
    service = TestBed.inject(StyleCorpusService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('upload : POST multipart sur la bonne URL', () => {
    const file = new File(['x'], 'ref.pdf', { type: 'application/pdf' });
    let result: { id: string; status: string } | undefined;

    service.upload(WS, file).subscribe((r) => (result = r));

    const req = httpMock.expectOne(BASE);
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBe(true);
    expect((req.request.body as FormData).get('file')).toBe(file);

    req.flush({ id: 'doc-1', status: 'PENDING' });
    expect(result).toEqual({ id: 'doc-1', status: 'PENDING' });
  });

  it('list : GET sur la bonne URL', () => {
    let result: StyleCorpusDocumentSummary[] | undefined;

    service.list(WS).subscribe((r) => (result = r));

    const req = httpMock.expectOne(BASE);
    expect(req.request.method).toBe('GET');

    const docs = [summary()];
    req.flush(docs);
    expect(result).toEqual(docs);
  });

  it('setActive : PATCH sur .../{id} avec le corps {active}', () => {
    let result: StyleCorpusDocumentSummary | undefined;

    service.setActive(WS, 'doc-1', false).subscribe((r) => (result = r));

    const req = httpMock.expectOne(`${BASE}/doc-1`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ active: false });

    const updated = summary({ active: false });
    req.flush(updated);
    expect(result).toEqual(updated);
  });

  it('remove : DELETE sur .../{id}', () => {
    let done = false;

    service.remove(WS, 'doc-1').subscribe(() => (done = true));

    const req = httpMock.expectOne(`${BASE}/doc-1`);
    expect(req.request.method).toBe('DELETE');

    req.flush(null);
    expect(done).toBe(true);
  });
});
