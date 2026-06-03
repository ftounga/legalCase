import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { VictimeTraiteBeSectionComponent } from './victime-traite-be-section.component';
import { VictimeTraiteBeResponse } from '../../core/models/victime-traite-be.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('VictimeTraiteBeSectionComponent', () => {
  let component: VictimeTraiteBeSectionComponent;
  let fixture: ComponentFixture<VictimeTraiteBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/victime-traite-be-analysis';

  function beResponse(
    overrides: Partial<VictimeTraiteBeResponse> = {},
  ): VictimeTraiteBeResponse {
    return {
      caseFileId: 'case-1',
      phaseProcedure: 'DECLARATION_FAITE',
      ruptureAvecReseau: true,
      cooperationJudiciaire: false,
      accompagnementCentreSpecialise: true,
      dateDebutAccompagnement: '2026-05-30',
      verdict: 'ELIGIBLE_TITRE_TEMPORAIRE',
      etapeProcedure: 'Déclaration faite',
      basesJuridiques: ['Loi du 15/12/1980 art. 61/2 et s. (à vérifier par avocat)'],
      messages: ['Éligible au titre temporaire.'],
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
      imports: [VictimeTraiteBeSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(VictimeTraiteBeSectionComponent);
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
    expect(VictimeTraiteBeSectionComponent.TOOL_LABEL).toContain('VICTIME DE LA TRAITE');
    expect(VictimeTraiteBeSectionComponent.TOOL_ICON).toBe('support');
  });

  // ---- getPrefillCount : 0 / partiel / nominal=3 ----
  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(VictimeTraiteBeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 1 on partial pré-fill (phase only)', () => {
    expect(VictimeTraiteBeSectionComponent.getPrefillCount({
      aiData: { victimeTraitePhase: 'REFLEXION_45J' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(1);
  });

  it('static getPrefillCount returns 3 (nominal) when all real signals present (BELGIQUE)', () => {
    expect(VictimeTraiteBeSectionComponent.getPrefillCount({
      aiData: {
        victimeTraitePhase: 'PROCEDURE_PENALE_EN_COURS',
        victimeTraiteRupture: true,
        victimeTraiteAccompagnement: false,
      },
      workspaceCountry: 'BELGIQUE',
    })).toBe(3);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=FRANCE', () => {
    expect(VictimeTraiteBeSectionComponent.getPrefillCount({
      aiData: { victimeTraitePhase: 'DECLARATION_FAITE' },
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
    expect(component.result()!.verdict).toBe('ELIGIBLE_TITRE_TEMPORAIRE');
    expect(component.showForm()).toBe(false);
    expect(component.phaseProcedure()).toBe('DECLARATION_FAITE');
    expect(component.accompagnementCentreSpecialise()).toBe(true);
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ---- form validation (bouton désactivé si pas de phase) ----
  it('formValid false until phase present; true once phase set', () => {
    expect(component.formValid()).toBe(false);
    component.phaseProcedure.set('AUCUNE');
    expect(component.formValid()).toBe(true);
  });

  // ---- pré-fill IA (3 champs réels) ----
  it('prefills the 3 real fields from aiData on ngOnChanges (BELGIQUE)', () => {
    const aiData: ImmigrationExtractedData = {
      victimeTraitePhase: 'PROCEDURE_PENALE_EN_COURS',
      victimeTraiteRupture: true,
      victimeTraiteAccompagnement: false,
    } as ImmigrationExtractedData;
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, true) });
    expect(component.phaseProcedure()).toBe('PROCEDURE_PENALE_EN_COURS');
    expect(component.ruptureAvecReseau()).toBe(true);
    expect(component.accompagnementCentreSpecialise()).toBe(false);
    expect(component.provenancePhase()).toBe('IA');
    expect(component.provenanceRupture()).toBe('IA');
    expect(component.provenanceAccompagnement()).toBe('IA');
  });

  // ---- POST nominal ----
  it('POST on analyze() and switches to result mode', () => {
    component.phaseProcedure.set('PROCEDURE_PENALE_EN_COURS');
    component.ruptureAvecReseau.set(true);
    component.cooperationJudiciaire.set(true);
    component.accompagnementCentreSpecialise.set(true);
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.phaseProcedure).toBe('PROCEDURE_PENALE_EN_COURS');
    expect(req.request.body.cooperationJudiciaire).toBe(true);
    req.flush(beResponse({ verdict: 'ELIGIBLE_SOUS_PROCEDURE_PENALE', cooperationJudiciaire: true }));
    expect(component.result()!.verdict).toBe('ELIGIBLE_SOUS_PROCEDURE_PENALE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('does not POST when form invalid (no phase)', () => {
    component.analyze();
    httpMock.expectNone(BASE_URL);
  });

  // ---- verdict mapping ----
  it('maps verdict to chip/icon/label classes', () => {
    expect(component.verdictChipClass('ELIGIBLE_TITRE_TEMPORAIRE')).toContain('success');
    expect(component.verdictChipClass('ELIGIBLE_SOUS_PROCEDURE_PENALE')).toContain('success');
    expect(component.verdictChipClass('CONDITIONS_NON_REUNIES')).toContain('warning');
    expect(component.verdictChipClass('DELAI_REFLEXION')).toContain('info');
    expect(component.verdictChipClass('A_ORIENTER_CENTRE')).toContain('info');
    expect(component.verdictLabel('A_ORIENTER_CENTRE')).toBe('Orienter vers un centre');
    expect(component.verdictIcon('ELIGIBLE_TITRE_TEMPORAIRE')).toBe('verified');
  });

  it('phaseLabel maps the phase enum to a human label', () => {
    expect(component.phaseLabel('REFLEXION_45J')).toContain('réflexion');
    expect(component.phaseLabel(null)).toBe('—');
  });

  it('formatDateFr converts ISO to dd/mm/yyyy', () => {
    expect(component.formatDateFr('2026-05-30')).toBe('30/05/2026');
    expect(component.formatDateFr(null)).toBe('—');
  });
});
