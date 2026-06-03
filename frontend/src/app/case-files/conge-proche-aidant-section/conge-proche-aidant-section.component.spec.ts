import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { CongeProcheAidantSectionComponent } from './conge-proche-aidant-section.component';
import { CongeProcheAidantResponse } from '../../core/models/conge-proche-aidant.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('CongeProcheAidantSectionComponent', () => {
  let component: CongeProcheAidantSectionComponent;
  let fixture: ComponentFixture<CongeProcheAidantSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/conge-proche-aidant-analysis';

  function eligibleResponse(overrides: Partial<CongeProcheAidantResponse> = {}): CongeProcheAidantResponse {
    return {
      caseFileId: 'case-1',
      statut: 'ELIGIBLE',
      lienPersonneAidee: 'ASCENDANT',
      personneAideeResideFrance: true,
      dureeSouhaiteeMois: 3,
      dureeMaxMois: 12,
      dureeRetenueMois: 3,
      ajpaDemandee: false,
      ajpaJournaliere: null,
      estimationAjpa: null,
      protectionEmploi: true,
      nonImputableCongesPayes: true,
      notes: ['Lien retenu : ascendant (art. L.3142-16 CT).'],
      country: 'FRANCE',
      baseJuridique: 'art. L.3142-16 à L.3142-27 du Code du travail (à vérifier par avocat)',
      ...overrides,
    };
  }

  function nonEligibleResponse(): CongeProcheAidantResponse {
    return eligibleResponse({
      statut: 'NON_ELIGIBLE',
      personneAideeResideFrance: false,
      dureeRetenueMois: null,
      notes: ['La personne aidée doit résider en France/EEE (art. L.3142-16 CT).'],
    });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);

    await TestBed.configureTestingModule({
      imports: [CongeProcheAidantSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CongeProcheAidantSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('se crée', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('charge l\'analyse existante au ngOnInit (GET 200) et masque le formulaire', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(eligibleResponse());
    expect(component.result()?.statut).toBe('ELIGIBLE');
    expect(component.result()?.dureeMaxMois).toBe(12);
    expect(component.showForm()).toBe(false);
  });

  it('affiche le formulaire si aucune analyse (GET 404)', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid() exige un lien et une durée > 0', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    expect(component.formValid()).toBe(false);
    component.onLienChange('ASCENDANT');
    expect(component.formValid()).toBe(false);
    component.onDureeChange(0);
    expect(component.formValid()).toBe(false);
    component.onDureeChange(3);
    expect(component.formValid()).toBe(true);
  });

  it('POST éligible → ELIGIBLE + dureeMaxMois affichée', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onLienChange('ASCENDANT');
    component.onDureeChange(3);
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.lienPersonneAidee).toBe('ASCENDANT');
    expect(req.request.body.dureeSouhaiteeMois).toBe(3);
    req.flush(eligibleResponse());
    expect(component.result()?.statut).toBe('ELIGIBLE');
    expect(component.result()?.dureeMaxMois).toBe(12);
  });

  it('POST personne hors France → NON_ELIGIBLE + pas de durée retenue', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onLienChange('ASCENDANT');
    component.onDureeChange(3);
    component.onResideFranceChange(false);
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.body.personneAideeResideFrance).toBe(false);
    req.flush(nonEligibleResponse());
    expect(component.result()?.statut).toBe('NON_ELIGIBLE');
    expect(component.result()?.dureeRetenueMois).toBeNull();
  });

  it('POST avec AJPA → estimation transmise / affichée', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onLienChange('DESCENDANT');
    component.onDureeChange(3);
    component.onAjpaChange(true);
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.body.ajpaDemandee).toBe(true);
    req.flush(eligibleResponse({
      lienPersonneAidee: 'DESCENDANT',
      ajpaDemandee: true,
      ajpaJournaliere: 64.54,
      estimationAjpa: 4259.64,
    }));
    expect(component.result()?.estimationAjpa).toBe(4259.64);
    expect(component.result()?.ajpaJournaliere).toBe(64.54);
  });

  it('statutChipClass distingue ELIGIBLE (success) et NON_ELIGIBLE (danger)', () => {
    expect(component.statutChipClass('ELIGIBLE')).toContain('is-chip--success');
    expect(component.statutChipClass('NON_ELIGIBLE')).toContain('is-chip--danger');
  });

  it('pré-remplit le lien depuis Sf218dDetail (snake_case) avec badge IA', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    const aiData = { lien_personne_aidee: 'CONJOINT' } as unknown as TravailExtractedData;
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, false) });
    expect(component.lienPersonneAidee()).toBe('CONJOINT');
    expect(component.provenanceLien()).toBe('IA');
  });

  it('getPrefillCount = 1 (nominal), 0 (vide), 0 (BE)', () => {
    expect(CongeProcheAidantSectionComponent.getPrefillCount({
      aiData: { lien_personne_aidee: 'ASCENDANT' } as any,
      workspaceCountry: 'FRANCE',
    })).toBe(1);
    expect(CongeProcheAidantSectionComponent.getPrefillCount({})).toBe(0);
    expect(CongeProcheAidantSectionComponent.getPrefillCount({
      aiData: { lien_personne_aidee: 'ASCENDANT' } as any,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('coherenceAlerts signale une divergence lien IA ↔ saisie', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.aiData = { lien_personne_aidee: 'ASCENDANT' } as unknown as TravailExtractedData;
    component.onLienChange('CONJOINT'); // IA dit ASCENDANT, saisie dit CONJOINT
    const alerts = component.coherenceAlerts();
    expect(alerts.LIEN).toBeTruthy();
    expect(alerts.LIEN!.field).toBe('LIEN');
  });

  it('gate FR : isFrance() false en BELGIQUE et pas d\'appel GET', () => {
    component.workspaceCountry = 'BELGIQUE';
    fixture.detectChanges();
    expect(component.isFrance()).toBe(false);
    httpMock.expectNone(BASE_URL);
  });

  it('error POST → snackBar erreur sans casser le composant', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onLienChange('ASCENDANT');
    component.onDureeChange(3);
    component.analyze();
    httpMock.expectOne(BASE_URL).flush({ message: 'boom' }, { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalled();
    expect(component.analyzing()).toBe(false);
  });
});
