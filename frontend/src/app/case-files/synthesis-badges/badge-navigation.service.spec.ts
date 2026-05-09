import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';

import { BadgeNavigationService } from './badge-navigation.service';
import { SynthesisShortBlockDialogComponent } from '../synthesis-short-block-dialog/synthesis-short-block-dialog.component';

/**
 * F-229 SF-229-01 / SF-229-03 — couvre la résolution {@link BadgeKey} →
 * destination pour les 8 clés exposées (CA-05 + CA-11 SF-229-03). Mocke
 * {@link Router} et {@link MatDialog} ; les composants destination ne sont
 * pas instanciés.
 */
describe('BadgeNavigationService', () => {
  let service: BadgeNavigationService;
  let router: jest.Mocked<Pick<Router, 'navigate'>>;
  let dialog: jest.Mocked<Pick<MatDialog, 'open'>>;

  const CASE_FILE_ID = 'case-1';

  beforeEach(() => {
    router = { navigate: jest.fn() } as any;
    dialog = { open: jest.fn() } as any;

    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: router },
        { provide: MatDialog, useValue: dialog },
      ],
    });
    service = TestBed.inject(BadgeNavigationService);
  });

  it('CA-05 T-01: go("pistes") → router.navigate vers /synthesis avec fragment section-pistes', () => {
    service.go('pistes', CASE_FILE_ID);
    expect(router.navigate).toHaveBeenCalledWith(
      ['/case-files', CASE_FILE_ID, 'synthesis'],
      { fragment: 'section-pistes' },
    );
    expect(dialog.open).not.toHaveBeenCalled();
  });

  it('CA-05 T-02: go("pieces", ctx) → MatDialog.open avec items, title, icon fournis', () => {
    service.go('pieces', CASE_FILE_ID, {
      popupItems: ['Contrat', 'Bulletin'],
      popupTitle: 'Pièces manquantes',
      popupIcon: 'report_problem',
    });
    expect(dialog.open).toHaveBeenCalledTimes(1);
    const args = dialog.open.mock.calls[0];
    expect(args[0]).toBe(SynthesisShortBlockDialogComponent);
    expect(args[1]?.data).toEqual({
      title: 'Pièces manquantes',
      icon: 'report_problem',
      items: ['Contrat', 'Bulletin'],
    });
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('CA-05 T-02b: go("pieces") sans contextData → popup avec items vides + fallback title/icon', () => {
    service.go('pieces', CASE_FILE_ID);
    expect(dialog.open).toHaveBeenCalledTimes(1);
    const args = dialog.open.mock.calls[0];
    expect(args[1]?.data).toEqual({
      title: 'Pièces manquantes',
      icon: 'report_problem',
      items: [],
    });
  });

  it('CA-05 T-03: go("risques") → router.navigate vers la sous-page /synthesis/risques', () => {
    service.go('risques', CASE_FILE_ID);
    expect(router.navigate).toHaveBeenCalledWith(
      ['/case-files', CASE_FILE_ID, 'synthesis', 'risques'],
    );
    expect(dialog.open).not.toHaveBeenCalled();
  });

  it('CA-05 T-04: go("questions") → router.navigate vers /synthesis avec fragment section-questions', () => {
    service.go('questions', CASE_FILE_ID);
    expect(router.navigate).toHaveBeenCalledWith(
      ['/case-files', CASE_FILE_ID, 'synthesis'],
      { fragment: 'section-questions' },
    );
    expect(dialog.open).not.toHaveBeenCalled();
  });

  it('CA-05 T-05: go("timeline") → router.navigate vers la sous-page /synthesis/timeline', () => {
    service.go('timeline', CASE_FILE_ID);
    expect(router.navigate).toHaveBeenCalledWith(
      ['/case-files', CASE_FILE_ID, 'synthesis', 'timeline'],
    );
  });

  it('CA-05 T-06: go("faits") → router.navigate vers la sous-page /synthesis/faits', () => {
    service.go('faits', CASE_FILE_ID);
    expect(router.navigate).toHaveBeenCalledWith(
      ['/case-files', CASE_FILE_ID, 'synthesis', 'faits'],
    );
  });

  it('CA-05 T-07: go("points-juridiques") → router.navigate vers la sous-page /synthesis/points-juridiques', () => {
    service.go('points-juridiques', CASE_FILE_ID);
    expect(router.navigate).toHaveBeenCalledWith(
      ['/case-files', CASE_FILE_ID, 'synthesis', 'points-juridiques'],
    );
  });

  // F-229 SF-229-03 (2026-05-09) — clé `checklist` ajoutée pour la tile résumé
  // F-193-procedure-checks-summary (procedure checks F-96 matérialisés).
  it('CA-11 (SF-229-03) T-08: go("checklist") → router.navigate vers /synthesis avec fragment section-checklist', () => {
    service.go('checklist', CASE_FILE_ID);
    expect(router.navigate).toHaveBeenCalledWith(
      ['/case-files', CASE_FILE_ID, 'synthesis'],
      { fragment: 'section-checklist' },
    );
    expect(dialog.open).not.toHaveBeenCalled();
  });
});
