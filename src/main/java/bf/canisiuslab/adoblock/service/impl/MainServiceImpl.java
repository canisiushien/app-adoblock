package bf.canisiuslab.adoblock.service.impl;

import bf.canisiuslab.adoblock.exception.CustomException;
import bf.canisiuslab.adoblock.utils.HashUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.security.*;
import java.util.Base64;
import java.util.Date;
import java.security.spec.ECGenParameterSpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import bf.canisiuslab.adoblock.service.MainService;
import bf.canisiuslab.adoblock.service.dto.DocumentETH;
import bf.canisiuslab.adoblock.service.dto.KeysPairDTO;
import bf.canisiuslab.adoblock.service.dto.ResponseVerifDTO;

/**
 * Implementation de la logique metier d'authentification de doc admin
 * 
 * @author Canisius <canisiushien@gmail.com>
 */
@Slf4j
@Service
public class MainServiceImpl implements MainService {

    /** Construteur */
    public MainServiceImpl() {
        // Rien à y mettre pour l'instant
    }

    /**
     * genere une paire de cles cryptographiques
     * 
     * @return un objet contenant les cles cryptographiques
     * @throws NoSuchAlgorithmException en cas d'erreur d'exécution
     */
    @Override
    public KeysPairDTO generateKeysPair() throws NoSuchAlgorithmException {
        log.info("Generation de paire de cles ECDSA");
        KeysPairDTO keysPair = new KeysPairDTO();
        try {
            // Génération d'une paire de clés ECDSA (Courbe P-256)
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(HashUtil.KEYGEN_ALGORITHM);
            keyGen.initialize(new ECGenParameterSpec(HashUtil.KEYGEN_PARAMETER));
            KeyPair keyPair = keyGen.generateKeyPair();
            PrivateKey privateKey = keyPair.getPrivate();
            PublicKey publicKey = keyPair.getPublic();

            keysPair.setPrivateKey(Base64.getEncoder().encodeToString(privateKey.getEncoded()));
            keysPair.setPublicKey(Base64.getEncoder().encodeToString(publicKey.getEncoded()));
            keysPair.setTypeKey(HashUtil.TYPE_KEY);
            keysPair.setEllipticCurve(HashUtil.ELLIPTIC_CURVE);

        } catch (InvalidAlgorithmParameterException ex) {
            throw new CustomException("Une erreur s'est produite lors de la generation des cles. \n" + ex);
        }

        return keysPair;
    }

    /**
     * fourni les données sur le document administratif à stocker dans la blockchain
     *
     * -extraire le contenu textuel du fichier
     * -calculer le hash (empreinte numerique)
     * -signer le hash avec la clé privée
     * 
     * @param digitalDocument   fichier numerique du document administratif
     * @param trustedKeys       fichier .crt contenant les clé
     * @param privateKeyEncoded encodé en Base64
     * @param publicKeyEncoded  encodé en Base64
     * @return une message sur l'exécution de l'opération
     * @throws Exception           en cas d'erreur d'exécution
     * @throws InvalidKeyException en cas d'erreur d'exécution
     */
    @Override
    public DocumentETH prepareDocToStore(MultipartFile digitalDocument, MultipartFile trustedKeys,
            String privateKeyEncoded, String publicKeyEncoded) throws InvalidKeyException, Exception {
        log.info("Préparation du document {} pour stockage Ethereum", digitalDocument.getOriginalFilename());
        // controle et validation de paramaetres
        if (trustedKeys == null && (privateKeyEncoded.strip() == null || publicKeyEncoded.strip() == null)) {
            throw new CustomException("Veuillez, soit renseigner les 2 clés soit charger le fichier .crt de clés SVP.");
        }

        // recuperation des clés du fichier
        if (trustedKeys != null) {
            KeysPairDTO extractedKeys;
            extractedKeys = this.certificateReader(trustedKeys);
            privateKeyEncoded = extractedKeys.getPrivateKey();
            publicKeyEncoded = extractedKeys.getPublicKey();
        }

        /**
         * pour le hash du contenu qui sera calculé et pour la signature numérique (le
         * hash qui sera signé/chiffré via la clé privée) du document
         */
        byte[] hash;
        byte[] signedHash;
        /**
         * contient le contenu du document qui sera extrait et la reponse d'execution de
         * la transaction de stockage blockchain
         */
        String content;
        DocumentETH response = new DocumentETH();
        response.setFileName(digitalDocument.getOriginalFilename());
        /** récuperation de l'extension du fichier */
        String fileType = digitalDocument.getContentType();

        // verification de l'extension du fichier
        if (fileType.equals(HashUtil.PDF_TYPE)) {
            content = this.extractTextFromPdf(digitalDocument);
        } else if (fileType.equals(HashUtil.WORD_TYPE)) {
            content = this.extractTextFromWord(digitalDocument);
        } else {
            throw new CustomException(
                    "Le type du document n'est pas supporté. Veuillez réessayer avec un PDF ou Word SVP.");
        }

        // calcul de l'empreinte numerique du contenu du document
        hash = HashUtil.calculateHashWithSHA256(content);
        // chiffrement de l'empreinte numerique (la signature numérique)
        signedHash = this.signHashWithPrivateKey(hash, privateKeyEncoded.strip());

        // Conversion en Base64 pour stockage dans la blockchain Ethereum
        String hashEncoded = Base64.getEncoder().encodeToString(hash);
        String signedHashEncoded = Base64.getEncoder().encodeToString(signedHash);

        // envoi des valeurs ci-dessous au Smart Contract pour stockage dans Ethereum
        log.info("hash: {}", hash);
        log.info("hashEncoded: {}", hashEncoded);
        log.info("signedHashEncoded: {}", signedHashEncoded);
        log.info("Public Key: {}", publicKeyEncoded.strip());

        response.setHashEncoded(hashEncoded);
        response.setSignedHashEncoded(signedHashEncoded);
        response.setPublicKeyEncoded(publicKeyEncoded.strip());

        return response;
    }

