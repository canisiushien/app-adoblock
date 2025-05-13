package bf.canisiuslab.adoblock.controller;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import bf.canisiuslab.adoblock.service.dto.KeysPairDTO;
import bf.canisiuslab.adoblock.service.dto.ResponseVerifDTO;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import bf.canisiuslab.adoblock.service.MainService;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller principal d'API REST.
 * 
 * **API de generation de paire de cles cryptographiques
 * **API d'enregistrement de doc admin sur Ethereum
 * **API de verification de l'authenticite d'un doc admin
 * 
 * @author Canisius <canisiushien@gmail.com>
 */
@CrossOrigin("*")
@RestController
@RequestMapping("/api/adoblock")
public class MainController {

    private final MainService service;

    public MainController(MainService fileService) {
        this.service = fileService;
    }

    /**
     * génère une paire de clés cryptographiques
     *
     * @return
     * @throws NoSuchAlgorithmException
     */
    @GetMapping(path = "/generate-keys")
    public ResponseEntity<KeysPairDTO> getKeysPair() throws NoSuchAlgorithmException {
        return ResponseEntity.status(HttpStatus.OK).body(service.generateKeysPair());
    }

    /**
     * extrait et calcul le necessaire pour l'enregistrement d'un document
     * administratif sur la blockchain
     * 
     * 
     * @param documentAdministratif
     * @return
     * @throws Exception
     * @throws InvalidKeyException
     */
    @PostMapping(value = "/prepare-store-to-blockchain", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> prepareDocumentToSaveEthereum(
            @RequestParam(name = "file", required = true) MultipartFile documentAdministratif,
            @RequestParam(name = "fileKey", required = false) MultipartFile trustedKeys,
            @RequestParam(name = "privateKey", required = false) String privateKey,
            @RequestParam(name = "publicKey", required = false) String publicKey)
            throws InvalidKeyException, Exception {

        return ResponseEntity.status(HttpStatus.OK)
                .body(service.prepareDocToStore(documentAdministratif, trustedKeys, privateKey, publicKey));
    }

    /**
     * extrait et calcul le necessaire pour la recherche d'un document administratif
     * depuis la blockchain
     * 
     * @param file
     * @return
     * @throws Exception
     * @throws InvalidKeyException
     */
    @PostMapping(value = "/prepare-get-from-blockchain", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> prepareDocumentToGetEthereum(
            @RequestParam(name = "file", required = true) MultipartFile file)
            throws Exception {

        return ResponseEntity.status(HttpStatus.OK)
                .body(service.prepareDocToGet(file));

    }

    /**
     * effectue les 2 vérifications pour confirmer/infirmer l'authenticité du doc
     * 
     * 
     * @param digitalDoc
     * @return
     */
    @PostMapping(value = "/verify-from-blockchain")
    public ResponseEntity<?> verifyDocumentFromBlockchain(@RequestBody ResponseVerifDTO digitalDoc) {

        return ResponseEntity.status(HttpStatus.OK)
                .body(service.verifyDocumentFromBlockchain(digitalDoc));
    }
}
