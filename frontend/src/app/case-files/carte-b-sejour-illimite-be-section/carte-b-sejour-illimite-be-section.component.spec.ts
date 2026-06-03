import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { CarteBSejourIllimiteBeSectionComponent } from './carte-b-sejour-illimite-be-section.component';
import { CarteBSejourIllimiteBeResponse } from '../../core/models/carte-b-sejour-illimite-be.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('CarteBSejourIllimiteBeSectionComponent', () => {
  let component: CarteBSejourIllimiteBeSectionComponent;
  let fixture: ComponentFixture<CarteBSejourIllimiteBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/carte-b-sejour-illimite-be-analysis';

  function beResponse(
    overrides: Partial<CarteBSejourIllimiteBeResponse> = {},
  ): CarteBSejourIllimiteBeResponse {
    return {
      caseFileId: 'case-1',
      dateDebutSejourRegulier: '2020-01-01',
      sejourIninterrompu: true,
      absencesSuperieuresLimites: false,
      motifSejourStable: true,
      ordrePublicRisque: false,
      verdict: 'ELIGIBLE',
      dureeSejourMois: 72,
      moisRestants: 0,
      basesJuridiques: ['Loi du 15/12/1980 art. 14 (à vérifier par avocat)'],
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
      imports: [CarteBSejourIllimiteBeSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(CarteBSejourIllimiteBeSectionComponent);
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
    expect(CarteBSejourIllimiteBeSectionComponent.TOOL_LABEL).toContain('CARTE B SÉJOUR ILLIMITÉ');
    expect(CarteBSejourIllimiteBeSectionComponent.TOOL_ICON).toBe('all_inclusive');
  });

  // ---- getPrefillCount : 0 / partiel / nominal=3 ----
  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(CarteBSejourIllimiteBeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 1 on partial pré-fill (date only)', () => {
    expect(CarteBSejourIllimiteBeSectionComponent.getPrefillCount({
      aiData: { carteBDateDebutSejour: '2020-01-01' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(1);
  });

  it('static getPrefillCount returns 3 (nominal) when all real signals present (BELGIQUE)', () => {
    expect(CarteBSejourIllimiteBeSectionComponent.getPrefillCount({
      aiData: {
        carteBDateDebutSejour: '2020-01-01',
        carteBSejourIninterrompu: true,
        carteBMotifStable: true,
      },
      workspaceCountry: 'BELGIQUE',
    })).toBe(3);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=FRANCE', () => {
    expect(CarteBSejourIllimiteBeSectionComponent.getPrefillCount({
      aiData: { carteBDateDebutSejour: '2020-01-01' },
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
    expect(component.dateDebutSejourRegulier()).toBe('2020-01-01');
    expect(component.motifSejourStable()).toBe(true);
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
    component.dateDebutSejourRegulier.set('2020-01-01');
    expect(component.formValid()).toBe(true);
  });

  // ---- pré-fill IA (3 champs) ----
  it('prefills the 3 real fields from aiData on ngOnChanges (BELGIQUE)', () => {
    const aiData: ImmigrationExtractedData = {
      carteBDateDebutSejour: '2019-03-15',
      carteBSejourIninterrompu: true,
      carteBMotifStable: false,
    } as ImmigrationExtractedData;
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, true) });
    expect(component.dateDebutSejourRegulier()).toBe('2019-03-15');
    expect(component.sejourIninterrompu()).toBe(true);
    expect(component.motifSejourStable()).toBe(false);
    expect(component.provenanceDateDebut()).toBe('IA');
  });

  // ---- POST nominal ----
  it('POST on analyze() and switches to result mode', () => {
    component.dateDebutSejourRegulier.set('2020-01-01');
    component.sejourIninterrompu.set(true);
    component.motifSejourStable.set(true);
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.dateDebutSejourRegulier).toBe('2020-01-01');
    req.flush(beResponse());
    expect(component.result()!.verdict).toBe('ELIGIBLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('does not POST when form invalid', () => {
    component.analyze();
    httpMock.expectNone(BASE_URL);
  });

  // ---- verdict mapping ----
  it('maps verdict to banner/chip/icon/label classes', () => {
    expect(component.verdictChipClass('ELIGIBLE')).toContain('success');
    expect(component.verdictChipClass('DUREE_INSUFFISANTE')).toContain('warning');
    expect(component.verdictChipClass('CONTINUITE_ROMPUE')).toContain('danger');
    expect(component.verdictChipClass('RISQUE_ORDRE_PUBLIC')).toContain('danger');
    expect(component.verdictChipClass('A_EXAMINER')).toContain('info');
    expect(component.verdictLabel('CONTINUITE_ROMPUE')).toBe('Continuité rompue');
    expect(component.verdictIcon('ELIGIBLE')).toBe('verified');
  });

  it('formatDateFr converts ISO to dd/mm/yyyy', () => {
    expect(component.formatDateFr('2020-01-01')).toBe('01/01/2020');
    expect(component.formatDateFr(null)).toBe('—');
  });
});