    /**
     * fourni les données sur le document administratif à stocker dans la blockchain
     *
     * -extraire le contenu textuel du fichier
     * -calculer le hash (empreinte numerique)
     * 
     * @param digitalDocument  fichier numerique du document administratif
     * @param publicKeyEncoded encodé en Base64
     * @return une message sur l'exécution de l'opération
     * @throws Exception en cas d'erreur d'exécution
     */
    @Override
    public DocumentETH prepareDocToGet(MultipartFile digitalDocument) throws Exception {
        log.info("Préparation du document {} pour recherche Ethereum", digitalDocument.getOriginalFilename());

        // pour le hash du contenu qui sera calculé
        byte[] hash;

        /**
         * contient le contenu du document qui sera extrait et la reponse d'execution de
         * la transaction de stockage blockchain
         */
        String content;
        DocumentETH response = new DocumentETH();
        response.setFileName(digitalDocument.getOriginalFilename());
        /** récuperation de l'extension du fichier */
        String fileType = digitalDocument.getContentType();

        // verification de l'extension du fichier
        if (fileType.equals(HashUtil.PDF_TYPE)) {
            content = this.extractTextFromPdf(digitalDocument);
        } else if (fileType.equals(HashUtil.WORD_TYPE)) {
            content = this.extractTextFromWord(digitalDocument);
        } else {
            throw new CustomException(
                    "Le type du document n'est pas supporte. Veuillez réessayer avec un PDF ou Word SVP.");
        }

        // calcul de l'empreinte numerique du contenu du document
        hash = HashUtil.calculateHashWithSHA256(content);
        // Conversion en Base64 pour stockage dans la blockchain Ethereum
        String hashEncoded = Base64.getEncoder().encodeToString(hash);
        response.setHashEncoded(hashEncoded);

        return response;
    }

    /**
     * verifie si le document est valide
     *
     * @param digitalDoc données du document administratif a authentifier
     * @return un objet contenant les infos issues de l'authentification
     */
    @Override
    public ResponseVerifDTO verifyDocumentFromBlockchain(ResponseVerifDTO digitalDoc) {
        log.info("Vérification de l'authenticité du document digital : {}", digitalDoc);
        /** construction de l'objet pour la reponse */
        ResponseVerifDTO response = new ResponseVerifDTO();
        response.setTypeKey(HashUtil.TYPE_KEY);
        response.setEllipticCurve(HashUtil.ELLIPTIC_CURVE);
        response.setFileName(digitalDoc.getFileName());
        response.setIntegrated(true);
        response.setNewHashEncoded(digitalDoc.getNewHashEncoded().strip());
        response.setHashEncodedStored(digitalDoc.getHashEncodedStored().strip());
        response.setSignedHashEncodedStored(digitalDoc.getSignedHashEncodedStored().strip());
        response.setPublicKeyStored(digitalDoc.getPublicKeyStored().strip());

        // Convertir les données récupérées de Base64 en bytes
        byte[] storedHash = HashUtil.decodeBase64ToByteArray(response.getHashEncodedStored());
        // Convertir les données calculées de Base64 en bytes
        byte[] newHash = HashUtil.decodeBase64ToByteArray(response.getNewHashEncoded());

        /**
         * Comparaison du hash recalculé avec celui stocké (ou reçu de) sur la
         * blockchain
         */
        if (!MessageDigest.isEqual(newHash, storedHash)) {
            response.setIntegrated(false);
            log.info("Le document a été modifié !");
        }

        // Vérification de la signature avec la clé publique
        boolean isValid = false;
        try {
            isValid = this.verifySignatureWithPublicKey(response.getHashEncodedStored(),
                    response.getSignedHashEncodedStored(),
                    response.getPublicKeyStored());
            response.setAuthenticated(isValid);
        } catch (InvalidKeyException e) {
            log.info("Erreur survenue lors de la verification de la clé privée. {}", e);
        } catch (Exception ex) {
            log.info("Erreur inatendue survenue. {}", ex);
        }

        return response;
    }

