package bf.canisiuslab.adoblock.service;

import bf.canisiuslab.adoblock.service.dto.KeysPairDTO;
import bf.canisiuslab.adoblock.service.dto.ResponseAddDTO;
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
     * enregistre un document administratif dans la blockchain Ethereum
     * 
     * @param digitalDocument
     * @param privateKey
     * @param publicKey
     * @return
     * @throws InvalidKeyException
     * @throws Exception
     */
    ResponseAddDTO addDocumentToBlockchain(MultipartFile digitalDocument, String privateKeyEncoded,
            String publicKeyEncoded)
            throws InvalidKeyException, Exception;

    /**
     * extrait du texte brut dans un fichier, calcule le hash et verifie que
     * fichier en question est valide
     *
     * @param digitalDocument
     * @return
     * @throws InvalidKeyException
     */
    ResponseVerifDTO verifyDocumentFromBlockchain(MultipartFile digitalDocument) throws InvalidKeyException;
}
