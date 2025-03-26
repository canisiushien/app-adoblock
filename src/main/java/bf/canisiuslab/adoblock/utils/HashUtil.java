package bf.canisiuslab.adoblock.utils;

import lombok.extern.slf4j.Slf4j;

import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.*;
import java.util.Base64;

import bf.canisiuslab.adoblock.exception.CustomException;

/**
 * Constantes et fonctions utiles
 * 
 * @author Canisius <canisiushien@gmail.com>
 */
@Slf4j
public class HashUtil {

    /** algo de generation de cles */
    public static final String KEYGEN_ALGORITHM = "EC";

    /**
     * parametre specifique de generation. Cela precise l'usage de la courbe
     * elliptique P-256
     */
    public static final String KEYGEN_PARAMETER = "secp256r1";

    /** algo de hachage */
    public static final String HASH_ALGORITHM = "SHA-256";

    /** algo de signature par cles asymetriques */
    public static final String SIGNATURE_ALGORITHM = "SHA256withECDSA";

    /** type de paire de cles */
    public static final String TYPE_KEY = "ECDSA";

    /** type de courbe */
    public static final String ELLIPTIC_CURVE = "secp256r1 (P-256)";

    /** extension fichier pdf */
    public static final String PDF_TYPE = "application/pdf";

    /** extension fichier word */
    public static final String WORD_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    /** Calculer le hash d'un texte */
    public static byte[] calculateHashWithSHA256(String text) {
        try {
            // Initialiser le calculateur SHA-256
            // Java fournit une implémentation native de SHA-256 via la classe
            // MessageDigest. Donc, pas besoin de nouvelle dependance
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);

            // Convertir le texte en tableau d'octets
            return digest.digest(text.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new CustomException("Algorithm SHA-256 introuvable : " + e);
        }
    }

    /** Convertir en byte[] en String hexadecimal */
    public static String byteArrayToStringHex(byte[] hashBytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);

            /** Ajouter un zéro pour les valeurs < 0x10 si la taille = 1 */
            if (1 == hex.length())
                hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /** Convertir un String HEXA en byte[] */
    public static byte[] hexStringToByteArray(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Convertir la clé privée Base64 en PrivateKey. Les clés privées sont encodées
     * en PKCS8 (format PKCS8EncodedKeySpec)
     */
    public static PrivateKey decodePrivateKey(String privateKeyBase64) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEYGEN_ALGORITHM);
        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * Convertir la clé publique Base64 en PublicKey. Les clés publiques sont
     * encodées en X.509 (format X509EncodedKeySpec)
     */
    public static PublicKey decodePublicKey(String publicKeyBase64) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEYGEN_ALGORITHM);
        return keyFactory.generatePublic(keySpec);
    }

    /** Convertir un string encodé base64 en un tableau de bytes */
    public static byte[] decodeBase64ToByteArray(String textEncodedBase64) {
        return Base64.getDecoder().decode(textEncodedBase64);
    }
}