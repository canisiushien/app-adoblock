package bf.canisiuslab.adoblock.service.impl;

import bf.canisiuslab.adoblock.entity.DocumentAdmin;
import bf.canisiuslab.adoblock.exception.CustomException;
import bf.canisiuslab.adoblock.repository.DocumentAdminRepository;
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
import java.time.Instant;

import bf.canisiuslab.adoblock.service.EthereumService;
import bf.canisiuslab.adoblock.service.MainService;
import bf.canisiuslab.adoblock.service.dto.DocumentETH;
import bf.canisiuslab.adoblock.service.dto.KeysPairDTO;
import bf.canisiuslab.adoblock.service.dto.ResponseAddDTO;
import bf.canisiuslab.adoblock.service.dto.ResponseVerifDTO;

/**
 * Implementation de la logique metier d'authentification de doc admin
 * 
 * @author Canisius <canisiushien@gmail.com>
 */
@Slf4j
@Service
public class MainServiceImpl implements MainService {

    private final DocumentAdminRepository documentAdminRepository;

    private final EthereumService ethereumService;

    /** Construteur */
    public MainServiceImpl(DocumentAdminRepository documentAdminRepository, EthereumService ethereumService) {
        this.documentAdminRepository = documentAdminRepository;
        this.ethereumService = ethereumService;
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
     * enregistre un document administratif dans la blockchain
     *
     * -extraire le contenu textuel du fichier
     * -calculer le hash (empreinte numerique)
     * -signer le hash avec la clé privée
     * -enregister le doc dans une BDR
     * 
     * @param digitalDocument   fichier numerique du document administratif
     * @param privateKeyEncoded encodé en Base64
     * @param publicKeyEncoded  encodé en Base64
     * @return une message sur l'exécution de l'opération
     * @throws Exception           en cas d'erreur d'exécution
     * @throws InvalidKeyException en cas d'erreur d'exécution
     */
    @Override
    public ResponseAddDTO addDocumentToBlockchain(MultipartFile digitalDocument, String privateKeyEncoded,
            String publicKeyEncoded)
            throws InvalidKeyException, Exception {
        log.info("Enregistrement du document {} dans la blockchain", digitalDocument.getOriginalFilename());
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
        ResponseAddDTO response = new ResponseAddDTO();
        response.setDocumentName(digitalDocument.getOriginalFilename());
        /** récuperation de l'extension du fichier */
        String fileType = digitalDocument.getContentType();
        Instant horodatage = new Date().toInstant();

        // verification de l'extension du fichier
        if (fileType.equals(HashUtil.PDF_TYPE)) {
            content = this.extractTextFromPdf(digitalDocument);
        } else if (fileType.equals(HashUtil.WORD_TYPE)) {
            content = this.extractTextFromWord(digitalDocument);
        } else {
            throw new CustomException(
                    "Le type du document n'est pas supporte. Veuillez reessayer avec un PDF ou Word SVP.");
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
        log.info("horodatage: {}", horodatage);

        this.saveDocument(content, digitalDocument.getOriginalFilename(), hash, HashUtil.byteArrayToStringHex(hash),
                hashEncoded, signedHashEncoded,
                privateKeyEncoded.strip(), publicKeyEncoded.strip(), horodatage);

        return response;
    }

    /**
     * verifie si le document est valide
     *
     * @param file fichier numerique du document administratif a authentifier
     * @return un objet contenant les infos issues de l'authentification
     * @throws InvalidKeyException en cas d'erreur d'exécution
     */
    @Override
    public ResponseVerifDTO verifyDocumentFromBlockchain(MultipartFile digitalDocument) throws InvalidKeyException {
        log.info("Verification de l'authenticite du document {} dans la blockchain",
                digitalDocument.getOriginalFilename());

        String fileType = digitalDocument.getContentType();
        String content = null;
        /** construction de l'objet pour la reponse */
        ResponseVerifDTO response = new ResponseVerifDTO();
        response.setTypeKey(HashUtil.TYPE_KEY);
        response.setEllipticCurve(HashUtil.ELLIPTIC_CURVE);
        response.setFileName(digitalDocument.getOriginalFilename());
        response.setIntegrated(true);
        response.setRequestDate(new Date().toInstant());

        try {
            if (fileType.equals(HashUtil.PDF_TYPE)) {
                content = this.extractTextFromPdf(digitalDocument);
            } else if (fileType.equals(HashUtil.WORD_TYPE)) {
                content = this.extractTextFromWord(digitalDocument);
            } else {
                throw new CustomException(
                        "Le type du document n'est pas supporte. Veuillez reessayer avec un PDF ou Word SVP.");
            }
        } catch (IOException e) {
            log.info("Erreur survenue lors du traitement du fichier. {}", e);
        }

        // Calculer l'empreinte numérique du document à authentifier
        byte[] newHash = HashUtil.calculateHashWithSHA256(content);
        // utilisé pour la recherche dans la blockchain
        String newHashEncoded = Base64.getEncoder().encodeToString(newHash);

        // Recuperation des données sur la blockchain

        DocumentAdmin check = documentAdminRepository
                .findByHashEncodeBase64(newHashEncoded).orElse(null);
        String storedHashEncoded = check.getHashEncodeBase64();
        String storedSignedHashEncoded = check.getSignatureEncodeBase64();
        String storedPublicKeyEncoded = check.getPublicKeyEncodeBase64();

        // Convertir les données récupérées de Base64 en bytes
        byte[] storedHash = HashUtil.decodeBase64ToByteArray(storedHashEncoded);

        /**
         * Comparaison du hash recalculé avec celui stocké (ou reçu de) sur la
         * blockchain
         */
        if (!MessageDigest.isEqual(newHash, storedHash)) {
            response.setIntegrated(false);
            log.info("Le document a ete modifie !");
        }

        // Vérification de la signature avec la clé publique
        boolean isValid = false;
        try {
            isValid = this.verifySignatureWithPublicKey(storedHashEncoded, storedSignedHashEncoded,
                    storedPublicKeyEncoded);
            response.setAuthenticated(isValid);
        } catch (InvalidKeyException e) {
            log.info("Erreur survenue lors de la verification de la cle. {}", e);
        } catch (Exception ex) {
            log.info("Erreur inatendue survenue. {}", ex);
        }

        // Résultat final
        return response;
    }

    // ========================= PRIVATE METHODS ===============================
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
     * juste pour des tests de comparaison dans une BDR
     * 
     * @param texte
     * @param fileName
     * @param hash
     * @param hashHexa
     * @param hashEncode
     * @param signatureEncode
     * @param privateKey
     * @param publicKey
     * @param horodatage
     */
    private void saveDocument(String texte, String fileName, byte[] hash, String hashHexa, String hashEncode,
            String signatureEncode,
            String privateKey, String publicKey, Instant horodatage) {
        log.info("Enregistrement d'un doc dans un BDR");
        DocumentAdmin documentAdmin = new DocumentAdmin();

        documentAdmin.setDateChargement(Instant.now());
        documentAdmin.setNomFichier(fileName);
        documentAdmin.setVersionJson(texte);
        documentAdmin.setHash(hash);
        documentAdmin.setHashHexa(hashHexa);
        documentAdmin.setHashEncodeBase64(hashEncode);
        documentAdmin.setSignatureEncodeBase64(signatureEncode);
        documentAdmin.setPrivateKeyEncodeBase64(privateKey);
        documentAdmin.setPublicKeyEncodeBase64(publicKey);
        documentAdmin.setDateChargement(horodatage);
        documentAdminRepository.save(documentAdmin);
    }

    // cI-dessous la bonne version blockchain
    // private final DocumentAdminRepository documentAdminRepository;

    // private final EthereumService ethereumService;

    // /** Construteur */
    // public MainServiceImpl(DocumentAdminRepository documentAdminRepository,
    // EthereumService ethereumService) {
    // this.documentAdminRepository = documentAdminRepository;
    // this.ethereumService = ethereumService;
    // }

    // /**
    // * genere une paire de cles cryptographiques
    // *
    // * @return un objet contenant les cles cryptographiques
    // * @throws NoSuchAlgorithmException en cas d'erreur d'exécution
    // */
    // @Override
    // public KeysPairDTO generateKeysPair() throws NoSuchAlgorithmException {
    // log.info("Generation de paire de cles ECDSA");
    // KeysPairDTO keysPair = new KeysPairDTO();
    // try {
    // // Génération d'une paire de clés ECDSA (Courbe P-256)
    // KeyPairGenerator keyGen =
    // KeyPairGenerator.getInstance(HashUtil.KEYGEN_ALGORITHM);
    // keyGen.initialize(new ECGenParameterSpec(HashUtil.KEYGEN_PARAMETER));
    // KeyPair keyPair = keyGen.generateKeyPair();
    // PrivateKey privateKey = keyPair.getPrivate();
    // PublicKey publicKey = keyPair.getPublic();

    // keysPair.setPrivateKey(Base64.getEncoder().encodeToString(privateKey.getEncoded()));
    // keysPair.setPublicKey(Base64.getEncoder().encodeToString(publicKey.getEncoded()));
    // keysPair.setTypeKey(HashUtil.TYPE_KEY);
    // keysPair.setEllipticCurve(HashUtil.ELLIPTIC_CURVE);

    // } catch (InvalidAlgorithmParameterException ex) {
    // throw new CustomException("Une erreur s'est produite lors de la generation
    // des cles. \n" + ex);
    // }

    // return keysPair;
    // }

    // /**
    // * enregistre un document administratif dans la blockchain
    // *
    // * -extraire le contenu textuel du fichier
    // * -calculer le hash (empreinte numerique)
    // * -signer le hash avec la clé privée
    // * -enregister le doc dans une BDR
    // *
    // * @param digitalDocument fichier numerique du document administratif
    // * @param privateKeyEncoded encodé en Base64
    // * @param publicKeyEncoded encodé en Base64
    // * @return une message sur l'exécution de l'opération
    // * @throws Exception en cas d'erreur d'exécution
    // * @throws InvalidKeyException en cas d'erreur d'exécution
    // */
    // @Override
    // public ResponseAddDTO addDocumentToBlockchain(MultipartFile digitalDocument,
    // String privateKeyEncoded,
    // String publicKeyEncoded)
    // throws InvalidKeyException, Exception {
    // log.info("Enregistrement du document {} dans la blockchain",
    // digitalDocument.getOriginalFilename());
    // /**
    // * pour le hash du contenu qui sera calculé et pour la signature numérique (le
    // * hash qui sera signé/chiffré via la clé privée) du document
    // */
    // byte[] hash;
    // byte[] signedHash;
    // privateKeyEncoded = privateKeyEncoded.strip();
    // publicKeyEncoded = publicKeyEncoded.strip();
    // /**
    // * contient le contenu du document qui sera extrait et la reponse d'execution
    // de
    // * la transaction de stockage blockchain
    // */
    // String content;
    // ResponseAddDTO response = new ResponseAddDTO();
    // /** récuperation de l'extension du fichier */
    // String fileType = digitalDocument.getContentType();
    // Instant horodatage = new Date().toInstant();

    // // verification de l'extension du fichier
    // if (fileType.equals(HashUtil.PDF_TYPE)) {
    // content = this.extractTextFromPdf(digitalDocument);
    // } else if (fileType.equals(HashUtil.WORD_TYPE)) {
    // content = this.extractTextFromWord(digitalDocument);
    // } else {
    // throw new CustomException(
    // "Le type du document n'est pas supporte. Veuillez reessayer avec un PDF ou
    // Word SVP.");
    // }

    // // calcul de l'empreinte numerique du contenu du document
    // hash = HashUtil.calculateHashWithSHA256(content);
    // // chiffrement de l'empreinte numerique (la signature numérique)
    // signedHash = this.signHashWithPrivateKey(hash, privateKeyEncoded);

    // // Conversion en Base64 pour stockage dans la blockchain Ethereum
    // String hashEncoded = Base64.getEncoder().encodeToString(hash);
    // String signedHashEncoded = Base64.getEncoder().encodeToString(signedHash);

    // // envoi des valeurs ci-dessous au Smart Contract pour stockage dans Ethereum
    // log.info("hash: {}", hash);
    // log.info("hashEncoded: {}", hashEncoded);
    // log.info("signedHashEncoded: {}", signedHashEncoded);
    // log.info("Public Key: {}", publicKeyEncoded);
    // log.info("horodatage: {}", horodatage);
    // response = ethereumService.storeDocument(hashEncoded, signedHashEncoded,
    // publicKeyEncoded);

    // this.saveDocument(content, digitalDocument.getOriginalFilename(), hash,
    // HashUtil.byteArrayToStringHex(hash),
    // hashEncoded, signedHashEncoded,
    // privateKeyEncoded, publicKeyEncoded, horodatage);

    // return response;
    // }

    // /**
    // * verifie si le document est valide
    // *
    // * @param file fichier numerique du document administratif a authentifier
    // * @return un objet contenant les infos issues de l'authentification
    // * @throws InvalidKeyException en cas d'erreur d'exécution
    // */
    // @Override
    // public ResponseVerifDTO verifyDocumentFromBlockchain(MultipartFile
    // digitalDocument) throws InvalidKeyException {
    // log.info("Verification de l'authenticite du document {} dans la blockchain",
    // digitalDocument.getOriginalFilename());

    // String fileType = digitalDocument.getContentType();
    // String content = null;
    // /** construction de l'objet pour la reponse */
    // ResponseVerifDTO response = new ResponseVerifDTO();
    // response.setTypeKey(HashUtil.TYPE_KEY);
    // response.setEllipticCurve(HashUtil.ELLIPTIC_CURVE);
    // response.setFileName(digitalDocument.getOriginalFilename());
    // response.setIntegrated(true);
    // response.setRequestDate(new Date().toInstant());

    // try {
    // if (fileType.equals(HashUtil.PDF_TYPE)) {
    // content = this.extractTextFromPdf(digitalDocument);
    // } else if (fileType.equals(HashUtil.WORD_TYPE)) {
    // content = this.extractTextFromWord(digitalDocument);
    // } else {
    // throw new CustomException(
    // "Le type du document n'est pas supporte. Veuillez reessayer avec un PDF ou
    // Word SVP.");
    // }
    // } catch (IOException e) {
    // log.info("Erreur survenue lors du traitement du fichier. {}", e);
    // }

    // // Calculer l'empreinte numérique du document à authentifier
    // byte[] newHash = HashUtil.calculateHashWithSHA256(content);
    // // utilisé pour la recherche dans la blockchain
    // String newHashEncoded = Base64.getEncoder().encodeToString(newHash);

    // // Recuperation des données sur la blockchain
    // /*
    // * DocumentAdmin check = documentAdminRepository
    // * .findByHashEncodeBase64(newHashEncoded).orElse(null);
    // * String storedHashEncoded = check.getHashEncodeBase64();
    // * String storedSignedHashEncoded = check.getSignatureEncodeBase64();
    // * String storedPublicKeyEncoded = check.getPublicKeyEncodeBase64();
    // */
    // DocumentETH documentFromEth = new DocumentETH();
    // try {
    // documentFromEth = ethereumService.getDocument(newHashEncoded);
    // } catch (Exception e) {
    // log.info("Erreur survenue lors de recuperation de doc sur Ethereum. {}", e);
    // }
    // String storedHashEncoded = documentFromEth.getHashEncoded();
    // String storedSignedHashEncoded = documentFromEth.getSignedHashEncoded();
    // String storedPublicKeyEncoded = documentFromEth.getPublicKeyEncoded();

    // response.setDocHashed(newHashEncoded);
    // response.setPublicKey(storedPublicKeyEncoded);
    // response.setHorodatage(documentFromEth.getTimestamp());

    // // Convertir les données récupérées de Base64 en bytes
    // byte[] storedHash = HashUtil.decodeBase64ToByteArray(storedHashEncoded);

    // /**
    // * Comparaison du hash recalculé avec celui stocké (ou reçu de) sur la
    // * blockchain
    // */
    // if (!MessageDigest.isEqual(newHash, storedHash)) {
    // response.setIntegrated(false);
    // log.info("Le document a ete modifie !");
    // }

    // // Vérification de la signature avec la clé publique
    // boolean isValid = false;
    // try {
    // isValid = this.verifySignatureWithPublicKey(storedHashEncoded,
    // storedSignedHashEncoded,
    // storedPublicKeyEncoded);
    // response.setAuthenticated(isValid);
    // } catch (InvalidKeyException e) {
    // log.info("Erreur survenue lors de la verification de la cle. {}", e);
    // } catch (Exception ex) {
    // log.info("Erreur inatendue survenue. {}", ex);
    // }

    // // Résultat final
    // return response;
    // }

    // // ========================= PRIVATE METHODS ===============================
    // /**
    // * extrait et retourne du texte brut contenu dans fichier PDF
    // *
    // * @param file fichier numerique du document administratif a authentifier
    // * @return le texte du fichier numerique (document administratif a
    // * stocker/authentifier)
    // * @throws IOException en cas d'erreur d'exécution
    // */
    // private String extractTextFromPdf(MultipartFile file) throws IOException {
    // log.info("Extraction du contenu d'un PDF textuel : {}",
    // file.getOriginalFilename());
    // PDDocument document = PDDocument.load(file.getInputStream());
    // PDFTextStripper stripper = new PDFTextStripper();
    // String text = stripper.getText(document);
    // document.close();

    // return text;
    // }

    // /**
    // * extrait et retourne du texte brut contenu dans fichier Word
    // *
    // * @param file fichier numerique du document administratif a authentifier
    // * @return le texte du fichier numerique (document administratif a
    // * stocker/authentifier)
    // * @throws IOException en cas d'erreur d'exécution
    // */
    // private String extractTextFromWord(MultipartFile file) throws IOException {
    // log.info("Extraction du contenu d'un word : {}", file.getOriginalFilename());
    // XWPFDocument document = new XWPFDocument(file.getInputStream());
    // XWPFWordExtractor extractor = new XWPFWordExtractor(document);
    // String text = extractor.getText();
    // document.close();

    // return text;
    // }

    // /**
    // * chiffre le hash du document à l'aide de la clé privée (signature numérique)
    // *
    // * @param hash empreinte numerique du document admin a stocker
    // * @param privateKeyEncoded cle privee du signataire dudit document
    // * @return la signature numerique (hash chiffre) dudit document
    // * @throws InvalidKeyException en cas d'erreur d'exécution
    // * @throws Exception en cas d'erreur d'exécution
    // */
    // private byte[] signHashWithPrivateKey(byte[] hash, String privateKeyEncoded)
    // throws InvalidKeyException, Exception {
    // log.info("Chiffrement (via privateKey) du hash calcule [signature numerique
    // du doc].");
    // Signature signature = Signature.getInstance(HashUtil.SIGNATURE_ALGORITHM);
    // /** convertir String privateKeyEncoded en objet PrivateKey */
    // signature.initSign(HashUtil.decodePrivateKey(privateKeyEncoded));
    // signature.update(hash);

    // return signature.sign();
    // }

    // /**
    // * verifie via la clé publique(reçue de la blockchain), la signature numérique
    // * reçue de la blockchain
    // *
    // * @param storedHashEncoded hash encode en base 64 venant d'Ethereum
    // * @param storedSignedHashEncoded signature numerique encodee en base 64
    // venant
    // * d'Ethereum
    // * @param publicKeyEncoded cle publique encodee en base 64 venant
    // * d'Ethereum
    // * @return resultat booleen apres verification
    // * @throws Exception en cas d'erreur d'exécution
    // * @throws InvalidKeyException en cas d'erreur d'exécution
    // */
    // private boolean verifySignatureWithPublicKey(String storedHashEncoded, String
    // storedSignedHashEncoded,
    // String publicKeyEncoded) throws InvalidKeyException, Exception {
    // log.info("Verification (via publicKey) de la signature numerique d'un hash");
    // Signature signature = Signature.getInstance(HashUtil.SIGNATURE_ALGORITHM);
    // signature.initVerify(HashUtil.decodePublicKey(publicKeyEncoded));
    // signature.update(HashUtil.decodeBase64ToByteArray(storedHashEncoded));

    // return
    // signature.verify(HashUtil.decodeBase64ToByteArray(storedSignedHashEncoded));
    // }

    // /**
    // * juste pour des tests de comparaison dans une BDR
    // *
    // * @param texte
    // * @param fileName
    // * @param hash
    // * @param hashHexa
    // * @param hashEncode
    // * @param signatureEncode
    // * @param privateKey
    // * @param publicKey
    // * @param horodatage
    // */
    // private void saveDocument(String texte, String fileName, byte[] hash, String
    // hashHexa, String hashEncode,
    // String signatureEncode,
    // String privateKey, String publicKey, Instant horodatage) {
    // log.info("Enregistrement d'un doc dans un BDR");
    // DocumentAdmin documentAdmin = new DocumentAdmin();

    // documentAdmin.setDateChargement(Instant.now());
    // documentAdmin.setNomFichier(fileName);
    // documentAdmin.setVersionJson(texte);
    // documentAdmin.setHash(hash);
    // documentAdmin.setHashHexa(hashHexa);
    // documentAdmin.setHashEncodeBase64(hashEncode);
    // documentAdmin.setSignatureEncodeBase64(signatureEncode);
    // documentAdmin.setPrivateKeyEncodeBase64(privateKey);
    // documentAdmin.setPublicKeyEncodeBase64(publicKey);
    // documentAdmin.setDateChargement(horodatage);
    // documentAdminRepository.save(documentAdmin);
    // }

}
