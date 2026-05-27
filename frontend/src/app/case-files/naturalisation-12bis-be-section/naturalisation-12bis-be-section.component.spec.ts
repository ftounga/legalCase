import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { Naturalisation12bisBeSectionComponent } from './naturalisation-12bis-be-section.component';
import { Naturalisation12bisBeResponse } from '../../core/models/naturalisation-12bis-be.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('Naturalisation12bisBeSectionComponent', () => {
  let component: Naturalisation12bisBeSectionComponent;
  let fixture: ComponentFixture<Naturalisation12bisBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/naturalisation-12bis-be-analysis';

  function beResponse(
    overrides: Partial<Naturalisation12bisBeResponse> = {},
  ): Naturalisation12bisBeResponse {
    return {
      caseFileId: 'case-1',
      dureeSejour: 72,
      typeSejour: 'ILLIMITE',
      niveauLangue: 'A2',
      preuveIntegration: true,
      preuveEmploi: true,
      menaceOrdrePublic: false,
      condamnationPenale: false,
      voie5Ans: true,
      voie10Ans: true,
      voieEligible: 'VOIE_5_ANS',
      dureeManquante: 0,
      criteresNonRemplis: [],
      procedureReference:
        'Chambre des représentants — Commission des naturalisations',
      baseJuridique:
        'Code de la nationalité belge art. 12bis ; Loi 28/06/1984 mod. Loi 04/12/2012',
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
      imports: [Naturalisation12bisBeSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(Naturalisation12bisBeSectionComponent);
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
    expect(Naturalisation12bisBeSectionComponent.TOOL_LABEL).toContain('NATURALISATION 12BIS');
    expect(Naturalisation12bisBeSectionComponent.TOOL_ICON).toBe('flag');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(Naturalisation12bisBeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 3 when all 3 REAL IA signals present (BELGIQUE) — MAX', () => {
    expect(Naturalisation12bisBeSectionComponent.getPrefillCount({
      aiData: {
        naturalisationBeDureeSejour: 72,
        naturalisationBeTypeSejour: 'ILLIMITE',
        naturalisationBeNiveauLangue: 'A2',
      },
      workspaceCountry: 'BELGIQUE',
    })).toBe(3);
  });

  it('static getPrefillCount returns 1 on partial pré-fill (only dureeSejour)', () => {
    expect(Naturalisation12bisBeSectionComponent.getPrefillCount({
      aiData: { naturalisationBeDureeSejour: 36 },
      workspaceCountry: 'BELGIQUE',
    })).toBe(1);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=FRANCE', () => {
    expect(Naturalisation12bisBeSectionComponent.getPrefillCount({
      aiData: { naturalisationBeDureeSejour: 72 },
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
    expect(component.result()!.voieEligible).toBe('VOIE_5_ANS');
    expect(component.showForm()).toBe(false);
    expect(component.dureeSejour()).toBe(72);
    expect(component.typeSejour()).toBe('ILLIMITE');
    expect(component.niveauLangue()).toBe('A2');
    expect(component.preuveIntegration()).toBe(true);
    expect(component.preuveEmploi()).toBe(true);
    expect(component.menaceOrdrePublic()).toBe(false);
    expect(component.condamnationPenale()).toBe(false);
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid false if any required field is missing', () => {
    expect(component.formValid()).toBe(false);
    component.dureeSejour.set(72);
    expect(component.formValid()).toBe(false);
    component.typeSejour.set('ILLIMITE');
    expect(component.formValid()).toBe(false);
    component.niveauLangue.set('A2');
    expect(component.formValid()).toBe(true);
  });

  it('formValid false if dureeSejour out of bounds', () => {
    component.typeSejour.set('ILLIMITE');
    component.niveauLangue.set('A2');
    component.dureeSejour.set(-1);
    expect(component.formValid()).toBe(false);
    component.dureeSejour.set(601);
    expect(component.formValid()).toBe(false);
    component.dureeSejour.set(60);
    expect(component.formValid()).toBe(true);
  });

  it('analyze() POST nominal -> result + snack + body has 7 fields', () => {
    component.ngOnInit();
    flush404();
    component.dureeSejour.set(72);
    component.typeSejour.set('ILLIMITE');
    component.niveauLangue.set('A2');
    component.preuveIntegration.set(true);
    component.preuveEmploi.set(true);
    component.menaceOrdrePublic.set(false);
    component.condamnationPenale.set(false);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dureeSejour: 72,
      typeSejour: 'ILLIMITE',
      niveauLangue: 'A2',
      preuveIntegration: true,
      preuveEmploi: true,
      menaceOrdrePublic: false,
      condamnationPenale: false,
    });
    req.flush(beResponse());
    expect(component.result()!.voieEligible).toBe('VOIE_5_ANS');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() backend 400 -> snack-error', () => {
    component.ngOnInit();
    flush404();
    component.dureeSejour.set(24);
    component.typeSejour.set('LIMITE');
    component.niveauLangue.set('INFERIEUR_A2');
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
  });

  it('aiData with 3 REAL IA signals -> pre-fills + provenance IA (4 checkboxes NOT pre-filled)', () => {
    component.aiData = {
      naturalisationBeDureeSejour: 84,
      naturalisationBeTypeSejour: 'ILLIMITE',
      naturalisationBeNiveauLangue: 'SUPERIEUR_A2',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.dureeSejour()).toBe(84);
    expect(component.provenanceDureeSejour()).toBe('IA');
    expect(component.typeSejour()).toBe('ILLIMITE');
    expect(component.provenanceTypeSejour()).toBe('IA');
    expect(component.niveauLangue()).toBe('SUPERIEUR_A2');
    expect(component.provenanceNiveauLangue()).toBe('IA');
    // PREFILL_COUNT_ALWAYS_ZERO — 4 checkboxes restent false par défaut sans provenance signal.
    expect(component.preuveIntegration()).toBe(false);
    expect(component.preuveEmploi()).toBe(false);
    expect(component.menaceOrdrePublic()).toBe(false);
    expect(component.condamnationPenale()).toBe(false);
  });

  it('GET 200 -> no pre-fill (server values win)', () => {
    component.aiData = {
      naturalisationBeDureeSejour: 36,
      naturalisationBeTypeSejour: 'LIMITE',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(beResponse({ dureeSejour: 120, typeSejour: 'ILLIMITE' }));
    expect(component.dureeSejour()).toBe(120);
    expect(component.typeSejour()).toBe('ILLIMITE');
    expect(component.provenanceDureeSejour()).toBeNull();
    expect(component.provenanceTypeSejour()).toBeNull();
  });

  it('onDureeSejourChange clears provenance', () => {
    component.aiData = {
      naturalisationBeDureeSejour: 60,
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceDureeSejour()).toBe('IA');
    component.onDureeSejourChange(84);
    expect(component.provenanceDureeSejour()).toBeNull();
  });

  it('onTypeSejourChange clears provenance', () => {
    component.aiData = {
      naturalisationBeTypeSejour: 'ILLIMITE',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceTypeSejour()).toBe('IA');
    component.onTypeSejourChange('LIMITE');
    expect(component.provenanceTypeSejour()).toBeNull();
  });

  it('onNiveauLangueChange clears provenance', () => {
    component.aiData = {
      naturalisationBeNiveauLangue: 'A2',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceNiveauLangue()).toBe('IA');
    component.onNiveauLangueChange('SUPERIEUR_A2');
    expect(component.provenanceNiveauLangue()).toBeNull();
  });

  it('voieBannerClass covers VOIE_5_ANS (green), VOIE_10_ANS (green) and AUCUNE (red)', () => {
    expect(component.voieBannerClass('VOIE_5_ANS')).toContain('nat12bis-banner--success');
    expect(component.voieBannerClass('VOIE_10_ANS')).toContain('nat12bis-banner--success');
    expect(component.voieBannerClass('AUCUNE')).toContain('nat12bis-banner--danger');
  });

  it('voieChipClass covers 3 verdicts', () => {
    expect(component.voieChipClass('VOIE_5_ANS')).toContain('nat12bis-chip--success');
    expect(component.voieChipClass('VOIE_10_ANS')).toContain('nat12bis-chip--success');
    expect(component.voieChipClass('AUCUNE')).toContain('nat12bis-chip--danger');
  });

  it('voieIcon covers 3 verdicts', () => {
    expect(component.voieIcon('VOIE_5_ANS')).toBe('check_circle');
    expect(component.voieIcon('VOIE_10_ANS')).toBe('check_circle');
    expect(component.voieIcon('AUCUNE')).toBe('block');
  });

  it('voieLabel covers 3 verdicts', () => {
    expect(component.voieLabel('VOIE_5_ANS')).toBe('Voie 5 ans');
    expect(component.voieLabel('VOIE_10_ANS')).toBe('Voie 10 ans');
    expect(component.voieLabel('AUCUNE')).toContain('Aucune');
  });

  it('typeSejourLabel resolves both codes', () => {
    expect(component.typeSejourLabel('LIMITE')).toContain('limité');
    expect(component.typeSejourLabel('ILLIMITE')).toContain('illimité');
    expect(component.typeSejourLabel(null)).toBe('');
  });

  it('niveauLangueLabel resolves the 3 codes', () => {
    expect(component.niveauLangueLabel('INFERIEUR_A2')).toContain('Inférieur');
    expect(component.niveauLangueLabel('A2')).toContain('A2');
    expect(component.niveauLangueLabel('SUPERIEUR_A2')).toContain('Supérieur');
    expect(component.niveauLangueLabel(null)).toBe('');
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
    expect(component.dureeSejour()).toBeNull();
    component.aiData = {
      naturalisationBeDureeSejour: 96,
      naturalisationBeTypeSejour: 'ILLIMITE',
      naturalisationBeNiveauLangue: 'A2',
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.dureeSejour()).toBe(96);
    expect(component.provenanceDureeSejour()).toBe('IA');
    expect(component.typeSejour()).toBe('ILLIMITE');
    expect(component.provenanceTypeSejour()).toBe('IA');
  });

  it('ngOnChanges does NOT re-prefill when result already loaded', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(beResponse({ dureeSejour: 120 }));
    component.aiData = {
      naturalisationBeDureeSejour: 9999,
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.dureeSejour()).toBe(120);
    expect(component.provenanceDureeSejour()).toBeNull();
  });

  it('FRANCE workspace shows info banner instead of form', () => {
    component.workspaceCountry = 'FRANCE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.nat12bis-banner--info');
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

  it('renders verdict chip in result header (AUCUNE = red)', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({
      voie5Ans: false, voie10Ans: false, voieEligible: 'AUCUNE', dureeManquante: 24,
    }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.nat12bis-chip');
    expect(chip).not.toBeNull();
    expect(chip.classList.contains('nat12bis-chip--danger')).toBe(true);
  });

  it('renders dureeManquante block when > 0', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({
      voie5Ans: false, voie10Ans: false, voieEligible: 'AUCUNE', dureeManquante: 18,
    }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const block = fixture.nativeElement.querySelector('[data-testid="duree-manquante-block"]');
    expect(block).not.toBeNull();
    expect(block.textContent).toContain('18');
    expect(block.textContent).toContain('mois à attendre');
  });

  it('does NOT render dureeManquante block when = 0', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({ dureeManquante: 0 }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const block = fixture.nativeElement.querySelector('[data-testid="duree-manquante-block"]');
    expect(block).toBeNull();
  });

  it('renders info bloc when voie5Ans=true (cours régional)', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({ voie5Ans: true }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const bloc = fixture.nativeElement.querySelector('[data-testid="info-voie-5-ans"]');
    expect(bloc).not.toBeNull();
    expect(bloc.textContent).toContain('Parcours');
  });

  it('renders criteresNonRemplis list when result has missing criteria', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({
      voie5Ans: false, voie10Ans: false, voieEligible: 'AUCUNE',
      criteresNonRemplis: ['Durée séjour insuffisante', 'Langue < A2', 'Pas de preuve intégration'],
    }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const items = fixture.nativeElement.querySelectorAll('.nat12bis-criteres-list mat-list-item');
    expect(items.length).toBe(3);
  });

  it('renders procedureReference (Chambre des représentants) in result', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse());
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const proc = fixture.nativeElement.querySelector('.nat12bis-procedure');
    expect(proc).not.toBeNull();
    expect(proc.textContent).toContain('Chambre des représentants');
  });

  it('F-IA-03 coherenceAlerts has DUREE_SEJOUR alert when user value diverges from AI', () => {
    component.aiData = {
      naturalisationBeDureeSejour: 60,
    } as ImmigrationExtractedData;
    component.dureeSejour.set(120);
    expect(component.coherenceAlerts().DUREE_SEJOUR).toBeTruthy();
    const alert = component.coherenceAlerts().DUREE_SEJOUR!;
    expect(component.alertBadgeLabel(alert)).toContain('60 mois');
  });

  it('F-IA-03 no DUREE_SEJOUR alert when user value matches AI', () => {
    component.aiData = {
      naturalisationBeDureeSejour: 60,
    } as ImmigrationExtractedData;
    component.dureeSejour.set(60);
    // Builder returns null when no source registered.
    expect(component.coherenceAlerts().DUREE_SEJOUR).toBeUndefined();
  });

  it('alertTooltip returns the alert reason', () => {
    component.aiData = {
      naturalisationBeDureeSejour: 60,
    } as ImmigrationExtractedData;
    component.dureeSejour.set(120);
    const alert = component.coherenceAlerts().DUREE_SEJOUR!;
    expect(component.alertTooltip(alert)).toContain('60');
  });
});
