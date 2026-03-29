import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { CaseNoteService } from './case-note.service';

describe('CaseNoteService', () => {
  let service: CaseNoteService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [CaseNoteService, provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(CaseNoteService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should GET notes for a case file', () => {
    service.list('cf-1').subscribe();
    httpMock.expectOne('/api/v1/case-files/cf-1/notes').flush([]);
  });

  it('should POST to create a note', () => {
    service.create('cf-1', 'Ma note').subscribe();
    const req = httpMock.expectOne('/api/v1/case-files/cf-1/notes');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ content: 'Ma note' });
    req.flush({});
  });

  it('should PUT to update a note', () => {
    service.update('cf-1', 'n-1', 'Modifiée').subscribe();
    const req = httpMock.expectOne('/api/v1/case-files/cf-1/notes/n-1');
    expect(req.request.method).toBe('PUT');
    req.flush({});
  });

  it('should DELETE a note', () => {
    service.delete('cf-1', 'n-1').subscribe();
    const req = httpMock.expectOne('/api/v1/case-files/cf-1/notes/n-1');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
