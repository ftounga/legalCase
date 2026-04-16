package fr.ailegalcase.casefile;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/anciennete/baremes")
public class BaremeController {

    @GetMapping("/{conventionCode}")
    public BaremeResponse get(@PathVariable String conventionCode) {
        ConventionBareme bareme = ConventionBaremeReferentiel.getByCode(conventionCode);
        if (bareme == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Convention inconnue : " + conventionCode);
        }
        return new BaremeResponse(
                bareme.code(),
                bareme.label(),
                bareme.country(),
                bareme.congesLegauxJours(),
                bareme.congesSupplementaires().stream()
                        .map(c -> new BaremeResponse.CongesSupplementaireData(
                                c.ancienneteMinAnnees(), c.joursSupplementaires())).toList(),
                bareme.primesAnciennete().stream()
                        .map(p -> new BaremeResponse.PrimeAncienneteData(
                                p.ancienneteMinAnnees(), p.pourcentage())).toList()
        );
    }
}
