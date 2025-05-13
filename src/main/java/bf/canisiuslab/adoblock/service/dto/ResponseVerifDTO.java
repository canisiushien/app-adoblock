package bf.canisiuslab.adoblock.service.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * DTO pour le resultat de verification d'authenticite
 *
 * @author Canisius <canisiushien@gmail.com>
 */
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResponseVerifDTO {

    /** le doc est-il valide/authentique ? */
    private Boolean authenticated;

    /** le contenu du doc est-il au moins integre ? */
    private Boolean integrated;

    /** date a laquelle le document a ete ajoute a la blockchain (horodatage) */
    private Long horodatage;

    /** nom du fichier soumis a authentification (Document numetique) */
    private String fileName;

    /** date de demande d'authentification (Date de demande) */
    private Instant requestDate;

    /** type d'algo cryptographique */
    private String typeKey;

    /** courbe elliptique */
    private String ellipticCurve;

    /** cle publique associee */
    private String publicKeyStored;

    /** hash calculé et soumis à eth pour la recherche dans eth */
    private String newHashEncoded;

    /** hash encodé en base64 et stocké sur eth auparavant */
    private String hashEncodedStored;

    /** hash signé encodé en base64 et stocké sur eth auparavant */
    private String signedHashEncodedStored;
}