import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { CaseFileStatusService } from './case-file-status.service';

describe('CaseFileStatusService', () => {
  let service: CaseFileStatusService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        CaseFileStatusService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(CaseFileStatusService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('close() should call PATCH /api/v1/case-files/{id}/close', () => {
    const id = 'case-1';
    service.close(id).subscribe();
    const req = httpMock.expectOne(`/api/v1/case-files/${id}/close`);
    expect(req.request.method).toBe('PATCH');
    req.flush({});
  });

  it('reopen() should call PATCH /api/v1/case-files/{id}/reopen', () => {
    const id = 'case-2';
    service.reopen(id).subscribe();
    const req = httpMock.expectOne(`/api/v1/case-files/${id}/reopen`);
    expect(req.request.method).toBe('PATCH');
    req.flush({});
  });

  it('delete() should call DELETE /api/v1/case-files/{id}', () => {
    const id = 'case-3';
    service.delete(id).subscribe();
    const req = httpMock.expectOne(`/api/v1/case-files/${id}`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
