import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/**
 * F-288 / SF-288-01 — Un item curable d'une dimension de composition
 * (vague 1 : un outil décisionnel calculé). `included = true` ⇒ versé dans
 * l'acte ; `false` ⇒ exclu (case décochée à l'ouverture du modal).
 */
export interface CompositionItem {
  /** Clé stable de l'item (vague 1 : `toolId`). */
  key: string;
  /** Libellé court affiché dans le modal. */
  label: string;
  /** Vrai si l'item est intégré (non exclu) — pilote la case à cocher. */
  included: boolean;
}

/**
 * F-288 / SF-288-01 — Une dimension de composition (vague 1 : la seule
 * dimension `DECISION_TOOL` « Outils décisionnels »). Les vagues 2/3
 * ajouteront `HEAD_OF_CLAIM` et `ADVERSE_MOYEN` sans changer ce contrat.
 */
export interface CompositionDimension {
  /** Clé technique de la dimension (`DECISION_TOOL`, …). */
  key: string;
  /** Libellé de section affiché dans le modal. */
  label: string;
  /** Items curables de la dimension. */
  items: CompositionItem[];
}

/** F-288 / SF-288-01 — Composition complète d'un dossier (toutes dimensions). */
export interface Composition {
  dimensions: CompositionDimension[];
}

/** F-288 / SF-288-01 — Une exclusion (paire dimension × itemKey) envoyée en PUT. */
export interface CompositionExclusion {
  dimension: string;
  itemKey: string;
}

/**
 * F-288 / SF-288-01 — Accès à la **composition** des conclusions d'un dossier :
 * la liste des éléments curables par dimension (vague 1 : outils décisionnels
 * calculés) et la persistance de l'ensemble d'**exclusions** choisi par
 * l'avocat avant génération.
 *
 * Contrat figé (mini-spec SF-288-01) :
 *  - `GET  /api/v1/case-files/{id}/conclusions/composition` → `Composition`
 *  - `PUT  /api/v1/case-files/{id}/conclusions/composition`
 *      body `{ exclusions: [{ dimension, itemKey }] }` → `Composition`
 *
 * Le service est un simple wrapper HttpClient ; l'isolation workspace est
 * assurée côté backend (résolution du dossier).
 */
@Injectable({ providedIn: 'root' })
export class ConclusionCompositionService {
  private readonly http = inject(HttpClient);

  /** Liste des dimensions curables + items (`included` selon les exclusions persistées). */
  getComposition(caseFileId: string): Observable<Composition> {
    return this.http.get<Composition>(
      `/api/v1/case-files/${caseFileId}/conclusions/composition`,
    );
  }

  /**
   * Remplace l'ensemble d'exclusions des dimensions fournies puis renvoie la
   * composition à jour (même corps que `getComposition`).
   */
  saveComposition(
    caseFileId: string,
    exclusions: CompositionExclusion[],
  ): Observable<Composition> {
    return this.http.put<Composition>(
      `/api/v1/case-files/${caseFileId}/conclusions/composition`,
      { exclusions },
    );
  }
}
