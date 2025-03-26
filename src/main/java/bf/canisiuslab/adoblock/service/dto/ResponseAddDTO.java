package bf.canisiuslab.adoblock.service.dto;

import java.math.BigInteger;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * DTO pour le resultat d'ajout de document dans la blockchain
 *
 * @author Canisius <canisiushien@gmail.com>
 */
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResponseAddDTO {

    /** le nom du fichier soumis à enregristrement */
    private String documentName;

    /** le hash (id) de la transaction */
    private String idTransaction;

    /** le numero du block contenant la transaction */
    private BigInteger numeroBlock;

    /** l'adresse du contrat intelligent execute */
    private String addressContrat;

    /**
     * la somme totale du gas utilisé par la transaction et par toutes les
     * transactions précédentes incluses dans le même bloc
     */
    private BigInteger totalBlockGasUtilise;

    /** la quantite de gas réellement consommée pour exécuter la transaction */
    private BigInteger totalTransactionGasUtilise;

    /** le prix reel de gaz paye pour la transaction */
    private String prixReelTransaction;

    /** l'etat d'execution(statut) de la transaction */
    private String statut;
}