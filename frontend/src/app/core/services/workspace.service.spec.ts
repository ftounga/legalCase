import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { WorkspaceService } from './workspace.service';

describe('WorkspaceService', () => {
  let service: WorkspaceService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [WorkspaceService, provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(WorkspaceService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // S-01 : createWorkspace envoie { name, legalDomain, country } dans le body
  it('createWorkspace envoie body { name, legalDomain, country }', () => {
    service.createWorkspace('Cabinet Martin', 'DROIT_DU_TRAVAIL', 'FRANCE').subscribe();

    const req = httpMock.expectOne('/api/v1/workspaces');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ name: 'Cabinet Martin', legalDomain: 'DROIT_DU_TRAVAIL', country: 'FRANCE' });
    req.flush({});
  });

  // S-02 : createWorkspace avec BELGIQUE
  it('createWorkspace passe le pays BELGIQUE dans le body', () => {
    service.createWorkspace('Étude Bruxelles', 'DROIT_FAMILLE', 'BELGIQUE').subscribe();

    const req = httpMock.expectOne('/api/v1/workspaces');
    expect(req.request.body).toEqual({ name: 'Étude Bruxelles', legalDomain: 'DROIT_FAMILLE', country: 'BELGIQUE' });
    req.flush({});
  });
});