    // =========================================================================
    // ========================= PRIVATE METHODS ===============================
    // =========================================================================
    /**
     * extrait et retourne du texte brut contenu dans fichier PDF
     *
     * @param file fichier numerique du document administratif a authentifier
     * @return le texte du fichier numerique (document administratif a
     *         stocker/authentifier)
     * @throws IOException en cas d'erreur d'exécution
     */
    private String extractTextFromPdf(MultipartFile file) throws IOException {
        log.info("Extraction du contenu d'un PDF textuel : {}", file.getOriginalFilename());
        PDDocument document = PDDocument.load(file.getInputStream());
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);
        document.close();

        return text;
    }

    /**
     * extrait et retourne du texte brut contenu dans fichier Word
     *
     * @param file fichier numerique du document administratif a authentifier
     * @return le texte du fichier numerique (document administratif a
     *         stocker/authentifier)
     * @throws IOException en cas d'erreur d'exécution
     */
    private String extractTextFromWord(MultipartFile file) throws IOException {
        log.info("Extraction du contenu d'un word : {}", file.getOriginalFilename());
        XWPFDocument document = new XWPFDocument(file.getInputStream());
        XWPFWordExtractor extractor = new XWPFWordExtractor(document);
        String text = extractor.getText();
        document.close();

        return text;
    }

    /**
     * chiffre le hash du document à l'aide de la clé privée (signature numérique)
     * 
     * @param hash              empreinte numerique du document admin a stocker
     * @param privateKeyEncoded cle privee du signataire dudit document
     * @return la signature numerique (hash chiffre) dudit document
     * @throws InvalidKeyException en cas d'erreur d'exécution
     * @throws Exception           en cas d'erreur d'exécution
     */
    private byte[] signHashWithPrivateKey(byte[] hash, String privateKeyEncoded) throws InvalidKeyException, Exception {
        log.info("Chiffrement (via privateKey) du hash calcule [signature numerique du doc].");
        Signature signature = Signature.getInstance(HashUtil.SIGNATURE_ALGORITHM);
        /** convertir String privateKeyEncoded en objet PrivateKey */
        signature.initSign(HashUtil.decodePrivateKey(privateKeyEncoded));
        signature.update(hash);

        return signature.sign();
    }

    /**
     * verifie via la clé publique(reçue de la blockchain), la signature numérique
     * reçue de la blockchain
     * 
     * @param storedHashEncoded       hash encode en base 64 venant d'Ethereum
     * @param storedSignedHashEncoded signature numerique encodee en base 64 venant
     *                                d'Ethereum
     * @param publicKeyEncoded        cle publique encodee en base 64 venant
     *                                d'Ethereum
     * @return resultat booleen apres verification
     * @throws Exception           en cas d'erreur d'exécution
     * @throws InvalidKeyException en cas d'erreur d'exécution
     */
    private boolean verifySignatureWithPublicKey(String storedHashEncoded, String storedSignedHashEncoded,
            String publicKeyEncoded) throws InvalidKeyException, Exception {
        log.info("Verification (via publicKey) de la signature numerique d'un hash");
        Signature signature = Signature.getInstance(HashUtil.SIGNATURE_ALGORITHM);
        signature.initVerify(HashUtil.decodePublicKey(publicKeyEncoded));
        signature.update(HashUtil.decodeBase64ToByteArray(storedHashEncoded));

        return signature.verify(HashUtil.decodeBase64ToByteArray(storedSignedHashEncoded));
    }

    /**
     * extrait les clés dans un tableau de chaines
     * 
     * @param file
     * @return
     */
    private KeysPairDTO certificateReader(MultipartFile file) {
        log.info("Extraction des clés du fichier : {}", file.getOriginalFilename());

        KeysPairDTO keysPair = new KeysPairDTO();
        try {
            // Lire le fichier en String
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);

            // Séparer les clés en fonction de l'espace
            String[] keys = content.split(" ");

            if (keys.length == 2) {
                keysPair.setPrivateKey(keys[0].trim());
                keysPair.setPublicKey(keys[1].trim());
            } else {
                throw new CustomException("Format incorrect du fichier. Impossible d'extraire les clés.");
            }
        } catch (IOException e) {
            log.info("Erreur lors de la lecture du fichier : {}", e.getMessage());
        }

        return keysPair;
    }
}
