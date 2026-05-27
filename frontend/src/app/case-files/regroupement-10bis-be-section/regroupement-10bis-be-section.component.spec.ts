import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { Regroupement10bisBeSectionComponent } from './regroupement-10bis-be-section.component';
import { Regroupement10bisBeResponse } from '../../core/models/regroupement-10bis-be.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('Regroupement10bisBeSectionComponent', () => {
  let component: Regroupement10bisBeSectionComponent;
  let fixture: ComponentFixture<Regroupement10bisBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/regroupement-10bis-be-analysis';

  // Forward date used to keep "carte A non expirée" by default in fixtures.
  const FUTURE_DATE = '2099-12-31';
  const PAST_DATE = '2000-01-01';

  function beResponse(
    overrides: Partial<Regroupement10bisBeResponse> = {},
  ): Regroupement10bisBeResponse {
    return {
      caseFileId: 'case-1',
      lienFamilial: 'CONJOINT',
      typeCarteRegroupant: 'CARTE_A',
      revenusMensuelsNetsRegroupant: 1950,
      dureeSejour: 24,
      dateFinCarteA: FUTURE_DATE,
      logementConforme: true,
      assuranceMaladie: true,
      menaceOrdrePublic: false,
      seuilRessources: 1500,
      differentielRevenus: 450,
      scoreEligibilite: 85,
      verdict: 'ELIGIBLE',
      conditionTitreEnCours: true,
      criteresNonRemplis: [],
      baseJuridique:
        'Loi 15/12/1980 art. 10bis ; AR 17/05/2007 ; AR exécution Loi 15/12/1980',
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
      imports: [Regroupement10bisBeSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(Regroupement10bisBeSectionComponent);
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
    expect(Regroupement10bisBeSectionComponent.TOOL_LABEL).toContain('REGROUPEMENT 10BIS');
    expect(Regroupement10bisBeSectionComponent.TOOL_ICON).toBe('family_restroom');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(Regroupement10bisBeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 4 when all 4 IA signals present (BELGIQUE)', () => {
    expect(Regroupement10bisBeSectionComponent.getPrefillCount({
      aiData: {
        be10bisLienFamilial: 'CONJOINT',
        be10bisRevenusMensuels: 1950,
        be10bisDureeSejour: 24,
        be10bisDateFinCarteA: FUTURE_DATE,
      },
      workspaceCountry: 'BELGIQUE',
    })).toBe(4);
  });

  it('static getPrefillCount returns 2 on partial pré-fill', () => {
    expect(Regroupement10bisBeSectionComponent.getPrefillCount({
      aiData: {
        be10bisLienFamilial: 'CONJOINT',
        be10bisDureeSejour: 12,
      },
      workspaceCountry: 'BELGIQUE',
    })).toBe(2);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=FRANCE', () => {
    expect(Regroupement10bisBeSectionComponent.getPrefillCount({
      aiData: { be10bisLienFamilial: 'CONJOINT' },
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });

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
    expect(component.lienFamilial()).toBe('CONJOINT');
    expect(component.revenusMensuelsNetsRegroupant()).toBe(1950);
    expect(component.dureeSejour()).toBe(24);
    expect(component.dateFinCarteA()).toBe(FUTURE_DATE);
    expect(component.logementConforme()).toBe(true);
    expect(component.assuranceMaladie()).toBe(true);
    expect(component.menaceOrdrePublic()).toBe(false);
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('typeCarteRegroupant is forced to CARTE_A (1 valeur possible)', () => {
    expect(component.typeCarteRegroupant).toBe('CARTE_A');
    expect(component.typeCarteLabel).toContain('Carte A');
  });

  it('formValid false if any required field is missing', () => {
    expect(component.formValid()).toBe(false);
    component.lienFamilial.set('CONJOINT');
    expect(component.formValid()).toBe(false);
    component.revenusMensuelsNetsRegroupant.set(1950);
    expect(component.formValid()).toBe(false);
    component.dureeSejour.set(24);
    expect(component.formValid()).toBe(false);
    component.dateFinCarteA.set(FUTURE_DATE);
    expect(component.formValid()).toBe(true);
  });

  it('formValid false if revenus or dureeSejour out of bounds, or date malformed', () => {
    component.lienFamilial.set('CONJOINT');
    component.revenusMensuelsNetsRegroupant.set(150_000);
    component.dureeSejour.set(24);
    component.dateFinCarteA.set(FUTURE_DATE);
    expect(component.formValid()).toBe(false);
    component.revenusMensuelsNetsRegroupant.set(1950);
    component.dureeSejour.set(1000);
    expect(component.formValid()).toBe(false);
    component.dureeSejour.set(24);
    component.dateFinCarteA.set('31/12/2027');
    expect(component.formValid()).toBe(false);
    component.dateFinCarteA.set(FUTURE_DATE);
    expect(component.formValid()).toBe(true);
  });

  it('analyze() POST nominal -> result + snack + body includes CARTE_A and dateFinCarteA', () => {
    component.ngOnInit();
    flush404();
    component.lienFamilial.set('CONJOINT');
    component.revenusMensuelsNetsRegroupant.set(1950);
    component.dureeSejour.set(24);
    component.dateFinCarteA.set(FUTURE_DATE);
    component.logementConforme.set(true);
    component.assuranceMaladie.set(true);
    component.menaceOrdrePublic.set(false);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      lienFamilial: 'CONJOINT',
      typeCarteRegroupant: 'CARTE_A',
      revenusMensuelsNetsRegroupant: 1950,
      dureeSejour: 24,
      dateFinCarteA: FUTURE_DATE,
      logementConforme: true,
      assuranceMaladie: true,
      menaceOrdrePublic: false,
    });
    req.flush(beResponse());
    expect(component.result()!.verdict).toBe('ELIGIBLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() backend 400 -> snack-error', () => {
    component.ngOnInit();
    flush404();
    component.lienFamilial.set('PARTENAIRE_ENREGISTRE');
    component.revenusMensuelsNetsRegroupant.set(1200);
    component.dureeSejour.set(6);
    component.dateFinCarteA.set(FUTURE_DATE);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
  });

  it('aiData with all 4 IA signals -> pre-fills + provenance IA', () => {
    component.aiData = {
      be10bisLienFamilial: 'PARTENAIRE_ENREGISTRE',
      be10bisRevenusMensuels: 2200,
      be10bisDureeSejour: 48,
      be10bisDateFinCarteA: FUTURE_DATE,
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.lienFamilial()).toBe('PARTENAIRE_ENREGISTRE');
    expect(component.provenanceLienFamilial()).toBe('IA');
    expect(component.revenusMensuelsNetsRegroupant()).toBe(2200);
    expect(component.provenanceRevenus()).toBe('IA');
    expect(component.dureeSejour()).toBe(48);
    expect(component.provenanceDureeSejour()).toBe('IA');
    expect(component.dateFinCarteA()).toBe(FUTURE_DATE);
    expect(component.provenanceDateFinCarte()).toBe('IA');
  });

  it('GET 200 -> no pre-fill (server values win)', () => {
    component.aiData = {
      be10bisLienFamilial: 'ASCENDANT_CHARGE',
      be10bisRevenusMensuels: 9999,
    } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(beResponse({ revenusMensuelsNetsRegroupant: 2000 }));
    expect(component.revenusMensuelsNetsRegroupant()).toBe(2000);
    expect(component.provenanceRevenus()).toBeNull();
  });

  it('onRevenusChange clears provenance', () => {
    component.aiData = {
      be10bisRevenusMensuels: 1950,
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceRevenus()).toBe('IA');
    component.onRevenusChange(2200);
    expect(component.provenanceRevenus()).toBeNull();
  });

  it('onDateFinCarteAChange clears provenance', () => {
    component.aiData = {
      be10bisDateFinCarteA: FUTURE_DATE,
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceDateFinCarte()).toBe('IA');
    component.onDateFinCarteAChange('2030-01-01');
    expect(component.provenanceDateFinCarte()).toBeNull();
  });

  it('onLienFamilialChange clears provenance', () => {
    component.aiData = {
      be10bisLienFamilial: 'CONJOINT',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceLienFamilial()).toBe('IA');
    component.onLienFamilialChange('ENFANT_MOINS_21');
    expect(component.provenanceLienFamilial()).toBeNull();
  });

  it('verdictClass covers all 3 verdict colors', () => {
    expect(component.verdictClass('ELIGIBLE')).toContain('re10bis-banner--success');
    expect(component.verdictClass('SOUS_RESERVE')).toContain('re10bis-banner--warning');
    expect(component.verdictClass('INELIGIBLE')).toContain('re10bis-banner--danger');
  });

  it('verdictChipClass covers all 3 verdicts', () => {
    expect(component.verdictChipClass('ELIGIBLE')).toContain('re10bis-chip--success');
    expect(component.verdictChipClass('SOUS_RESERVE')).toContain('re10bis-chip--warning');
    expect(component.verdictChipClass('INELIGIBLE')).toContain('re10bis-chip--danger');
  });

  it('verdictIcon covers all 3 verdicts', () => {
    expect(component.verdictIcon('ELIGIBLE')).toBe('check_circle');
    expect(component.verdictIcon('SOUS_RESERVE')).toBe('warning');
    expect(component.verdictIcon('INELIGIBLE')).toBe('block');
  });

  it('verdictLabel covers all 3 verdicts', () => {
    expect(component.verdictLabel('ELIGIBLE')).toContain('Éligible');
    expect(component.verdictLabel('SOUS_RESERVE')).toContain('réserve');
    expect(component.verdictLabel('INELIGIBLE')).toContain('Inéligible');
  });

  it('scoreBarColor maps verdicts to Material colors', () => {
    expect(component.scoreBarColor('ELIGIBLE')).toBe('primary');
    expect(component.scoreBarColor('SOUS_RESERVE')).toBe('accent');
    expect(component.scoreBarColor('INELIGIBLE')).toBe('warn');
  });

  it('differentielText is signed: positive (+) and negative', () => {
    expect(component.differentielText(450)).toContain('+450');
    expect(component.differentielText(450)).toContain('au-dessus');
    expect(component.differentielText(-200)).toContain('-200');
    expect(component.differentielText(-200)).toContain('en-dessous');
    expect(component.differentielText(0)).toContain('+0');
  });

  it('differentielBadgeClass is positive/negative coloured', () => {
    expect(component.differentielBadgeClass(450)).toContain('re10bis-diff--positive');
    expect(component.differentielBadgeClass(0)).toContain('re10bis-diff--positive');
    expect(component.differentielBadgeClass(-200)).toContain('re10bis-diff--negative');
    expect(component.differentielBadgeClass(null)).toBe('re10bis-diff');
  });

  it('lienFamilialLabel resolves the 5 codes', () => {
    expect(component.lienFamilialLabel('CONJOINT')).toBe('Conjoint');
    expect(component.lienFamilialLabel('PARTENAIRE_ENREGISTRE')).toContain('Partenaire');
    expect(component.lienFamilialLabel('ENFANT_MOINS_21')).toContain('21');
    expect(component.lienFamilialLabel('ENFANT_21_PLUS_CHARGE')).toContain('majeur');
    expect(component.lienFamilialLabel('ASCENDANT_CHARGE')).toContain('Ascendant');
    expect(component.lienFamilialLabel(null)).toBe('');
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

  it('ngOnChanges with new aiData in form mode -> re-prefill', () => {
    component.ngOnInit();
    flush404();
    expect(component.lienFamilial()).toBeNull();
    component.aiData = {
      be10bisLienFamilial: 'ENFANT_MOINS_21',
      be10bisRevenusMensuels: 2400,
      be10bisDateFinCarteA: FUTURE_DATE,
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.lienFamilial()).toBe('ENFANT_MOINS_21');
    expect(component.provenanceLienFamilial()).toBe('IA');
    expect(component.revenusMensuelsNetsRegroupant()).toBe(2400);
    expect(component.provenanceRevenus()).toBe('IA');
    expect(component.dateFinCarteA()).toBe(FUTURE_DATE);
    expect(component.provenanceDateFinCarte()).toBe('IA');
  });

  it('ngOnChanges does NOT re-prefill when result already loaded', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(beResponse({ revenusMensuelsNetsRegroupant: 2000 }));
    component.aiData = {
      be10bisRevenusMensuels: 9999,
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.revenusMensuelsNetsRegroupant()).toBe(2000);
    expect(component.provenanceRevenus()).toBeNull();
  });

  it('FRANCE workspace shows info banner instead of form', () => {
    component.workspaceCountry = 'FRANCE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.re10bis-banner--info');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('Belgique uniquement');
  });

  it('standaloneMode -> no GET, form visible, banner displayed', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    const banner = fixture.nativeElement.querySelector('[data-testid="standalone-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('Mode simulateur');
  });

  it('renders verdict chip in result header (INELIGIBLE)', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({ verdict: 'INELIGIBLE', scoreEligibilite: 25 }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.re10bis-chip');
    expect(chip).not.toBeNull();
    expect(chip.classList.contains('re10bis-chip--danger')).toBe(true);
  });

  it('renders criteresNonRemplis list when result is SOUS_RESERVE', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({
      verdict: 'SOUS_RESERVE',
      scoreEligibilite: 60,
      differentielRevenus: -150,
      criteresNonRemplis: ['Revenus insuffisants', 'Logement non documenté'],
    }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const items = fixture.nativeElement.querySelectorAll('.re10bis-criteres-list mat-list-item');
    expect(items.length).toBe(2);
  });

  it('carteAExpiree computed -> false when no date saisie', () => {
    expect(component.carteAExpiree()).toBe(false);
  });

  it('carteAExpiree computed -> true when dateFinCarteA in past', () => {
    component.dateFinCarteA.set(PAST_DATE);
    expect(component.carteAExpiree()).toBe(true);
  });

  it('carteAExpiree computed -> false when dateFinCarteA in future', () => {
    component.dateFinCarteA.set(FUTURE_DATE);
    expect(component.carteAExpiree()).toBe(false);
  });

  it('renders red expired-card banner when dateFinCarteA in past', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.collapsed.set(false);
    component.dateFinCarteA.set(PAST_DATE);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="carte-a-expiree-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('Carte A expirée');
    expect(banner.classList.contains('re10bis-banner--danger')).toBe(true);
  });

  it('does NOT render expired-card banner when dateFinCarteA in future', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.collapsed.set(false);
    component.dateFinCarteA.set(FUTURE_DATE);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="carte-a-expiree-banner"]');
    expect(banner).toBeNull();
  });

  it('renders condition-titre-banner in result when conditionTitreEnCours=false', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({
      verdict: 'INELIGIBLE',
      conditionTitreEnCours: false,
      dateFinCarteA: PAST_DATE,
    }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="condition-titre-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('Carte A expirée');
  });

  it('does NOT render condition-titre-banner in result when conditionTitreEnCours=true', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({ conditionTitreEnCours: true }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="condition-titre-banner"]');
    expect(banner).toBeNull();
  });

  it('renders the static CARTE_A label (typeCarte non-saisissable)', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.collapsed.set(false);
    fixture.detectChanges();
    const staticValue = fixture.nativeElement.querySelector('[data-testid="type-carte-static"]');
    expect(staticValue).not.toBeNull();
    expect(staticValue.textContent).toContain('Carte A');
  });
});
