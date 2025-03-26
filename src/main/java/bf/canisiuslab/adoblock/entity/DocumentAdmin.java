package bf.canisiuslab.adoblock.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Date;

/**
 *
 * @author Canisius <canisiushien@gmail.com>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@ToString
@Table(name = "document_administratif", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "hashEncodeBase64" }, name = "ux_hashEncodeBase64_in_documentadmin") })
public class DocumentAdmin {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Instant dateChargement;

    @Column(nullable = false)
    private String nomFichier;

    @Column(columnDefinition = "TEXT") // Mapping du type TEXT dans Spring Boot : Spécifie le type TEXT pour PostgreSQL
    private String versionJson;

    private byte[] hash;

    @Column(nullable = false, length = 64)
    private String hashHexa;

    private String hashEncodeBase64;

    private String signatureEncodeBase64;

    private String privateKeyEncodeBase64;

    private String publicKeyEncodeBase64;
}
