import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { RecepisseAttestationSectionComponent } from './recepisse-attestation-section.component';
import { RecepisseAttestationResponse } from '../../core/models/recepisse-attestation.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('RecepisseAttestationSectionComponent', () => {
  let component: RecepisseAttestationSectionComponent;
  let fixture: ComponentFixture<RecepisseAttestationSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/recepisse-attestation-analysis';

  function frResponse(overrides: Partial<RecepisseAttestationResponse> = {}): RecepisseAttestationResponse {
    return {
      caseFileId: 'case-1',
      typeDocument: 'RECEPISSE',
      dateDelivrance: '2025-09-01',
      dateExpiration: '2026-03-01',
      country: 'FRANCE',
      droitSejour: true,
      droitTravail: true,
      dureeValiditeJours: 181,
      risqueEmployeur: false,
      recommandations: ['Conserver le récépissé jusqu\'à délivrance du titre'],
      baseJuridique: 'CESEDA R.431-16',
      ...overrides,
    };
  }

  function flush404(): void {
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  function fillValidForm(): void {
    component.typeDocument.set('RECEPISSE');
    component.dateDelivrance.set('2025-09-01');
    component.dateExpiration.set('2026-03-01');
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        RecepisseAttestationSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
        RouterTestingModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(RecepisseAttestationSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match((r) => r.url.includes('/jurisprudence-citations')).forEach((r) => r.flush({ items: [] }));
    httpMock.verify();
  });

  it('creates', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('FRANCE : charge l\'analyse existante au ngOnInit', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(frResponse());
    expect(component.result()?.typeDocument).toBe('RECEPISSE');
    expect(component.showForm()).toBe(false);
  });

  it('404 : bascule sur le formulaire', () => {
    fixture.detectChanges();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('gate FRANCE : aucun appel HTTP en Belgique + bannière BE', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    expect(component.isFrance()).toBe(false);
    const banner = fixture.nativeElement.querySelector('[data-testid="be-banner"]');
    expect(banner).toBeTruthy();
  });

  it('formValid : exige type + dates ISO cohérentes (délivrance ≤ expiration)', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    expect(component.formValid()).toBe(false);
    component.typeDocument.set('RECEPISSE');
    expect(component.formValid()).toBe(false);
    component.dateDelivrance.set('2025-09-01');
    expect(component.formValid()).toBe(false);
    component.dateExpiration.set('2024-01-01'); // antérieure → invalide
    expect(component.formValid()).toBe(false);
    component.dateExpiration.set('2026-03-01');
    expect(component.formValid()).toBe(true);
  });

  it('analyze : POST + bascule sur le résultat + snackbar', () => {
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.typeDocument).toBe('RECEPISSE');
    expect(req.request.body.dateExpiration).toBe('2026-03-01');
    req.flush(frResponse());
    expect(component.showForm()).toBe(false);
    expect(component.result()?.droitSejour).toBe(true);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze : erreur HTTP → snackbar erreur, reste sur le formulaire', () => {
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Boom' }, { status: 500, statusText: 'Server Error' });
    expect(component.analyzing()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('ATTESTATION : bannière orange risqueEmployeur (L.8253-1) rendue', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(frResponse({
      typeDocument: 'ATTESTATION_PROLONGATION',
      droitTravail: false,
      risqueEmployeur: true,
    }));
    component.collapsed.set(false);
    fixture.detectChanges();
    const risque = fixture.nativeElement.querySelector('[data-testid="risque-employeur"]');
    expect(risque).toBeTruthy();
    expect(risque.textContent).toContain('L. 8253-1');
    const badgeTravail = fixture.nativeElement.querySelector('[data-testid="badge-travail"]');
    expect(badgeTravail.textContent).toContain('non autorisé');
  });

  it('RECEPISSE sans risque : pas de bannière risqueEmployeur', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(frResponse());
    component.collapsed.set(false);
    fixture.detectChanges();
    const risque = fixture.nativeElement.querySelector('[data-testid="risque-employeur"]');
    expect(risque).toBeNull();
    const badgeTravail = fixture.nativeElement.querySelector('[data-testid="badge-travail"]');
    expect(badgeTravail.textContent).toContain('autorisé');
  });

  it('prefillFromAi : pré-remplit type + dateExpiration depuis ImmigrationExtractedData', () => {
    const aiData = {
      recepisseOuAttestationType: 'ATTESTATION_PROLONGATION',
      dateExpirationTitre: '2026-04-30',
    } as ImmigrationExtractedData;
    fixture.detectChanges();
    flush404();
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, false) });
    expect(component.typeDocument()).toBe('ATTESTATION_PROLONGATION');
    expect(component.provenanceType()).toBe('IA');
    expect(component.dateExpiration()).toBe('2026-04-30');
    expect(component.provenanceExpiration()).toBe('IA');
  });

  it('getPrefillCount static reflète le runtime (2 champs)', () => {
    const count = RecepisseAttestationSectionComponent.getPrefillCount({
      aiData: { recepisseOuAttestationType: 'RECEPISSE', dateExpirationTitre: '2026-03-01' },
      workspaceCountry: 'FRANCE',
    });
    expect(count).toBe(2);
  });

  it('toggleCollapse inverse l\'état replié', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    const before = component.collapsed();
    component.toggleCollapse();
    expect(component.collapsed()).toBe(!before);
  });

  it('standaloneMode : pas d\'appel HTTP, formulaire visible', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    expect(component.showForm()).toBe(true);
    expect(component.collapsed()).toBe(false);
  });
});
