package fr.ailegalcase.casefile;

import java.util.function.BiFunction;

/**
 * SF-163-03 : descripteur d'un calculator stateless exposé via le dispatcher
 * {@code POST /api/v1/simulators/{toolId}/calculate}.
 *
 * <p>Encapsule l'identifiant du tool, le type de payload de requête (utilisé
 * par Jackson pour la désérialisation) et le handler qui invoque le calculator
 * pur. <b>Aucune persistance</b> n'est effectuée — le dispatcher contourne le
 * service "case-file scoped" qui appellerait {@code repository.save()}.</p>
 *
 * <p>Le handler reçoit le pays résolu depuis le workspace primary de
 * l'utilisateur authentifié (cf. {@code SimulatorsCatalogService}). Les
 * calculators qui n'ont pas besoin du pays peuvent l'ignorer ; ceux qui en ont
 * besoin l'utilisent pour valider le motif/régime/etc.</p>
 *
 * @param toolId       identifiant exact présent dans {@code TOOL_REGISTRY}
 *                     frontend et {@code decision_tool_visibility_rules.tool_id}
 * @param requestType  classe du payload de requête (mêmes champs que le request
 *                     case-file scoped correspondant)
 * @param handler      fonction qui invoque le calculator pur et retourne le
 *                     payload de réponse (mêmes champs que la response case-file
 *                     scoped correspondante). Reçoit {@code (request, country)}.
 * @param <Req>        type de la requête désérialisée
 */
public record SimulatorCalculatorDescriptor<Req>(
        String toolId,
        Class<Req> requestType,
        BiFunction<Req, String, Object> handler
) {}
