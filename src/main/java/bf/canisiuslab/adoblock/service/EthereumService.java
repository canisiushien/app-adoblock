package bf.canisiuslab.adoblock.service;

import bf.canisiuslab.adoblock.service.dto.DocumentETH;
import bf.canisiuslab.adoblock.service.dto.ResponseAddDTO;

/**
 * Prototypage des services d'interaction avec Ethereum
 * 
 * @author Canisius <canisiushien@gmail.com>
 */
public interface EthereumService {

    /**
     * Stocke un document administratif haché sur la blockchain Ethereum.
     *
     * @param hashEncoded       Hachage du document (SHA-256)
     * @param signedHashEncoded Signature du hachage avec la clé privée
     * @param publicKeyEncoded  Clé publique associée
     * @return Hash de la transaction + d'autres elements
     * @throws Exception en cas d'erreur d'exécution
     */
    ResponseAddDTO storeDocument(String hashEncoded, String signedHashEncoded, String publicKeyEncoded)
            throws Exception;

    /**
     * Récupère un document stocké dans la blockchain Ethereum à partir de son hash.
     *
     * @param hashEncoded Hachage du document admin soumis à authentification
     * @return Un objet DocumentETH contenant les détails stockés
     * @throws Exception en cas d'erreur d'exécution
     */
    DocumentETH getDocument(String hashEncoded) throws Exception;

}