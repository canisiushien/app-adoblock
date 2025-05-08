package bf.canisiuslab.adoblock.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * DTO pour les donnees provenant/envoyées de la blockchain Ethereum
 *
 * @author Canisius <canisiushien@gmail.com>
 */
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DocumentETH {

    /** nom du fichier */
    private String fileName;

    /** empreinte numerique encode en base64. A transmettre pour saving */
    private String hashEncoded;

    /** signature numerique encode en base64. A transmettre pour saving */
    private String signedHashEncoded;

    /** cle publique encode en base64. A transmettre pour saving */
    private String publicKeyEncoded;

    /** horodatage de stockage genere automatiquement par Ethereum */
    private long timestamp;
}