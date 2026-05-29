import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { Annexe13quinquiesBeSectionComponent } from './annexe13quinquies-be-section.component';
import { Annexe13quinquiesBeResponse } from '../../core/models/annexe13quinquies-be.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('Annexe13quinquiesBeSectionComponent', () => {
  let component: Annexe13quinquiesBeSectionComponent;
  let fixture: ComponentFixture<Annexe13quinquiesBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/annexe13quinquies-be-analysis';

  function beResponse(
    overrides: Partial<Annexe13quinquiesBeResponse> = {},
  ): Annexe13quinquiesBeResponse {
    return {
      caseFileId: 'case-1',
      dateNotificationAnnexe: '2026-05-01',
      motifInterdictionEntree: 'SEJOUR_IRREGULIER',
      precedentSejour: false,
      recoursForme: false,
      dateRecours: null,
      dureeInterdiction: 3,
      dateFinInterdiction: '2029-05-01',
      datePossibleLevePrecoce: '2027-11-01',
      dateLimiteRecoursAnnulation: '2026-05-31',
      joursRestantsRecours: 12,
      statutRecours: 'DISPONIBLE',
      conditionsLevee: [
        "Quitter effectivement le territoire Schengen",
        "Justifier d'un délai écoulé suffisant",
      ],
      baseJuridique: 'Loi 15/12/1980 art. 74/11 et 39/2 §2',
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
      imports: [Annexe13quinquiesBeSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(Annexe13quinquiesBeSectionComponent);
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
    expect(Annexe13quinquiesBeSectionComponent.TOOL_LABEL).toContain('ANNEXE 13QUINQUIES');
    expect(Annexe13quinquiesBeSectionComponent.TOOL_ICON).toBe('block');
  });

  // ---- getPrefillCount : 0 / partiel / nominal=2 ----
  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(Annexe13quinquiesBeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 1 on partial pré-fill (date only)', () => {
    expect(Annexe13quinquiesBeSectionComponent.getPrefillCount({
      aiData: { interdictionEntreeDateNotification: '2026-05-01' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(1);
  });

  it('static getPrefillCount returns 2 (nominal) when both real signals present (BELGIQUE)', () => {
    expect(Annexe13quinquiesBeSectionComponent.getPrefillCount({
      aiData: {
        interdictionEntreeDateNotification: '2026-05-01',
        interdictionEntreeMotif: 'MENACE_ORDRE_PUBLIC',
      },
      workspaceCountry: 'BELGIQUE',
    })).toBe(2);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=FRANCE', () => {
    expect(Annexe13quinquiesBeSectionComponent.getPrefillCount({
      aiData: { interdictionEntreeDateNotification: '2026-05-01' },
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
    expect(component.result()!.statutRecours).toBe('DISPONIBLE');
    expect(component.showForm()).toBe(false);
    expect(component.dateNotificationAnnexe()).toBe('2026-05-01');
    expect(component.motifInterdictionEntree()).toBe('SEJOUR_IRREGULIER');
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ---- form validation ----
  it('formValid false until date + motif present', () => {
    expect(component.formValid()).toBe(false);
    component.dateNotificationAnnexe.set('2026-05-01');
    expect(component.formValid()).toBe(false);
    component.motifInterdictionEntree.set('SEJOUR_IRREGULIER');
    expect(component.formValid()).toBe(true);
  });

  it('formValid requires dateRecours when recoursForme=true', () => {
    component.dateNotificationAnnexe.set('2026-05-01');
    component.motifInterdictionEntree.set('SEJOUR_IRREGULIER');
    component.onRecoursFormeChange(true);
    expect(component.formValid()).toBe(false);
    component.onDateRecoursChange('2026-05-10');
    expect(component.formValid()).toBe(true);
  });

  it('unchecking recoursForme clears dateRecours', () => {
    component.onRecoursFormeChange(true);
    component.onDateRecoursChange('2026-05-10');
    expect(component.dateRecours()).toBe('2026-05-10');
    component.onRecoursFormeChange(false);
    expect(component.dateRecours()).toBeNull();
  });

  // ---- analyze POST ----
  it('analyze() POST nominal -> result + snack', () => {
    component.ngOnInit();
    flush404();
    component.dateNotificationAnnexe.set('2026-05-01');
    component.motifInterdictionEntree.set('SEJOUR_IRREGULIER');
    component.onPrecedentSejourChange(true);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateNotificationAnnexe: '2026-05-01',
      motifInterdictionEntree: 'SEJOUR_IRREGULIER',
      precedentSejour: true,
      recoursForme: false,
      dateRecours: null,
    });
    req.flush(beResponse());
    expect(component.result()!.statutRecours).toBe('DISPONIBLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() POST with recoursForme sends dateRecours', () => {
    component.ngOnInit();
    flush404();
    component.dateNotificationAnnexe.set('2026-05-01');
    component.motifInterdictionEntree.set('MENACE_ORDRE_PUBLIC');
    component.onRecoursFormeChange(true);
    component.onDateRecoursChange('2026-05-10');
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body).toEqual({
      dateNotificationAnnexe: '2026-05-01',
      motifInterdictionEntree: 'MENACE_ORDRE_PUBLIC',
      precedentSejour: false,
      recoursForme: true,
      dateRecours: '2026-05-10',
    });
    req.flush(beResponse({ statutRecours: 'FORME', recoursForme: true, dateRecours: '2026-05-10' }));
    expect(component.result()!.statutRecours).toBe('FORME');
  });

  it('analyze() backend 400 -> snack-error', () => {
    component.ngOnInit();
    flush404();
    component.dateNotificationAnnexe.set('2026-05-01');
    component.motifInterdictionEntree.set('DECISION_JUDICIAIRE');
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
  });

  // ---- pré-fill IA ----
  it('aiData with both real signals -> pre-fills + provenance IA', () => {
    component.aiData = {
      interdictionEntreeDateNotification: '2026-04-15',
      interdictionEntreeMotif: 'RAISONS_SECURITE_NATIONALE',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.dateNotificationAnnexe()).toBe('2026-04-15');
    expect(component.provenanceDateNotification()).toBe('IA');
    expect(component.motifInterdictionEntree()).toBe('RAISONS_SECURITE_NATIONALE');
    expect(component.provenanceMotif()).toBe('IA');
  });

  it('onDateNotificationChange clears provenance', () => {
    component.aiData = { interdictionEntreeDateNotification: '2026-04-15' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceDateNotification()).toBe('IA');
    component.onDateNotificationChange('2026-04-20');
    expect(component.provenanceDateNotification()).toBeNull();
  });

  it('ngOnChanges with new aiData in form mode -> re-prefill', () => {
    component.ngOnInit();
    flush404();
    expect(component.motifInterdictionEntree()).toBeNull();
    component.aiData = {
      interdictionEntreeDateNotification: '2026-03-01',
      interdictionEntreeMotif: 'ATTEINTE_INTERET_UE',
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.motifInterdictionEntree()).toBe('ATTEINTE_INTERET_UE');
    expect(component.provenanceMotif()).toBe('IA');
  });

  // ---- F-IA-03 VOIE (a) inline badge ----
  it('F-IA-03: inline alert when user date diverges from IA date', () => {
    component.aiData = { interdictionEntreeDateNotification: '2026-05-01' } as ImmigrationExtractedData;
    component.dateNotificationAnnexe.set('2026-04-10');
    component.provenanceDateNotification.set(null);
    const alert = component.dateNotificationAlert();
    expect(alert).toBeTruthy();
    expect(alert!.label).toContain('01/05/2026');
  });

  it('F-IA-03: no inline alert when user date matches IA date', () => {
    component.aiData = { interdictionEntreeDateNotification: '2026-05-01' } as ImmigrationExtractedData;
    component.dateNotificationAnnexe.set('2026-05-01');
    expect(component.dateNotificationAlert()).toBeNull();
  });

  // ---- Badge durée IE 3 / 5 / 8 ans ----
  it('dureeBadgeClass: 3 ans -> orange', () => {
    expect(component.dureeBadgeClass(3)).toContain('duree-badge--3ans');
  });

  it('dureeBadgeClass: 5 ans -> rouge', () => {
    expect(component.dureeBadgeClass(5)).toContain('duree-badge--5ans');
  });

  it('dureeBadgeClass: 8 ans -> rouge sombre', () => {
    expect(component.dureeBadgeClass(8)).toContain('duree-badge--8ans');
  });

  it('dureeLabel pluralizes correctly', () => {
    expect(component.dureeLabel(1)).toBe('1 an');
    expect(component.dureeLabel(3)).toBe('3 ans');
    expect(component.dureeLabel(null)).toBe('—');
  });

  // ---- statutRecours badge 4 états ----
  it('statutChipClass covers all 4 statuts', () => {
    expect(component.statutChipClass('DISPONIBLE')).toContain('a13q-chip--success');
    expect(component.statutChipClass('URGENT')).toContain('a13q-chip--warning');
    expect(component.statutChipClass('EXPIRE')).toContain('a13q-chip--danger');
    expect(component.statutChipClass('FORME')).toContain('a13q-chip--info');
  });

  it('statutBannerClass + statutLabel + statutIcon cover all 4 statuts', () => {
    expect(component.statutBannerClass('DISPONIBLE')).toContain('a13q-banner--success');
    expect(component.statutBannerClass('URGENT')).toContain('a13q-banner--warning');
    expect(component.statutBannerClass('EXPIRE')).toContain('a13q-banner--danger');
    expect(component.statutBannerClass('FORME')).toContain('a13q-banner--info');
    expect(component.statutLabel('URGENT')).toContain('urgent');
    expect(component.statutLabel('EXPIRE')).toContain('expiré');
    expect(component.statutLabel('FORME')).toContain('déjà formé');
    expect(component.statutIcon('EXPIRE')).toBe('event_busy');
  });

  it('showCrossLinkCceAnnulation true only for URGENT', () => {
    expect(component.showCrossLinkCceAnnulation('URGENT')).toBe(true);
    expect(component.showCrossLinkCceAnnulation('DISPONIBLE')).toBe(false);
    expect(component.showCrossLinkCceAnnulation('EXPIRE')).toBe(false);
    expect(component.showCrossLinkCceAnnulation('FORME')).toBe(false);
  });

  // ---- dates JJ/MM/YYYY + jours restants ----
  it('formatDateFr converts ISO to JJ/MM/YYYY', () => {
    expect(component.formatDateFr('2029-05-01')).toBe('01/05/2029');
    expect(component.formatDateFr(null)).toBe('—');
    expect(component.formatDateFr('not-a-date')).toBe('—');
  });

  it('joursRestantsClass is negative-coloured when délai dépassé', () => {
    expect(component.joursRestantsClass(12)).toBe('a13q-jours');
    expect(component.joursRestantsClass(0)).toBe('a13q-jours');
    expect(component.joursRestantsClass(-3)).toContain('a13q-jours--negative');
  });

  it('motifLabel resolves codes and empty for null', () => {
    expect(component.motifLabel('SEJOUR_IRREGULIER')).toContain('irrégulier');
    expect(component.motifLabel('MENACE_ORDRE_PUBLIC')).toContain('ordre public');
    expect(component.motifLabel(null)).toBe('');
  });

  // ---- rendering : anti-régression VOIE (a) — no binding errors ----
  it('renders form fields without binding errors (standaloneMode)', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    const dateInput = fixture.nativeElement.querySelector('[data-testid="date-notification-input"]');
    const motifSelect = fixture.nativeElement.querySelector('[data-testid="motif-select"]');
    const precedent = fixture.nativeElement.querySelector('[data-testid="precedent-sejour-checkbox"]');
    const recours = fixture.nativeElement.querySelector('[data-testid="recours-forme-checkbox"]');
    expect(dateInput).not.toBeNull();
    expect(motifSelect).not.toBeNull();
    expect(precedent).not.toBeNull();
    expect(recours).not.toBeNull();
  });

  it('FRANCE workspace shows BE-only info banner instead of form', () => {
    component.workspaceCountry = 'FRANCE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="fr-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('Belgique uniquement');
  });

  it('renders the 3 result dates in JetBrains Mono (.a13q-date-mono)', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse());
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const fin = fixture.nativeElement.querySelector('[data-testid="date-fin"]');
    const leve = fixture.nativeElement.querySelector('[data-testid="date-leve-precoce"]');
    const limite = fixture.nativeElement.querySelector('[data-testid="date-limite-recours"]');
    expect(fin.classList.contains('a13q-date-mono')).toBe(true);
    expect(leve.classList.contains('a13q-date-mono')).toBe(true);
    expect(limite.classList.contains('a13q-date-mono')).toBe(true);
    expect(fin.textContent.trim()).toBe('01/05/2029');
    expect(leve.textContent.trim()).toBe('01/11/2027');
  });

  it('renders durée badge colored for 5 ans', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({ dureeInterdiction: 5 }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('[data-testid="duree-badge"]');
    expect(badge.classList.contains('duree-badge--5ans')).toBe(true);
    expect(badge.textContent.trim()).toBe('5 ans');
  });

  it('renders cross-link to F-IM-31 only when statutRecours URGENT', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    // DISPONIBLE -> no cross-link
    component.result.set(beResponse({ statutRecours: 'DISPONIBLE' }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="lien-fim31"]')).toBeNull();
    // URGENT -> cross-link F-IM-31
    component.result.set(beResponse({ statutRecours: 'URGENT', joursRestantsRecours: 3 }));
    fixture.detectChanges();
    const lien = fixture.nativeElement.querySelector('[data-testid="lien-fim31"]');
    expect(lien).not.toBeNull();
    expect(lien.textContent).toContain('F-IM-31');
  });

  it('renders conditionsLevee list', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse());
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const cond = fixture.nativeElement.querySelector('[data-testid="conditions-levee"]');
    expect(cond).not.toBeNull();
    expect(cond.querySelectorAll('li').length).toBe(2);
  });

  it('standaloneMode -> no GET, form visible, simulator banner displayed', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    const banner = fixture.nativeElement.querySelector('[data-testid="standalone-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('Mode simulateur');
  });

  it('toggleCollapse inverts collapsed state', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
  });

  it('editMode resets showForm to true', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });
});
