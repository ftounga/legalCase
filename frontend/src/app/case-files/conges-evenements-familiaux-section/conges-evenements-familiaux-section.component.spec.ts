import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { CongesEvenementsFamiliauxSectionComponent } from './conges-evenements-familiaux-section.component';
import { CongesEvenementsFamiliauxResponse } from '../../core/models/conges-evenements-familiaux.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('CongesEvenementsFamiliauxSectionComponent', () => {
  let component: CongesEvenementsFamiliauxSectionComponent;
  let fixture: ComponentFixture<CongesEvenementsFamiliauxSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/conges-evenements-familiaux-analysis';

  function mariageResponse(overrides: Partial<CongesEvenementsFamiliauxResponse> = {}): CongesEvenementsFamiliauxResponse {
    return {
      caseFileId: 'case-1',
      typeEvenement: 'MARIAGE_PACS',
      conventionPlusFavorable: false,
      dureeConventionnelleJours: null,
      dureeLegaleJours: 4,
      dureeApplicableJours: 4,
      base: 'LEGALE',
      maintienSalaire: true,
      assimileTempsTravailEffectif: true,
      dureeMajoreePossible: false,
      notes: ['Mariage ou PACS du salarié : congé légal de 4 jours.'],
      country: 'FRANCE',
      baseJuridique: 'art. L.3142-1 à L.3142-5 du Code du travail (à vérifier par avocat)',
      ...overrides,
    };
  }

  function decesEnfantResponse(): CongesEvenementsFamiliauxResponse {
    return mariageResponse({
      typeEvenement: 'DECES_ENFANT',
      dureeLegaleJours: 5,
      dureeApplicableJours: 5,
      dureeMajoreePossible: true,
      notes: ['Décès d\'un enfant : congé légal de 5 jours, porté à 7 jours ouvrés...'],
    });
  }

  function conventionnelleResponse(): CongesEvenementsFamiliauxResponse {
    return mariageResponse({
      typeEvenement: 'NAISSANCE',
      conventionPlusFavorable: true,
      dureeConventionnelleJours: 5,
      dureeLegaleJours: 3,
      dureeApplicableJours: 5,
      base: 'CONVENTIONNELLE',
    });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);

    await TestBed.configureTestingModule({
      imports: [CongesEvenementsFamiliauxSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CongesEvenementsFamiliauxSectionComponent);
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
    req.flush(mariageResponse());
    expect(component.result()?.typeEvenement).toBe('MARIAGE_PACS');
    expect(component.result()?.dureeApplicableJours).toBe(4);
    expect(component.showForm()).toBe(false);
  });

  it('affiche le formulaire si aucune analyse (GET 404)', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid() exige un type d\'évènement sélectionné', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    expect(component.formValid()).toBe(false);
    component.onTypeChange('MARIAGE_PACS');
    expect(component.formValid()).toBe(true);
  });

  it('formValid() exige une durée conventionnelle > 0 si conventionPlusFavorable', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    component.onTypeChange('NAISSANCE');
    component.onConventionChange(true);
    expect(component.formValid()).toBe(false);
    component.onDureeConventionnelleChange(5);
    expect(component.formValid()).toBe(true);
  });

  it('POST mariage → durée 4 j base LEGALE + maintien salaire vert', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onTypeChange('MARIAGE_PACS');
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.typeEvenement).toBe('MARIAGE_PACS');
    req.flush(mariageResponse());
    expect(component.result()?.dureeApplicableJours).toBe(4);
    expect(component.result()?.base).toBe('LEGALE');
    expect(component.result()?.maintienSalaire).toBe(true);
  });

  it('POST décès enfant → durée 5 j + dureeMajoreePossible true', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onTypeChange('DECES_ENFANT');
    component.analyze();
    httpMock.expectOne(BASE_URL).flush(decesEnfantResponse());
    expect(component.result()?.dureeApplicableJours).toBe(5);
    expect(component.result()?.dureeMajoreePossible).toBe(true);
  });

  it('POST naissance + CCN plus favorable → durée 5 j base CONVENTIONNELLE', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onTypeChange('NAISSANCE');
    component.onConventionChange(true);
    component.onDureeConventionnelleChange(5);
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.body.conventionPlusFavorable).toBe(true);
    expect(req.request.body.dureeConventionnelleJours).toBe(5);
    req.flush(conventionnelleResponse());
    expect(component.result()?.base).toBe('CONVENTIONNELLE');
    expect(component.result()?.dureeApplicableJours).toBe(5);
  });

  it('décocher conventionPlusFavorable réinitialise la durée conventionnelle', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    component.onConventionChange(true);
    component.onDureeConventionnelleChange(7);
    expect(component.dureeConventionnelleJours()).toBe(7);
    component.onConventionChange(false);
    expect(component.dureeConventionnelleJours()).toBeNull();
  });

  it('changement de type recalcule (nouveau POST renvoie une durée différente)', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onTypeChange('MARIAGE_PACS');
    component.analyze();
    httpMock.expectOne(BASE_URL).flush(mariageResponse());
    expect(component.result()?.dureeApplicableJours).toBe(4);

    component.editMode();
    component.onTypeChange('DECES_ENFANT');
    component.analyze();
    httpMock.expectOne(BASE_URL).flush(decesEnfantResponse());
    expect(component.result()?.dureeApplicableJours).toBe(5);
  });

  it('baseChipClass distingue CONVENTIONNELLE (info) et LEGALE (neutral)', () => {
    expect(component.baseChipClass('CONVENTIONNELLE')).toContain('is-chip--info');
    expect(component.baseChipClass('LEGALE')).toContain('is-chip--neutral');
  });

  it('pré-remplit le type d\'évènement depuis Sf218dDetail (snake_case) avec badge IA', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    const aiData = { type_evenement_familial: 'DECES_CONJOINT' } as unknown as TravailExtractedData;
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, false) });
    expect(component.typeEvenement()).toBe('DECES_CONJOINT_PARTENAIRE');
    expect(component.provenanceType()).toBe('IA');
  });

  it('getPrefillCount = 1 (nominal), 0 (vide), 0 (BE)', () => {
    expect(CongesEvenementsFamiliauxSectionComponent.getPrefillCount({
      aiData: { type_evenement_familial: 'MARIAGE' } as any,
      workspaceCountry: 'FRANCE',
    })).toBe(1);
    expect(CongesEvenementsFamiliauxSectionComponent.getPrefillCount({})).toBe(0);
    expect(CongesEvenementsFamiliauxSectionComponent.getPrefillCount({
      aiData: { type_evenement_familial: 'MARIAGE' } as any,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('coherenceAlerts signale une divergence type IA ↔ saisie', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.aiData = { type_evenement_familial: 'MARIAGE' } as unknown as TravailExtractedData;
    component.onTypeChange('NAISSANCE'); // IA dit mariage, saisie dit naissance
    const alerts = component.coherenceAlerts();
    expect(alerts.TYPE_EVENEMENT).toBeTruthy();
    expect(alerts.TYPE_EVENEMENT!.field).toBe('TYPE_EVENEMENT');
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
    component.onTypeChange('MARIAGE_PACS');
    component.analyze();
    httpMock.expectOne(BASE_URL).flush({ message: 'boom' }, { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalled();
    expect(component.analyzing()).toBe(false);
  });
});
