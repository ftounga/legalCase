import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { StagiaireGratificationSectionComponent } from './stagiaire-gratification-section.component';
import { StagiaireGratificationResponse } from '../../core/models/stagiaire-gratification.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('StagiaireGratificationSectionComponent', () => {
  let component: StagiaireGratificationSectionComponent;
  let fixture: ComponentFixture<StagiaireGratificationSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/stagiaire-gratification-analysis';

  function rappelResponse(overrides: Partial<StagiaireGratificationResponse> = {}): StagiaireGratificationResponse {
    return {
      caseFileId: 'case-1',
      dateDebutStage: '2026-01-06',
      dateFinStage: '2026-05-06',
      nombreJoursPresence: 80,
      gratificationMensuelleVersee: 0,
      tauxHoraireConventionnel: null,
      missionsHorsProjetPedagogique: false,
      posteTravailPermanent: false,
      gratificationObligatoire: true,
      seuilAtteint: true,
      gratificationMinimaleDue: 2400,
      rappelGratification: 2400,
      risqueRequalification: 'FAIBLE',
      motifs: [],
      verdictGlobal: 'RAPPEL_GRATIFICATION',
      country: 'FRANCE',
      baseJuridique: 'Art. L.124-6 Code de l\'éducation (à vérifier par avocat)',
      ...overrides,
    };
  }

  function conformeResponse(): StagiaireGratificationResponse {
    return rappelResponse({
      gratificationMensuelleVersee: 2400,
      gratificationMinimaleDue: 2400,
      rappelGratification: 0,
      verdictGlobal: 'STAGE_CONFORME',
    });
  }

  function requalificationResponse(): StagiaireGratificationResponse {
    return rappelResponse({
      missionsHorsProjetPedagogique: true,
      posteTravailPermanent: true,
      risqueRequalification: 'ELEVE',
      motifs: [
        'Occupation d\'un poste de travail permanent (art. L.124-8).',
        'Missions exécutées hors du projet pédagogique.',
      ],
      verdictGlobal: 'REQUALIFICATION_PROBABLE',
    });
  }

  function sousSeuilResponse(): StagiaireGratificationResponse {
    return rappelResponse({
      nombreJoursPresence: 20,
      gratificationObligatoire: false,
      seuilAtteint: false,
      gratificationMinimaleDue: 0,
      rappelGratification: 0,
      verdictGlobal: 'STAGE_CONFORME',
    });
  }

  function flush404(): void {
    httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [StagiaireGratificationSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(StagiaireGratificationSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  // --- statics / contract ---

  it('exposes TOOL_LABEL and TOOL_ICON statics', () => {
    expect(StagiaireGratificationSectionComponent.TOOL_LABEL).toContain('STAGIAIRE');
    expect(StagiaireGratificationSectionComponent.TOOL_ICON).toBe('school');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(StagiaireGratificationSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 2 (nominal) with both dates', () => {
    expect(StagiaireGratificationSectionComponent.getPrefillCount({
      aiData: { dateDebutStage: '2026-01-06', dateFinStage: '2026-05-06' },
      workspaceCountry: 'FRANCE',
    })).toBe(2);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(StagiaireGratificationSectionComponent.getPrefillCount({
      aiData: { dateDebutStage: '2026-01-06', dateFinStage: '2026-05-06' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  // --- gate pays / lifecycle ---

  it('FRANCE -> GET called on ngOnInit', () => {
    expect(component.isFrance()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'NF' }, { status: 404, statusText: 'NF' });
  });

  it('BELGIQUE -> no HTTP on ngOnInit', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('BELGIQUE workspace shows FR gate banner instead of form', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="country-gate-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('français uniquement');
  });

  it('loads existing analysis on GET 200', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(rappelResponse());
    expect(component.result()!.verdictGlobal).toBe('RAPPEL_GRATIFICATION');
    expect(component.showForm()).toBe(false);
    expect(component.dateDebutStage()).toBe('2026-01-06');
    expect(component.dateFinStage()).toBe('2026-05-06');
    expect(component.nombreJoursPresence()).toBe(80);
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // --- form validity ---

  it('formValid requires both dates + nombre de jours and rejects negatives', () => {
    expect(component.formValid()).toBe(false);
    component.dateDebutStage.set('2026-01-06');
    component.dateFinStage.set('2026-05-06');
    component.nombreJoursPresence.set(80);
    expect(component.formValid()).toBe(true);
    component.nombreJoursPresence.set(-1);
    expect(component.formValid()).toBe(false);
    component.nombreJoursPresence.set(80);
    component.gratificationMensuelleVersee.set(-5);
    expect(component.formValid()).toBe(false);
  });

  it('indicesRequalificationLocal counts the two irregularity checkboxes', () => {
    component.missionsHorsProjetPedagogique.set(true);
    component.posteTravailPermanent.set(false);
    expect(component.indicesRequalificationLocal()).toBe(1);
    component.posteTravailPermanent.set(true);
    expect(component.indicesRequalificationLocal()).toBe(2);
  });

  // --- coherence (F-IA-03) ---

  it('raises a coherence alert when dateFin < dateDebut', () => {
    component.dateDebutStage.set('2026-05-06');
    component.dateFinStage.set('2026-01-06');
    expect(component.coherenceAlerts().some(a => a.includes('antérieure'))).toBe(true);
  });

  it('raises a coherence alert when missions hors projet sans poste permanent', () => {
    component.missionsHorsProjetPedagogique.set(true);
    component.posteTravailPermanent.set(false);
    expect(component.coherenceAlerts().some(a => a.includes('hors projet'))).toBe(true);
  });

  // --- analyze ---

  it('analyze() POST nominal -> result + snack + refresh + exact body', () => {
    component.ngOnInit();
    flush404();
    component.dateDebutStage.set('2026-01-06');
    component.dateFinStage.set('2026-05-06');
    component.nombreJoursPresence.set(80);
    component.gratificationMensuelleVersee.set(0);
    component.tauxHoraireConventionnel.set(null);
    component.missionsHorsProjetPedagogique.set(false);
    component.posteTravailPermanent.set(false);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateDebutStage: '2026-01-06',
      dateFinStage: '2026-05-06',
      nombreJoursPresence: 80,
      gratificationMensuelleVersee: 0,
      tauxHoraireConventionnel: null,
      missionsHorsProjetPedagogique: false,
      posteTravailPermanent: false,
    });
    req.flush(rappelResponse());
    expect(component.result()!.verdictGlobal).toBe('RAPPEL_GRATIFICATION');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('analyze() error -> snack error, stays in form', () => {
    component.ngOnInit();
    flush404();
    component.dateDebutStage.set('2026-01-06');
    component.dateFinStage.set('2026-05-06');
    component.nombreJoursPresence.set(80);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    req.flush({ message: 'Boom' }, { status: 400, statusText: 'Bad Request' });
    expect(component.analyzing()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  // --- result rendering : verdict + montants + risque ---

  it('RAPPEL_GRATIFICATION -> warning verdict chip + gratification obligatoire + rappel rouge', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(rappelResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="verdict-chip"]')!;
    expect(chip.textContent).toContain('Rappel');
    expect(chip.className).toContain('is-chip--warning');
    expect(el.querySelector('[data-testid="gratification-obligatoire-chip"]')!.textContent).toContain('obligatoire');
    expect(el.querySelector('[data-testid="gratification-due-value"]')!.textContent).toContain('2400');
    const rappel = el.querySelector('[data-testid="rappel-value"]')!;
    expect(rappel.textContent).toContain('2400');
    expect(rappel.className).toContain('is-figure-value--danger');
    expect(el.querySelector('[data-testid="annual-note-result"]')!.textContent).toContain('actualiser annuellement');
  });

  it('STAGE_CONFORME -> success verdict chip + rappel non rouge (0)', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(conformeResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="verdict-chip"]')!;
    expect(chip.textContent).toContain('conforme');
    expect(chip.className).toContain('is-chip--success');
    const rappel = el.querySelector('[data-testid="rappel-value"]')!;
    expect(rappel.textContent).toContain('0');
    expect(rappel.className).not.toContain('is-figure-value--danger');
  });

  it('REQUALIFICATION_PROBABLE -> danger verdict chip + risque ELEVE + motifs listés', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(requalificationResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="verdict-chip"]')!;
    expect(chip.textContent).toContain('Requalification');
    expect(chip.className).toContain('is-chip--danger');
    const risque = el.querySelector('[data-testid="risque-chip"]')!;
    expect(risque.textContent).toContain('élevé');
    expect(risque.className).toContain('is-chip--danger');
    const motifs = el.querySelectorAll('[data-testid="motifs-list"] .is-critere');
    expect(motifs.length).toBe(2);
  });

  it('stage sous le seuil -> gratification non obligatoire', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(sousSeuilResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="gratification-obligatoire-chip"]')!.textContent).toContain('non obligatoire');
    expect(el.querySelector('[data-testid="seuil-chip"]')).toBeNull();
  });

  it('verdict / risque chip classes map their states', () => {
    expect(component.verdictChipClass('STAGE_CONFORME')).toContain('success');
    expect(component.verdictChipClass('RAPPEL_GRATIFICATION')).toContain('warning');
    expect(component.verdictChipClass('REQUALIFICATION_PROBABLE')).toContain('danger');
    expect(component.risqueChipClass('FAIBLE')).toContain('success');
    expect(component.risqueChipClass('MODERE')).toContain('warning');
    expect(component.risqueChipClass('ELEVE')).toContain('danger');
    expect(component.gratificationChipClass(true)).toContain('warning');
    expect(component.gratificationChipClass(false)).toContain('neutral');
  });

  it('annual update mention present in the form', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fixture.detectChanges();
    const note = fixture.nativeElement.querySelector('[data-testid="annual-note"]');
    expect(note).not.toBeNull();
    expect(note.textContent).toContain('actualiser annuellement');
  });

  // --- pré-fill IA ---

  it('pré-fills dateDebutStage and dateFinStage from aiData (with provenance)', () => {
    const aiData: TravailExtractedData = {
      dateDebutStage: '2026-01-06',
      dateFinStage: '2026-05-06',
    } as TravailExtractedData;
    component.aiData = aiData;
    component.ngOnInit();
    flush404();
    expect(component.dateDebutStage()).toBe('2026-01-06');
    expect(component.dateFinStage()).toBe('2026-05-06');
    expect(component.provenanceDateDebut()).toBe('IA');
    expect(component.provenanceDateFin()).toBe('IA');
  });

  it('ngOnChanges aiData triggers pré-fill while in form mode', () => {
    const aiData = { dateDebutStage: '2026-02-01' } as TravailExtractedData;
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, true) });
    expect(component.dateDebutStage()).toBe('2026-02-01');
    expect(component.provenanceDateDebut()).toBe('IA');
  });

  it('onDateDebutChange clears provenance', () => {
    component.provenanceDateDebut.set('IA');
    component.onDateDebutChange('2026-03-01');
    expect(component.dateDebutStage()).toBe('2026-03-01');
    expect(component.provenanceDateDebut()).toBeNull();
  });

  it('onDateFinChange clears provenance', () => {
    component.provenanceDateFin.set('IA');
    component.onDateFinChange('2026-06-01');
    expect(component.dateFinStage()).toBe('2026-06-01');
    expect(component.provenanceDateFin()).toBeNull();
  });
});
