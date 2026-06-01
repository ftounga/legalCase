import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { ReglementInterieurValiditeSectionComponent } from './reglement-interieur-validite-section.component';
import { ReglementInterieurValiditeResponse } from '../../core/models/reglement-interieur-validite.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('ReglementInterieurValiditeSectionComponent', () => {
  let component: ReglementInterieurValiditeSectionComponent;
  let fixture: ComponentFixture<ReglementInterieurValiditeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/reglement-interieur-validite-analysis';

  function conformeResponse(overrides: Partial<ReglementInterieurValiditeResponse> = {}): ReglementInterieurValiditeResponse {
    return {
      caseFileId: 'case-1',
      effectif: 80,
      reglementExiste: true,
      checklist: [
        { item: 'Mesures d\'hygiène et de sécurité', conforme: true, type: 'OBLIGATOIRE', commentaire: 'L.1321-1 1°' },
        { item: 'Discipline / échelle des sanctions', conforme: true, type: 'OBLIGATOIRE', commentaire: '' },
        { item: 'Droits de la défense', conforme: true, type: 'OBLIGATOIRE', commentaire: '' },
        { item: 'Rappel harcèlement / agissements sexistes', conforme: true, type: 'OBLIGATOIRE', commentaire: '' },
        { item: 'Absence de clause portant atteinte aux libertés', conforme: true, type: 'INTERDIT', commentaire: '' },
        { item: 'Absence de sanction pécuniaire', conforme: true, type: 'INTERDIT', commentaire: '' },
        { item: 'Consultation du CSE', conforme: true, type: 'PROCEDURE', commentaire: '' },
        { item: 'Transmission à l\'inspection du travail', conforme: true, type: 'PROCEDURE', commentaire: '' },
        { item: 'Dépôt au greffe du CPH', conforme: true, type: 'PROCEDURE', commentaire: '' },
      ],
      itemsObligatoiresManquants: 0,
      clausesInterditesPresentes: 0,
      statut: 'CONFORME',
      opposabilite: 'OPPOSABLE',
      consequences: ['Le règlement intérieur est opposable aux salariés.'],
      country: 'FRANCE',
      baseJuridique: 'Art. L.1311-1 à L.1322-4, L.1321-1 et s. CT (à vérifier par avocat)',
      ...overrides,
    };
  }

  function nonConformeContenuResponse(): ReglementInterieurValiditeResponse {
    return conformeResponse({
      checklist: [
        { item: 'Mesures d\'hygiène et de sécurité', conforme: true, type: 'OBLIGATOIRE', commentaire: '' },
        { item: 'Discipline / échelle des sanctions', conforme: true, type: 'OBLIGATOIRE', commentaire: '' },
        { item: 'Droits de la défense', conforme: true, type: 'OBLIGATOIRE', commentaire: '' },
        { item: 'Rappel harcèlement / agissements sexistes', conforme: false, type: 'OBLIGATOIRE', commentaire: 'Manquant (L.1321-2)' },
        { item: 'Absence de clause portant atteinte aux libertés', conforme: true, type: 'INTERDIT', commentaire: '' },
        { item: 'Absence de sanction pécuniaire', conforme: true, type: 'INTERDIT', commentaire: '' },
      ],
      itemsObligatoiresManquants: 1,
      clausesInterditesPresentes: 0,
      statut: 'NON_CONFORME',
      opposabilite: 'OPPOSABLE',
    });
  }

  function clauseInterditeResponse(): ReglementInterieurValiditeResponse {
    return conformeResponse({
      checklist: [
        { item: 'Mesures d\'hygiène et de sécurité', conforme: true, type: 'OBLIGATOIRE', commentaire: '' },
        { item: 'Absence de sanction pécuniaire', conforme: false, type: 'INTERDIT', commentaire: 'Clause stipulée (L.1331-2)' },
      ],
      itemsObligatoiresManquants: 0,
      clausesInterditesPresentes: 1,
      statut: 'NON_CONFORME',
      opposabilite: 'OPPOSABLE',
    });
  }

  function inopposableResponse(): ReglementInterieurValiditeResponse {
    return conformeResponse({
      checklist: [
        { item: 'Consultation du CSE', conforme: false, type: 'PROCEDURE', commentaire: 'Non réalisée (L.1321-4)' },
        { item: 'Transmission à l\'inspection du travail', conforme: true, type: 'PROCEDURE', commentaire: '' },
        { item: 'Dépôt au greffe du CPH', conforme: true, type: 'PROCEDURE', commentaire: '' },
      ],
      itemsObligatoiresManquants: 0,
      clausesInterditesPresentes: 0,
      statut: 'INOPPOSABLE',
      opposabilite: 'INOPPOSABLE',
    });
  }

  function nonRequisResponse(): ReglementInterieurValiditeResponse {
    return conformeResponse({
      effectif: 20,
      reglementExiste: false,
      checklist: [],
      itemsObligatoiresManquants: 0,
      clausesInterditesPresentes: 0,
      statut: 'NON_REQUIS',
      opposabilite: 'INOPPOSABLE',
      consequences: [],
    });
  }

  function flush404(): void {
    httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [ReglementInterieurValiditeSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReglementInterieurValiditeSectionComponent);
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
    expect(ReglementInterieurValiditeSectionComponent.TOOL_LABEL).toContain('RÈGLEMENT INTÉRIEUR');
    expect(ReglementInterieurValiditeSectionComponent.TOOL_ICON).toBe('rule');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(ReglementInterieurValiditeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 2 (nominal) with both fields', () => {
    expect(ReglementInterieurValiditeSectionComponent.getPrefillCount({
      aiData: { pseNombreSalaries: 80, reglementInterieurPresent: true },
      workspaceCountry: 'FRANCE',
    })).toBe(2);
  });

  it('static getPrefillCount returns 1 (partiel) with only effectif', () => {
    expect(ReglementInterieurValiditeSectionComponent.getPrefillCount({
      aiData: { pseNombreSalaries: 80 },
      workspaceCountry: 'FRANCE',
    })).toBe(1);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(ReglementInterieurValiditeSectionComponent.getPrefillCount({
      aiData: { pseNombreSalaries: 80, reglementInterieurPresent: true },
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
    httpMock.expectOne(BASE_URL).flush(conformeResponse());
    expect(component.result()!.statut).toBe('CONFORME');
    expect(component.showForm()).toBe(false);
    expect(component.effectif()).toBe(80);
    expect(component.reglementExiste()).toBe(true);
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // --- form validity ---

  it('formValid requires a positive effectif', () => {
    expect(component.formValid()).toBe(false);
    component.effectif.set(0);
    expect(component.formValid()).toBe(false);
    component.effectif.set(80);
    expect(component.formValid()).toBe(true);
  });

  // --- coherence (F-IA-03) ---

  it('raises a coherence alert when effectif >= 50 sans RI', () => {
    component.effectif.set(60);
    component.reglementExiste.set(false);
    expect(component.coherenceAlerts().some(a => a.includes('50 salariés'))).toBe(true);
  });

  it('raises a coherence alert when RI existant sans consultation CSE', () => {
    component.reglementExiste.set(true);
    component.consultationCseRealisee.set(false);
    expect(component.coherenceAlerts().some(a => a.includes('CSE n\'a pas été consulté'))).toBe(true);
  });

  it('raises a coherence alert when une clause interdite est stipulée', () => {
    component.clauseSanctionPecuniaire.set(true);
    expect(component.coherenceAlerts().some(a => a.includes('clause interdite'))).toBe(true);
  });

  // --- analyze ---

  it('analyze() POST nominal -> result + snack + refresh + exact body', () => {
    component.ngOnInit();
    flush404();
    component.effectif.set(80);
    component.reglementExiste.set(true);
    component.contenuHygieneSecurite.set(true);
    component.contenuDiscipline.set(true);
    component.contenuDroitsDefense.set(true);
    component.contenuHarcelementAgissements.set(true);
    component.clauseAtteinteLibertesNonJustifiee.set(false);
    component.clauseSanctionPecuniaire.set(false);
    component.consultationCseRealisee.set(true);
    component.transmissionInspectionTravail.set(true);
    component.depotGreffeCph.set(true);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      effectif: 80,
      reglementExiste: true,
      contenuHygieneSecurite: true,
      contenuDiscipline: true,
      contenuDroitsDefense: true,
      contenuHarcelementAgissements: true,
      clauseAtteinteLibertesNonJustifiee: false,
      clauseSanctionPecuniaire: false,
      consultationCseRealisee: true,
      transmissionInspectionTravail: true,
      depotGreffeCph: true,
    });
    req.flush(conformeResponse());
    expect(component.result()!.statut).toBe('CONFORME');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('analyze() does nothing when form invalid (no effectif)', () => {
    component.ngOnInit();
    flush404();
    component.effectif.set(null);
    component.analyze();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('analyze() error -> snack error, stays in form', () => {
    component.ngOnInit();
    flush404();
    component.effectif.set(80);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    req.flush({ message: 'Boom' }, { status: 400, statusText: 'Bad Request' });
    expect(component.analyzing()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  // --- result rendering : 4 statut states + checklist + opposabilité + compteurs ---

  it('CONFORME -> success statut chip + OPPOSABLE + checklist ✓ + compteurs 0', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(conformeResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="statut-chip"]')!;
    expect(chip.textContent).toContain('conforme');
    expect(chip.className).toContain('is-chip--success');
    const opp = el.querySelector('[data-testid="opposabilite-chip"]')!;
    expect(opp.textContent).toContain('Opposable');
    expect(opp.className).toContain('is-chip--success');
    const items = el.querySelectorAll('[data-testid="checklist"] .is-critere');
    expect(items.length).toBe(9);
    expect(el.querySelectorAll('[data-testid="badge-type"]').length).toBe(9);
    expect(el.querySelector('[data-testid="items-manquants"]')!.textContent).toContain('0');
    expect(el.querySelector('[data-testid="clauses-interdites"]')!.textContent).toContain('0');
    expect(el.querySelector('[data-testid="opposabilite-note"]')!.textContent).toContain('opposable');
  });

  it('NON_CONFORME (contenu manquant) -> warning statut chip + item ✗ + items-manquants 1', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(nonConformeContenuResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="statut-chip"]')!;
    expect(chip.textContent).toContain('non conforme');
    expect(chip.className).toContain('is-chip--warning');
    const koItems = el.querySelectorAll('[data-testid="checklist"] .is-critere--ko');
    expect(koItems.length).toBe(1);
    expect(el.querySelector('[data-testid="items-manquants"]')!.textContent).toContain('1');
  });

  it('NON_CONFORME (clause interdite) -> warning + clauses-interdites >= 1', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(clauseInterditeResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="statut-chip"]')!.className).toContain('is-chip--warning');
    expect(el.querySelector('[data-testid="clauses-interdites"]')!.textContent).toContain('1');
    const koItems = el.querySelectorAll('[data-testid="checklist"] .is-critere--ko');
    expect(koItems.length).toBe(1);
  });

  it('INOPPOSABLE -> danger statut chip + opposabilité INOPPOSABLE rouge + procédure ✗', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(inopposableResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="statut-chip"]')!;
    expect(chip.textContent).toContain('inopposable');
    expect(chip.className).toContain('is-chip--danger');
    const opp = el.querySelector('[data-testid="opposabilite-chip"]')!;
    expect(opp.textContent).toContain('Inopposable');
    expect(opp.className).toContain('is-chip--danger');
    expect(el.querySelector('[data-testid="opposabilite-note"]')!.textContent).toContain('inopposable');
  });

  it('NON_REQUIS -> neutral statut chip + checklist masquée + encart facultatif', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(nonRequisResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="statut-chip"]')!;
    expect(chip.textContent).toContain('non requis');
    expect(chip.className).toContain('is-chip--neutral');
    expect(el.querySelector('[data-testid="checklist"]')).toBeNull();
    expect(el.querySelector('[data-testid="opposabilite-chip"]')).toBeNull();
    expect(el.textContent).toContain('facultatif');
  });

  it('statut / opposabilité chip classes map their states', () => {
    expect(component.statutChipClass('CONFORME')).toContain('success');
    expect(component.statutChipClass('NON_CONFORME')).toContain('warning');
    expect(component.statutChipClass('INOPPOSABLE')).toContain('danger');
    expect(component.statutChipClass('NON_REQUIS')).toContain('neutral');
    expect(component.opposabiliteChipClass('OPPOSABLE')).toContain('success');
    expect(component.opposabiliteChipClass('INOPPOSABLE')).toContain('danger');
    expect(component.typeBadgeClass('OBLIGATOIRE')).toContain('obligatoire');
    expect(component.typeBadgeClass('INTERDIT')).toContain('interdit');
    expect(component.typeBadgeClass('PROCEDURE')).toContain('procedure');
  });

  // --- pré-fill IA ---

  it('pré-fills effectif and reglementExiste from aiData (with provenance)', () => {
    const aiData: TravailExtractedData = {
      pseNombreSalaries: 120,
      reglementInterieurPresent: true,
    };
    component.aiData = aiData;
    component.ngOnInit();
    flush404();
    expect(component.effectif()).toBe(120);
    expect(component.reglementExiste()).toBe(true);
    expect(component.provenanceEffectif()).toBe('IA');
    expect(component.provenanceReglementExiste()).toBe('IA');
  });

  it('ngOnChanges aiData triggers pré-fill while in form mode', () => {
    const aiData: TravailExtractedData = { pseNombreSalaries: 60 };
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, true) });
    expect(component.effectif()).toBe(60);
    expect(component.provenanceEffectif()).toBe('IA');
  });

  it('onEffectifChange clears provenance', () => {
    component.provenanceEffectif.set('IA');
    component.onEffectifChange(42);
    expect(component.effectif()).toBe(42);
    expect(component.provenanceEffectif()).toBeNull();
  });

  it('onReglementExisteChange clears provenance', () => {
    component.provenanceReglementExiste.set('IA');
    component.onReglementExisteChange(false);
    expect(component.reglementExiste()).toBe(false);
    expect(component.provenanceReglementExiste()).toBeNull();
  });
});
