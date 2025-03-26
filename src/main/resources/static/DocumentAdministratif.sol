// SPDX-License-Identifier: GPL-3.0
pragma solidity ^0.8.20;

import "hardhat/console.sol";

/**
 * @author canisiushien@gmail.com
 * 
 * @title CONTRAT INTELLIGENT "DocumentAdministratif".
 * @dev Stocke et recherche un document administratif (hash) dans Ethereum.
 * @dev Ce contrat est une intéraction entre deux parties : le document administratif et l'utilisateur.
 * @dev date-implCompilDeployAndTest: 10 mars 2025.
 */
contract DocumentAdministratif {
    //definition de la structure d'un document a stocker
    struct Document {
        string hashEncoded; //hash du document
        string signedHashEncoded; //signature numerique du document
        string publicKeyEncoded; //cle publique du signataire
        uint256 timestamp; //date et heure(horodatage) de stockage du document dans la blockchain
    }

    //mappage(appele "documents") de la structure du document avec une chaine (le hash)
    mapping(string => Document) private documents;

    //creation d'un evenement d'indexage (tel hash+horodatage correspond a tel ensemble de donnees w = signedHashEncoded+publicKeyEncoded)
    event DocumentStored(string indexed hashEncoded, uint256 timestamp);

    //fonction de stockage de document dans la blockchain
    function storeAdministrativeDocument(string memory _hashEncoded, string memory _signedHashEncoded, string memory _publicKeyEncoded) public {
        //controle de doublons de hash dans le stockage
        require(bytes(documents[_hashEncoded].hashEncoded).length == 0, "Ce document existe deja dans la blockchain.");

        //stocke le document dans une map
        documents[_hashEncoded] = Document({
            hashEncoded: _hashEncoded,
            signedHashEncoded: _signedHashEncoded,
            publicKeyEncoded: _publicKeyEncoded,
            timestamp: block.timestamp
        });

        console.log("_hashEncoded = ", _hashEncoded);
        console.log("_signedHashEncoded = ", _signedHashEncoded);
        console.log("_publicKeyEncoded = ", _publicKeyEncoded);
        console.log("timestamp = ", block.timestamp);

        //emettre l'evenement a l'attention des utilisateurs
        emit DocumentStored(_hashEncoded, block.timestamp);
    }

    //fonction de recherche/recuperation de document(hash) dans la blockchain
    function getAdministrativeDocument(string memory _hashEncoded) public view returns (string memory, string memory, string memory, uint256){
         console.log("Recherche de _hashEncoded = ", _hashEncoded);
        //on leve une exception si un hash similaire n'avait pas ete stocké dans la blockchain
        require(bytes(documents[_hashEncoded].hashEncoded).length != 0, "Ce document inexistant dans la blockchain.");

        Document memory doc = documents[_hashEncoded];
        return (doc.hashEncoded, doc.signedHashEncoded, doc.publicKeyEncoded, doc.timestamp);
    }
}