package bf.canisiuslab.adoblock.service;

import bf.canisiuslab.adoblock.service.dto.DocumentETH;
import bf.canisiuslab.adoblock.service.dto.KeysPairDTO;
import bf.canisiuslab.adoblock.service.dto.ResponseVerifDTO;

import org.springframework.web.multipart.MultipartFile;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Prototypage des services de logique metier
 * 
 * @author Canisius <canisiushien@gmail.com>
 */
public interface MainService {

        /**
         * genere une paire de clés cryptographiques
         *
         * @return
         */
        KeysPairDTO generateKeysPair() throws NoSuchAlgorithmException;

        /**
         * recoit le fichier, extait le contenu, hash le contenu, chiffre le hash pour
         * retourner hash, hashChiffré, publicKey.
         * 
         * @param digitalDocument
         * @param trustedKeys
         * @param privateKeyEncoded
         * @param publicKeyEncoded
         * @return
         * @throws InvalidKeyException
         * @throws Exception
         */
        DocumentETH prepareDocToStore(MultipartFile digitalDocument, MultipartFile trustedKeys,
                        String privateKeyEncoded,
                        String publicKeyEncoded)
                        throws InvalidKeyException, Exception;

        /**
         * recoit le fichier, extait le contenu, et hash le contenu pour le retourner
         * 
         * @param digitalDocument
         * @return
         * @throws Exception
         */
        DocumentETH prepareDocToGet(MultipartFile digitalDocument) throws Exception;

        /**
         * recoit les hash, hashChiffré, publicKey de la blockchain et verifie
         * l'authenticité du docment
         * 
         * @param digitalDoc
         * @return
         */
        ResponseVerifDTO verifyDocumentFromBlockchain(ResponseVerifDTO digitalDoc);
}
