import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  TravailNoirBeDimonaSectionComponent,
} from './travail-noir-be-dimona-section.component';
import {
  TravailNoirBeDimonaResponse,
} from '../../core/models/travail-noir-be-dimona.model';

describe('TravailNoirBeDimonaSectionComponent', () => {
  let component: TravailNoirBeDimonaSectionComponent;
  let fixture: ComponentFixture<TravailNoirBeDimonaSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/travail-noir-be-dimona';

  /**
   * Réponse backend par défaut — verdict ABSENCE_DIMONA_SANCTION_NIVEAU_4
   * (cas le plus représentatif : sanction art. 181 + cotisations rétroactives).
   */
  function response(
    overrides: Partial<TravailNoirBeDimonaResponse> = {},
  ): TravailNoirBeDimonaResponse {
    return {
      caseFileId: 'case-1',
      statutDimona: 'ABSENTE',
      dateDebutOccupation: '2026-01-15',
      dateDimonaEffective: null,
      dateControle: '2026-04-15',
      salaireBrutMensuel: 2500,
      nombreTravailleursConcernes: 1,
      personneMorale: true,
      recidiveDansLAn: false,
      elementsSubordination: 'OUI',
      verdict: 'ABSENCE_DIMONA_SANCTION_NIVEAU_4',
      dureeNonDeclareeJours: 90,
      cotisationsOnssEmployeur: 1875.00,
      cotisationsOnssTravailleur: 980.25,
      cotisationsOnssTotal: 2855.25,
      amendeOnssForfaitaire3x: 8565.75,
      sanctionPenaleNiveau4Applicable: true,
      amendePenaleAdminMin: 300,
      amendePenaleAdminMax: 3000,
      amendePenaleMin: 600,
      amendePenaleMax: 6000,
      emprisonnementApplicable: true,
      emprisonnementMinMois: 6,
      emprisonnementMaxMois: 36,
      multiplicationParTravailleur: false,
      majorationPersonneMorale: true,
      majorationRecidive: false,
      raison: 'ABSENCE_DIMONA',
      synthese: 'Absence DIMONA — sanction art. 181 niveau 4 + cotisations rétroactives.',
      baseJuridique: 'Loi-programme 24/12/2002 art. 167-184 + AR 05/11/2002 + C. pén. soc. art. 181.',
      avertissement: 'Indexation +0,90 décimes additionnels depuis le 01/01/2025 (AR 27/12/2024).',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        TravailNoirBeDimonaSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(TravailNoirBeDimonaSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  // ============================================================
  // Gate pays + init
  // ============================================================

  it('BELGIQUE => isAvailable() true, GET au ngOnInit', () => {
    expect(component.isAvailable()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  });

  it('FRANCE => isAvailable() false, aucun GET au ngOnInit', () => {
    component.workspaceCountry = 'FRANCE';
    expect(component.isAvailable()).toBe(false);
    component.ngOnInit();
    httpMock.expectNone((r) => r.url === BASE_URL);
  });

  it('FRANCE => bannière "réservé droit BE" + masquage form', () => {
    component.workspaceCountry = 'FRANCE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.empty-result');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('belge');
    expect(banner.textContent).toContain('DPAE');
  });

  // ============================================================
  // GET initial
  // ============================================================

  it('GET 200 => hydrate result + mode résultat + rehydrate form', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    expect(component.result()!.verdict).toBe('ABSENCE_DIMONA_SANCTION_NIVEAU_4');
    expect(component.showForm()).toBe(false);
    expect(component.statutDimona()).toBe('ABSENTE');
    expect(component.dateDebutOccupation()).toBe('2026-01-15');
    expect(component.dateControle()).toBe('2026-04-15');
    expect(component.salaireBrutMensuel()).toBe(2500);
    expect(component.nombreTravailleursConcernes()).toBe(1);
    expect(component.personneMorale()).toBe(true);
    expect(component.recidiveDansLAn()).toBe(false);
    expect(component.elementsSubordination()).toBe('OUI');
  });

  it('GET 404 => reste en mode formulaire vierge', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.result()).toBeNull();
    expect(component.showForm()).toBe(true);
  });

  // ============================================================
  // Form valid
  // ============================================================

  it('formValid() : false sans champs', () => {
    expect(component.formValid()).toBe(false);
  });

  function fillForm() {
    component.statutDimona.set('ABSENTE');
    component.dateDebutOccupation.set('2026-01-15');
    component.dateControle.set('2026-04-15');
    component.salaireBrutMensuel.set(2500);
    component.nombreTravailleursConcernes.set(1);
    component.personneMorale.set(true);
    component.recidiveDansLAn.set(false);
    component.elementsSubordination.set('OUI');
  }

  it('formValid() : true avec les champs requis remplis', () => {
    fillForm();
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans statutDimona', () => {
    fillForm();
    component.statutDimona.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans dateDebutOccupation', () => {
    fillForm();
    component.dateDebutOccupation.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans dateControle', () => {
    fillForm();
    component.dateControle.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans salaireBrutMensuel', () => {
    fillForm();
    component.salaireBrutMensuel.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec salaire négatif', () => {
    fillForm();
    component.salaireBrutMensuel.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans nombreTravailleursConcernes', () => {
    fillForm();
    component.nombreTravailleursConcernes.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec nb travailleurs = 0', () => {
    fillForm();
    component.nombreTravailleursConcernes.set(0);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec nb travailleurs > 1M', () => {
    fillForm();
    component.nombreTravailleursConcernes.set(1000001);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans personneMorale', () => {
    fillForm();
    component.personneMorale.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans recidiveDansLAn', () => {
    fillForm();
    component.recidiveDansLAn.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans elementsSubordination', () => {
    fillForm();
    component.elementsSubordination.set(null);
    expect(component.formValid()).toBe(false);
  });

  // ============================================================
  // calculate() — POST + states
  // ============================================================

  it('calculate() => POST + snack succès', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    expect(post.request.method).toBe('POST');
    expect(post.request.body.statutDimona).toBe('ABSENTE');
    expect(post.request.body.salaireBrutMensuel).toBe(2500);
    expect(post.request.body.elementsSubordination).toBe('OUI');
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Analyse Travail noir DIMONA calculée', 'OK', expect.anything(),
    );
  });

  it('calculate() : erreur 400 => snack avec message backend', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({ message: 'salaireBrutMensuel : 9 chiffres avant la virgule, 2 après' },
      { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'salaireBrutMensuel : 9 chiffres avant la virgule, 2 après', 'Fermer', expect.anything(),
    );
  });

  it('calculate() : erreur 500 => snack générique', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({}, { status: 500, statusText: 'Server Error' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Une erreur est survenue. Veuillez réessayer.', 'Fermer', expect.anything(),
    );
  });

  it('calculate() : erreur 404 => snack "Dossier introuvable"', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({}, { status: 404, statusText: 'Not Found' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Dossier introuvable', 'Fermer', expect.anything(),
    );
  });

  it('calculate() : FRANCE => snack indispo + pas de POST', () => {
    component.workspaceCountry = 'FRANCE';
    fillForm();
    component.calculate();
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Outil indisponible pour ce workspace', 'Fermer', expect.anything(),
    );
    httpMock.expectNone((r) => r.url === BASE_URL && r.method === 'POST');
  });

  // ============================================================
  // Verdict — label / classe / icône (6 cas)
  // ============================================================

  it('verdict ABSENCE_DIMONA_SANCTION_NIVEAU_4 => critical, gavel', () => {
    component.result.set(response());
    expect(component.verdictLabel()).toContain('Absence DIMONA');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('gavel');
  });

  it('verdict REQUALIFICATION_SALARIE_PRESUMEE => critical, report', () => {
    component.result.set(response({ verdict: 'REQUALIFICATION_SALARIE_PRESUMEE' }));
    expect(component.verdictLabel()).toContain('Présomption');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('report');
  });

  it('verdict INDEPENDANT_REQUALIFIE_NON_DECLARE => critical, warning', () => {
    component.result.set(response({ verdict: 'INDEPENDANT_REQUALIFIE_NON_DECLARE' }));
    expect(component.verdictLabel()).toContain('Faux indépendant');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('warning');
  });

  it('verdict DIMONA_TARDIVE_REGULARISABLE => warn, schedule', () => {
    component.result.set(response({
      verdict: 'DIMONA_TARDIVE_REGULARISABLE',
      statutDimona: 'DECLAREE_TARDIVE',
      dateDimonaEffective: '2026-02-15',
      sanctionPenaleNiveau4Applicable: false,
      amendePenaleAdminMin: 0, amendePenaleAdminMax: 0,
      amendePenaleMin: 0, amendePenaleMax: 0,
      emprisonnementApplicable: false,
      emprisonnementMinMois: 0, emprisonnementMaxMois: 0,
    }));
    expect(component.verdictLabel()).toContain('tardive');
    expect(component.verdictClass()).toBe('verdict-warn');
    expect(component.verdictIcon()).toBe('schedule');
  });

  it('verdict DIMONA_CONFORME => info, check_circle', () => {
    component.result.set(response({
      verdict: 'DIMONA_CONFORME',
      statutDimona: 'DECLAREE_AVANT_DEBUT',
      dateDimonaEffective: '2026-01-10',
      sanctionPenaleNiveau4Applicable: false,
      amendePenaleAdminMin: 0, amendePenaleAdminMax: 0,
      amendePenaleMin: 0, amendePenaleMax: 0,
      emprisonnementApplicable: false,
      emprisonnementMinMois: 0, emprisonnementMaxMois: 0,
      cotisationsOnssEmployeur: 0,
      cotisationsOnssTravailleur: 0,
      cotisationsOnssTotal: 0,
      amendeOnssForfaitaire3x: 0,
    }));
    expect(component.verdictLabel()).toContain('conforme');
    expect(component.verdictClass()).toBe('verdict-info');
    expect(component.verdictIcon()).toBe('check_circle');
  });

  it('verdict A_QUALIFIER => neutral, help_outline', () => {
    component.result.set(response({
      verdict: 'A_QUALIFIER',
      elementsSubordination: 'INDETERMINE',
      sanctionPenaleNiveau4Applicable: false,
      amendePenaleAdminMin: 0, amendePenaleAdminMax: 0,
      amendePenaleMin: 0, amendePenaleMax: 0,
      emprisonnementApplicable: false,
      emprisonnementMinMois: 0, emprisonnementMaxMois: 0,
    }));
    expect(component.verdictLabel()).toContain('Qualification ouverte');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('help_outline');
  });

  it('result null => labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('work_off');
  });

  // ============================================================
  // Badge dans header (6 cas)
  // ============================================================

  it('badge DIMONA_CONFORME = info', () => {
    expect(component.verdictBadge('DIMONA_CONFORME')).toBe('CONFORME');
    expect(component.verdictBadgeClass('DIMONA_CONFORME')).toBe('badge-info');
  });

  it('badge DIMONA_TARDIVE_REGULARISABLE = warn', () => {
    expect(component.verdictBadge('DIMONA_TARDIVE_REGULARISABLE')).toBe('TARDIVE');
    expect(component.verdictBadgeClass('DIMONA_TARDIVE_REGULARISABLE')).toBe('badge-warn');
  });

  it('badge ABSENCE_DIMONA_SANCTION_NIVEAU_4 = critical', () => {
    expect(component.verdictBadge('ABSENCE_DIMONA_SANCTION_NIVEAU_4')).toContain('ABSENCE');
    expect(component.verdictBadgeClass('ABSENCE_DIMONA_SANCTION_NIVEAU_4')).toBe('badge-critical');
  });

  it('badge REQUALIFICATION_SALARIE_PRESUMEE = critical', () => {
    expect(component.verdictBadge('REQUALIFICATION_SALARIE_PRESUMEE')).toContain('PRÉSOMPTION');
    expect(component.verdictBadgeClass('REQUALIFICATION_SALARIE_PRESUMEE')).toBe('badge-critical');
  });

  it('badge INDEPENDANT_REQUALIFIE_NON_DECLARE = critical', () => {
    expect(component.verdictBadge('INDEPENDANT_REQUALIFIE_NON_DECLARE')).toContain('FAUX');
    expect(component.verdictBadgeClass('INDEPENDANT_REQUALIFIE_NON_DECLARE')).toBe('badge-critical');
  });

  it('badge A_QUALIFIER = neutre', () => {
    expect(component.verdictBadge('A_QUALIFIER')).toBe('À QUALIFIER');
    expect(component.verdictBadgeClass('A_QUALIFIER')).toBe('badge-neutral');
  });

  // ============================================================
  // DOM — rendu verdict
  // ============================================================

  it('ABSENCE_DIMONA_SANCTION_NIVEAU_4 => badge rouge + ventilations avec emprisonnement', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-critical');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('ABSENCE');
    const ventilations = fixture.nativeElement.querySelectorAll('.ventilation-row');
    // 5 cotis (durée + employeur + travailleur + total + amende3x) + 2 sanction (admin + pénale)
    // + 1 emprisonnement + 3 majorations (×trav + ×PM + récidive) = 11.
    expect(ventilations.length).toBe(11);
  });

  it('DIMONA_CONFORME => badge info + 8 ventilations (pas de sanction/emprisonnement)', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'DIMONA_CONFORME',
      statutDimona: 'DECLAREE_AVANT_DEBUT',
      dateDimonaEffective: '2026-01-10',
      sanctionPenaleNiveau4Applicable: false,
      amendePenaleAdminMin: 0, amendePenaleAdminMax: 0,
      amendePenaleMin: 0, amendePenaleMax: 0,
      emprisonnementApplicable: false,
      emprisonnementMinMois: 0, emprisonnementMaxMois: 0,
      cotisationsOnssEmployeur: 0,
      cotisationsOnssTravailleur: 0,
      cotisationsOnssTotal: 0,
      amendeOnssForfaitaire3x: 0,
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-info');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('CONFORME');
    const ventilations = fixture.nativeElement.querySelectorAll('.ventilation-row');
    // 5 cotis fixes + 0 sanction + 0 emprisonnement + 3 majorations = 8.
    expect(ventilations.length).toBe(8);
  });

  // ============================================================
  // Libellés
  // ============================================================

  it('libellé statutDimona : 4 cas + null', () => {
    expect(component.statutDimonaLabel('DECLAREE_AVANT_DEBUT')).toContain('avant début');
    expect(component.statutDimonaLabel('DECLAREE_TARDIVE')).toContain('tardivement');
    expect(component.statutDimonaLabel('ABSENTE')).toContain('absente');
    expect(component.statutDimonaLabel('INDEPENDANT_FAUSSEMENT_QUALIFIE')).toContain('faussement');
    expect(component.statutDimonaLabel(null)).toBe('—');
  });

  it('libellé elementsSubordination : 3 cas + null', () => {
    expect(component.elementsSubordinationLabel('OUI')).toContain('caractérisée');
    expect(component.elementsSubordinationLabel('NON')).toContain('Aucun');
    expect(component.elementsSubordinationLabel('INDETERMINE')).toContain('Indéterminé');
    expect(component.elementsSubordinationLabel(null)).toBe('—');
  });

  // ============================================================
  // Formats
  // ============================================================

  it('formatEuros : décimales 2', () => {
    expect(component.formatEuros(2855.25)).toContain('2');
    expect(component.formatEuros(2855.25)).toContain('€');
    expect(component.formatEuros(null)).toBe('—');
    expect(component.formatEuros(undefined)).toBe('—');
  });

  it('formatEurosInt : entiers + €', () => {
    expect(component.formatEurosInt(3000)).toContain('3');
    expect(component.formatEurosInt(3000)).toContain('€');
    expect(component.formatEurosInt(null)).toBe('—');
  });

  it('formatBoolean : Oui / Non / —', () => {
    expect(component.formatBoolean(true)).toBe('Oui');
    expect(component.formatBoolean(false)).toBe('Non');
    expect(component.formatBoolean(null)).toBe('—');
    expect(component.formatBoolean(undefined)).toBe('—');
  });

  it('formatMoisRange : 0/0 => —, identique, range', () => {
    expect(component.formatMoisRange(0, 0)).toBe('—');
    expect(component.formatMoisRange(6, 6)).toBe('6 mois');
    expect(component.formatMoisRange(6, 36)).toContain('6 mois');
    expect(component.formatMoisRange(6, 36)).toContain('36 mois');
  });

  it('formatJours : null/0/1/N', () => {
    expect(component.formatJours(null)).toBe('—');
    expect(component.formatJours(undefined)).toBe('—');
    expect(component.formatJours(0)).toBe('0 jour');
    expect(component.formatJours(1)).toBe('1 jour');
    expect(component.formatJours(90)).toContain('90');
    expect(component.formatJours(90)).toContain('jours');
  });

  // ============================================================
  // Avertissement
  // ============================================================

  it('avertissement non vide => bannière ambre rendue', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      avertissement: 'Vérifier indexation à la date de la décision.',
    }));
    fixture.detectChanges();
    const alerts = fixture.nativeElement.querySelectorAll('.result-alert');
    const text = Array.from(alerts).map((a: unknown) => (a as HTMLElement).textContent).join(' ');
    expect(text).toContain('indexation');
  });

  // ============================================================
  // Synthèse
  // ============================================================

  it('synthèse affichée si présente', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      synthese: 'Absence DIMONA — niveau 4.',
    }));
    fixture.detectChanges();
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: unknown) => (a as HTMLElement).textContent).join(' ');
    expect(text).toContain('DIMONA');
  });

  // ============================================================
  // F-177 SF-177-12 — parité getPrefillCount static
  // ============================================================

  it('static getPrefillCount => 0 (V1)', () => {
    expect(TravailNoirBeDimonaSectionComponent.getPrefillCount({})).toBe(0);
    expect(TravailNoirBeDimonaSectionComponent.getPrefillCount({
      workspaceCountry: 'BELGIQUE',
      aiData: {
        dateNaissanceSalarie: '1985-06-12',
        dateRuptureContrat: '2026-04-10',
        motifRupture: 'autre',
        anneesCarriereSalarie: 8,
        salaireBrutMensuel: 2400,
        salaireBrutAnnuel: 28800,
      },
    })).toBe(0);
  });

  // ============================================================
  // editMode => re-affiche le form
  // ============================================================

  it('editMode() => showForm true', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  // ============================================================
  // F-JU-03 — citation jurisprudentielle bien câblée
  // ============================================================

  it('toolId pour jurisprudence = travail-noir-be-dimona', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('travail-noir-be-dimona');
  });

  // ============================================================
  // Toggle collapse + forceExpanded
  // ============================================================

  it('toggleCollapse() flips collapsed signal', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('forceExpanded => collapsed=false au ngOnInit', () => {
    component.forceExpanded = true;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    expect(component.collapsed()).toBe(false);
  });

  // ============================================================
  // Handlers signals
  // ============================================================

  it('handlers mettent à jour les signals correctement', () => {
    component.onStatutDimonaChange('ABSENTE');
    expect(component.statutDimona()).toBe('ABSENTE');

    component.onDateDebutOccupationChange('2026-01-15');
    expect(component.dateDebutOccupation()).toBe('2026-01-15');

    component.onDateDimonaEffectiveChange('2026-02-01');
    expect(component.dateDimonaEffective()).toBe('2026-02-01');

    component.onDateControleChange('2026-04-15');
    expect(component.dateControle()).toBe('2026-04-15');

    component.onSalaireBrutMensuelChange('2500');
    expect(component.salaireBrutMensuel()).toBe(2500);

    component.onNombreTravailleursConcernesChange('3');
    expect(component.nombreTravailleursConcernes()).toBe(3);

    component.onPersonneMoraleChange(true);
    expect(component.personneMorale()).toBe(true);

    component.onRecidiveDansLAnChange(false);
    expect(component.recidiveDansLAn()).toBe(false);

    component.onElementsSubordinationChange('OUI');
    expect(component.elementsSubordination()).toBe('OUI');

    component.onSalaireBrutMensuelChange('');
    expect(component.salaireBrutMensuel()).toBeNull();

    component.onNombreTravailleursConcernesChange(null);
    expect(component.nombreTravailleursConcernes()).toBeNull();
  });
});
