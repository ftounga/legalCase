import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { ResidenceLongueDureeUeBeSectionComponent } from './residence-longue-duree-ue-be-section.component';
import { ResidenceLongueDureeUeBeResponse } from '../../core/models/residence-longue-duree-ue-be.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('ResidenceLongueDureeUeBeSectionComponent', () => {
  let component: ResidenceLongueDureeUeBeSectionComponent;
  let fixture: ComponentFixture<ResidenceLongueDureeUeBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/residence-longue-duree-ue-be-analysis';

  function beResponse(
    overrides: Partial<ResidenceLongueDureeUeBeResponse> = {},
  ): ResidenceLongueDureeUeBeResponse {
    return {
      caseFileId: 'case-1',
      dateDebutSejourLegal: '2020-01-01',
      sejourLegalIninterrompu: true,
      ressourcesStablesSuffisantes: true,
      assuranceMaladie: true,
      conditionIntegrationRemplie: true,
      absencesHorsUeExcessives: false,
      verdict: 'ELIGIBLE',
      dureeSejourMois: 72,
      moisRestants: 0,
      conditionsManquantes: [],
      basesJuridiques: ['Loi du 15/12/1980 art. 15bis (à vérifier par avocat)'],
      messages: ['Conditions réunies.'],
      ...overrides,
    };
  }

  function flush404(): void {
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [ResidenceLongueDureeUeBeSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(ResidenceLongueDureeUeBeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match((r) => r.url.includes('/jurisprudence-citations')).forEach((r) => r.flush({ items: [] }));
    httpMock.verify();
  });

  it('exposes TOOL_LABEL and TOOL_ICON statics', () => {
    expect(ResidenceLongueDureeUeBeSectionComponent.TOOL_LABEL).toContain('RÉSIDENT LONGUE DURÉE UE');
    expect(ResidenceLongueDureeUeBeSectionComponent.TOOL_ICON).toBe('public');
  });

  // ---- getPrefillCount : 0 / partiel / nominal=4 ----
  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(ResidenceLongueDureeUeBeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 1 on partial pré-fill (date only)', () => {
    expect(ResidenceLongueDureeUeBeSectionComponent.getPrefillCount({
      aiData: { rlueDateDebutSejour: '2020-01-01' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(1);
  });

  it('static getPrefillCount returns 4 (nominal) when all real signals present (BELGIQUE)', () => {
    expect(ResidenceLongueDureeUeBeSectionComponent.getPrefillCount({
      aiData: {
        rlueDateDebutSejour: '2020-01-01',
        rlueRessourcesSuffisantes: true,
        rlueAssuranceMaladie: true,
        rlueIntegrationRemplie: true,
      },
      workspaceCountry: 'BELGIQUE',
    })).toBe(4);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=FRANCE', () => {
    expect(ResidenceLongueDureeUeBeSectionComponent.getPrefillCount({
      aiData: { rlueDateDebutSejour: '2020-01-01' },
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });

  // ---- HTTP lifecycle ----
  it('BELGIQUE -> GET called on ngOnInit', () => {
    expect(component.isBelgique()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'NF' }, { status: 404, statusText: 'NF' });
  });

  it('FRANCE -> no HTTP on ngOnInit', () => {
    component.workspaceCountry = 'FRANCE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('loads existing analysis on GET 200', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(beResponse());
    expect(component.result()!.verdict).toBe('ELIGIBLE');
    expect(component.showForm()).toBe(false);
    expect(component.dateDebutSejourLegal()).toBe('2020-01-01');
    expect(component.assuranceMaladie()).toBe(true);
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ---- form validation (bouton désactivé si vide) ----
  it('formValid false until date present; true once date set', () => {
    expect(component.formValid()).toBe(false);
    component.dateDebutSejourLegal.set('2020-01-01');
    expect(component.formValid()).toBe(true);
  });

  // ---- pré-fill IA (4 champs) ----
  it('prefills the 4 real fields from aiData on ngOnChanges (BELGIQUE)', () => {
    const aiData: ImmigrationExtractedData = {
      rlueDateDebutSejour: '2019-03-15',
      rlueRessourcesSuffisantes: true,
      rlueAssuranceMaladie: false,
      rlueIntegrationRemplie: true,
    } as ImmigrationExtractedData;
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, true) });
    expect(component.dateDebutSejourLegal()).toBe('2019-03-15');
    expect(component.ressourcesStablesSuffisantes()).toBe(true);
    expect(component.assuranceMaladie()).toBe(false);
    expect(component.conditionIntegrationRemplie()).toBe(true);
    expect(component.provenanceDateDebut()).toBe('IA');
  });

  // ---- POST nominal ----
  it('POST on analyze() and switches to result mode', () => {
    component.dateDebutSejourLegal.set('2020-01-01');
    component.sejourLegalIninterrompu.set(true);
    component.ressourcesStablesSuffisantes.set(true);
    component.assuranceMaladie.set(true);
    component.conditionIntegrationRemplie.set(true);
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.dateDebutSejourLegal).toBe('2020-01-01');
    req.flush(beResponse());
    expect(component.result()!.verdict).toBe('ELIGIBLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('does not POST when form invalid', () => {
    component.analyze();
    httpMock.expectNone(BASE_URL);
  });

  // ---- conditions matérielles non réunies ----
  it('exposes conditionsManquantes on CONDITIONS_MATERIELLES_NON_REUNIES verdict', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(beResponse({
      verdict: 'CONDITIONS_MATERIELLES_NON_REUNIES',
      ressourcesStablesSuffisantes: false,
      conditionsManquantes: ['Ressources stables, régulières et suffisantes'],
    }));
    expect(component.result()!.verdict).toBe('CONDITIONS_MATERIELLES_NON_REUNIES');
    expect(component.result()!.conditionsManquantes.length).toBe(1);
  });

  // ---- verdict mapping ----
  it('maps verdict to banner/chip/icon/label classes', () => {
    expect(component.verdictChipClass('ELIGIBLE')).toContain('success');
    expect(component.verdictChipClass('DUREE_INSUFFISANTE')).toContain('warning');
    expect(component.verdictChipClass('CONDITIONS_MATERIELLES_NON_REUNIES')).toContain('warning');
    expect(component.verdictChipClass('CONTINUITE_ROMPUE')).toContain('danger');
    expect(component.verdictChipClass('A_EXAMINER')).toContain('info');
    expect(component.verdictLabel('CONTINUITE_ROMPUE')).toBe('Continuité rompue');
    expect(component.verdictLabel('CONDITIONS_MATERIELLES_NON_REUNIES'))
      .toBe('Conditions matérielles non réunies');
    expect(component.verdictIcon('ELIGIBLE')).toBe('verified');
  });

  it('formatDateFr converts ISO to dd/mm/yyyy', () => {
    expect(component.formatDateFr('2020-01-01')).toBe('01/01/2020');
    expect(component.formatDateFr(null)).toBe('—');
  });
});
