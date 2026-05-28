import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { AesmMenaBeSectionComponent } from './aesm-mena-be-section.component';
import { AesmMenaBeResponse } from '../../core/models/aesm-mena-be.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('AesmMenaBeSectionComponent', () => {
  let component: AesmMenaBeSectionComponent;
  let fixture: ComponentFixture<AesmMenaBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/aesm-mena-be-analysis';

  function beResponse(
    overrides: Partial<AesmMenaBeResponse> = {},
  ): AesmMenaBeResponse {
    return {
      caseFileId: 'case-1',
      ageActuel: 16,
      dateArriveeBelgique: '2023-09-01',
      tuteurDesigne: true,
      integrationScolaire: true,
      dureeScolaire: 24,
      projetVieElabore: true,
      perspectiveAutonomie: true,
      menaceOrdrePublic: false,
      etapeTutelle: null,
      delaiDesignationTuteur: '2023-09-02',
      scoreIntegration: 80,
      bonus: 5,
      verdictAESM: 'FAVORABLE',
      prioriteUrgence: false,
      criteresNonRemplis: [],
      prochainActe: 'Désignation tuteur DGDE → Entretien OE → Décision AESM (3-12 mois)',
      baseJuridique: 'Loi 04/05/2007 tutelle MENA ; Art. 9bis Loi 15/12/1980 ; Circulaire OE 15/09/2005',
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
      imports: [AesmMenaBeSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(AesmMenaBeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match((r) => r.url.includes('/jurisprudence-citations')).forEach((r) => r.flush({ items: [] }));
    httpMock.verify();
  });

  // ===================== Statics =====================

  it('exposes TOOL_LABEL and TOOL_ICON statics', () => {
    expect(AesmMenaBeSectionComponent.TOOL_LABEL).toContain('AESM');
    expect(AesmMenaBeSectionComponent.TOOL_LABEL).toContain('MENA');
    expect(AesmMenaBeSectionComponent.TOOL_ICON).toBe('child_care');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(AesmMenaBeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 3 when all 3 REAL IA signals present (BELGIQUE) — MAX', () => {
    expect(AesmMenaBeSectionComponent.getPrefillCount({
      aiData: {
        menaAge: 16,
        menaDateArrivee: '2023-09-01',
        menaDureeScolaire: 24,
      },
      workspaceCountry: 'BELGIQUE',
    })).toBe(3);
  });

  it('static getPrefillCount returns 2 on partial pré-fill (no dureeScolaire)', () => {
    expect(AesmMenaBeSectionComponent.getPrefillCount({
      aiData: { menaAge: 15, menaDateArrivee: '2024-01-15' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(2);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=FRANCE', () => {
    expect(AesmMenaBeSectionComponent.getPrefillCount({
      aiData: { menaAge: 16 },
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });

  // ===================== HTTP / lifecycle =====================

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
    expect(component.result()!.verdictAESM).toBe('FAVORABLE');
    expect(component.showForm()).toBe(false);
    expect(component.ageActuel()).toBe(16);
    expect(component.dateArriveeBelgique()).toBe('2023-09-01');
    expect(component.tuteurDesigne()).toBe(true);
    expect(component.integrationScolaire()).toBe(true);
    expect(component.dureeScolaire()).toBe(24);
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ===================== Form validation =====================

  it('formValid false if age missing', () => {
    expect(component.formValid()).toBe(false);
    component.dateArriveeBelgique.set('2023-09-01');
    expect(component.formValid()).toBe(false);
    component.ageActuel.set(15);
    expect(component.formValid()).toBe(true);
  });

  it('formValid false if age out of bounds', () => {
    component.dateArriveeBelgique.set('2023-09-01');
    component.ageActuel.set(-1);
    expect(component.formValid()).toBe(false);
    component.ageActuel.set(18);
    expect(component.formValid()).toBe(false);
    component.ageActuel.set(15);
    expect(component.formValid()).toBe(true);
  });

  it('formValid requires dureeScolaire when integrationScolaire=true', () => {
    component.ageActuel.set(15);
    component.dateArriveeBelgique.set('2023-09-01');
    component.integrationScolaire.set(true);
    component.dureeScolaire.set(null);
    expect(component.formValid()).toBe(false);
    component.dureeScolaire.set(24);
    expect(component.formValid()).toBe(true);
  });

  it('formValid OK without dureeScolaire when integrationScolaire=false', () => {
    component.ageActuel.set(15);
    component.dateArriveeBelgique.set('2023-09-01');
    component.integrationScolaire.set(false);
    component.dureeScolaire.set(null);
    expect(component.formValid()).toBe(true);
  });

  it('onIntegrationScolaireChange(false) resets dureeScolaire', () => {
    component.integrationScolaire.set(true);
    component.dureeScolaire.set(36);
    component.provenanceDureeScolaire.set('IA');
    component.onIntegrationScolaireChange(false);
    expect(component.integrationScolaire()).toBe(false);
    expect(component.dureeScolaire()).toBeNull();
    expect(component.provenanceDureeScolaire()).toBeNull();
  });

  // ===================== Analyze POST =====================

  it('analyze() POST nominal -> result + snack + body has 8 fields', () => {
    component.ngOnInit();
    flush404();
    component.ageActuel.set(16);
    component.dateArriveeBelgique.set('2023-09-01');
    component.tuteurDesigne.set(true);
    component.integrationScolaire.set(true);
    component.dureeScolaire.set(24);
    component.projetVieElabore.set(true);
    component.perspectiveAutonomie.set(true);
    component.menaceOrdrePublic.set(false);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      ageActuel: 16,
      dateArriveeBelgique: '2023-09-01',
      tuteurDesigne: true,
      integrationScolaire: true,
      dureeScolaire: 24,
      projetVieElabore: true,
      perspectiveAutonomie: true,
      menaceOrdrePublic: false,
    });
    req.flush(beResponse());
    expect(component.result()!.verdictAESM).toBe('FAVORABLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() sends dureeScolaire=null when integrationScolaire=false', () => {
    component.ngOnInit();
    flush404();
    component.ageActuel.set(15);
    component.dateArriveeBelgique.set('2024-01-01');
    component.integrationScolaire.set(false);
    // Even if a stale value was set, the request must contain null when checkbox off.
    component.dureeScolaire.set(99);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.dureeScolaire).toBeNull();
    expect(req.request.body.integrationScolaire).toBe(false);
    req.flush(beResponse({ integrationScolaire: false, dureeScolaire: null }));
  });

  it('analyze() backend 400 -> snack-error', () => {
    component.ngOnInit();
    flush404();
    component.ageActuel.set(15);
    component.dateArriveeBelgique.set('2023-09-01');
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
  });

  // ===================== Pré-fill IA =====================

  it('aiData with 3 REAL IA signals -> pre-fills + provenance IA (5 checkboxes NOT pre-filled)', () => {
    component.aiData = {
      menaAge: 16,
      menaDateArrivee: '2023-09-01',
      menaDureeScolaire: 30,
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.ageActuel()).toBe(16);
    expect(component.provenanceAge()).toBe('IA');
    expect(component.dateArriveeBelgique()).toBe('2023-09-01');
    expect(component.provenanceDateArrivee()).toBe('IA');
    expect(component.dureeScolaire()).toBe(30);
    expect(component.provenanceDureeScolaire()).toBe('IA');
    // PREFILL_COUNT_ALWAYS_ZERO — 5 checkboxes restent false par défaut sans provenance signal.
    expect(component.tuteurDesigne()).toBe(false);
    expect(component.integrationScolaire()).toBe(false);
    expect(component.projetVieElabore()).toBe(false);
    expect(component.perspectiveAutonomie()).toBe(false);
    expect(component.menaceOrdrePublic()).toBe(false);
  });

  it('GET 200 -> no pre-fill (server values win)', () => {
    component.aiData = {
      menaAge: 14,
      menaDateArrivee: '2020-01-01',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(beResponse({ ageActuel: 17, dateArriveeBelgique: '2024-06-15' }));
    expect(component.ageActuel()).toBe(17);
    expect(component.dateArriveeBelgique()).toBe('2024-06-15');
    expect(component.provenanceAge()).toBeNull();
    expect(component.provenanceDateArrivee()).toBeNull();
  });

  it('onAgeActuelChange clears provenance', () => {
    component.aiData = { menaAge: 15 } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceAge()).toBe('IA');
    component.onAgeActuelChange(16);
    expect(component.provenanceAge()).toBeNull();
  });

  it('onDateArriveeBelgiqueChange clears provenance', () => {
    component.aiData = { menaDateArrivee: '2023-09-01' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceDateArrivee()).toBe('IA');
    component.onDateArriveeBelgiqueChange('2024-01-01');
    expect(component.provenanceDateArrivee()).toBeNull();
  });

  it('onDureeScolaireChange clears provenance', () => {
    component.aiData = { menaDureeScolaire: 24 } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceDureeScolaire()).toBe('IA');
    component.onDureeScolaireChange(36);
    expect(component.provenanceDureeScolaire()).toBeNull();
  });

  // ===================== Helpers verdict =====================

  it('verdictBannerClass covers 3 verdicts', () => {
    expect(component.verdictBannerClass('FAVORABLE')).toContain('aesm-mena-banner--success');
    expect(component.verdictBannerClass('SOUS_RESERVE')).toContain('aesm-mena-banner--warn');
    expect(component.verdictBannerClass('DEFAVORABLE')).toContain('aesm-mena-banner--danger');
    expect(component.verdictBannerClass(null)).toBe('aesm-mena-banner');
  });

  it('verdictChipClass covers 3 verdicts', () => {
    expect(component.verdictChipClass('FAVORABLE')).toContain('aesm-mena-chip--success');
    expect(component.verdictChipClass('SOUS_RESERVE')).toContain('aesm-mena-chip--warn');
    expect(component.verdictChipClass('DEFAVORABLE')).toContain('aesm-mena-chip--danger');
  });

  it('verdictIcon covers 3 verdicts', () => {
    expect(component.verdictIcon('FAVORABLE')).toBe('check_circle');
    expect(component.verdictIcon('SOUS_RESERVE')).toBe('warning');
    expect(component.verdictIcon('DEFAVORABLE')).toBe('block');
  });

  it('verdictLabel covers 3 verdicts', () => {
    expect(component.verdictLabel('FAVORABLE')).toBe('FAVORABLE');
    expect(component.verdictLabel('SOUS_RESERVE')).toBe('SOUS RÉSERVE');
    expect(component.verdictLabel('DEFAVORABLE')).toBe('DÉFAVORABLE');
  });

  it('progressColor maps verdict to mat-progress-bar color', () => {
    expect(component.progressColor('FAVORABLE')).toBe('primary');
    expect(component.progressColor('SOUS_RESERVE')).toBe('accent');
    expect(component.progressColor('DEFAVORABLE')).toBe('warn');
  });

  it('formatDateFr converts ISO yyyy-MM-dd to DD/MM/YYYY', () => {
    expect(component.formatDateFr('2024-12-31')).toBe('31/12/2024');
    expect(component.formatDateFr(null)).toBe('');
    expect(component.formatDateFr('invalid')).toBe('invalid');
  });

  // ===================== Lifecycle helpers =====================

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
    expect(component.ageActuel()).toBeNull();
    component.aiData = {
      menaAge: 15,
      menaDateArrivee: '2024-03-15',
      menaDureeScolaire: 18,
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.ageActuel()).toBe(15);
    expect(component.provenanceAge()).toBe('IA');
  });

  it('ngOnChanges does NOT re-prefill when result already loaded', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(beResponse({ ageActuel: 17 }));
    component.aiData = {
      menaAge: 14,
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.ageActuel()).toBe(17);
    expect(component.provenanceAge()).toBeNull();
  });

  // ===================== Rendering =====================

  it('FRANCE workspace shows info banner instead of form (gate BE-only)', () => {
    component.workspaceCountry = 'FRANCE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.aesm-mena-banner--info');
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

  it('renders verdict chip in result header (FAVORABLE = green)', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({ verdictAESM: 'FAVORABLE' }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.aesm-mena-chip');
    expect(chip).not.toBeNull();
    expect(chip.classList.contains('aesm-mena-chip--success')).toBe(true);
    expect(chip.textContent).toContain('FAVORABLE');
  });

  it('renders verdict chip (SOUS_RESERVE = orange)', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({ verdictAESM: 'SOUS_RESERVE' }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.aesm-mena-chip');
    expect(chip.classList.contains('aesm-mena-chip--warn')).toBe(true);
    expect(chip.textContent).toContain('SOUS R');
  });

  it('renders verdict chip (DEFAVORABLE = red)', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({ verdictAESM: 'DEFAVORABLE' }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.aesm-mena-chip');
    expect(chip.classList.contains('aesm-mena-chip--danger')).toBe(true);
  });

  it('renders urgence banner when prioriteUrgence=true (age >= 17)', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({ ageActuel: 17, prioriteUrgence: true }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const urgence = fixture.nativeElement.querySelector('[data-testid="urgence-banner"]');
    expect(urgence).not.toBeNull();
    expect(urgence.textContent).toContain('Approche de la majorité');
  });

  it('does NOT render urgence banner when prioriteUrgence=false', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({ ageActuel: 15, prioriteUrgence: false }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const urgence = fixture.nativeElement.querySelector('[data-testid="urgence-banner"]');
    expect(urgence).toBeNull();
  });

  it('renders etapeTutelle block (orange) when tuteurDesigne=false', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({
      tuteurDesigne: false,
      etapeTutelle: 'Saisir le Service des Tutelles DGDE par formulaire en ligne',
      delaiDesignationTuteur: '2024-01-10',
    }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const block = fixture.nativeElement.querySelector('[data-testid="etape-tutelle-block"]');
    expect(block).not.toBeNull();
    expect(block.textContent).toContain('Saisir le Service des Tutelles');
    const delai = fixture.nativeElement.querySelector('[data-testid="delai-tutelle-value"]');
    expect(delai.textContent.trim()).toBe('10/01/2024');
  });

  it('does NOT render etapeTutelle block when tuteur already désigné', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({ tuteurDesigne: true, etapeTutelle: null }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const block = fixture.nativeElement.querySelector('[data-testid="etape-tutelle-block"]');
    expect(block).toBeNull();
  });

  it('renders score gauge 0-100 + score value', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({ scoreIntegration: 75 }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const block = fixture.nativeElement.querySelector('[data-testid="score-block"]');
    expect(block).not.toBeNull();
    expect(block.textContent).toContain('75 / 100');
    const bar = fixture.nativeElement.querySelector('mat-progress-bar');
    expect(bar).not.toBeNull();
  });

  it('renders bonus block when bonus > 0 (scolarité longue durée)', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({ bonus: 5 }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const bonus = fixture.nativeElement.querySelector('[data-testid="bonus-block"]');
    expect(bonus).not.toBeNull();
    expect(bonus.textContent).toContain('+5');
    expect(bonus.textContent).toContain('scolarit');
  });

  it('does NOT render bonus block when bonus=0', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({ bonus: 0 }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const bonus = fixture.nativeElement.querySelector('[data-testid="bonus-block"]');
    expect(bonus).toBeNull();
  });

  it('renders criteresNonRemplis list when result has missing criteria', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({
      verdictAESM: 'DEFAVORABLE',
      criteresNonRemplis: [
        'Pas de tuteur DGDE désigné',
        'Pas de scolarité effective',
        'Menace ordre public détectée',
      ],
    }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const items = fixture.nativeElement.querySelectorAll('.aesm-mena-criteres-list mat-list-item');
    expect(items.length).toBe(3);
  });

  it('renders prochainActe in result', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse());
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const proc = fixture.nativeElement.querySelector('[data-testid="prochain-acte-block"]');
    expect(proc).not.toBeNull();
    expect(proc.textContent).toContain('Désignation tuteur DGDE');
  });

  it('dureeScolaire field is conditionally shown only when integrationScolaire=true', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({ message: 'NF' }, { status: 404, statusText: 'NF' });
    fixture.detectChanges();
    // By default integrationScolaire=false → dureeScolaire wrap absent.
    let wrap = fixture.nativeElement.querySelector('[data-testid="duree-scolaire-wrap"]');
    expect(wrap).toBeNull();
    // Toggle on → appears.
    component.integrationScolaire.set(true);
    fixture.detectChanges();
    wrap = fixture.nativeElement.querySelector('[data-testid="duree-scolaire-wrap"]');
    expect(wrap).not.toBeNull();
  });

  it('renders form fields without binding errors (regression #1385/#1386 — no orphan [coherenceAlert])', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({ message: 'NF' }, { status: 404, statusText: 'NF' });
    fixture.detectChanges();
    // Form parts must render — 2 fieldsets, age/date inputs, 5 aspirational checkboxes.
    expect(fixture.nativeElement.querySelector('[data-testid="fieldset-tutelle"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="fieldset-aesm"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="tuteur-designe-checkbox"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="date-arrivee-input"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="age-actuel-input"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="integration-scolaire-checkbox"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="projet-vie-checkbox"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="perspective-autonomie-checkbox"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="menace-ordre-public-checkbox"]')).not.toBeNull();
  });
});
