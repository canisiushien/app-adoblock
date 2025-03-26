/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bf.canisiuslab.adoblock.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * DTO pour la paire de clés
 *
 * @author Canisius <canisiushien@gmail.com>
 */
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class KeysPairDTO {

    /** type d'algo cryptographique */
    private String typeKey;

    /** courbe elliptique */
    private String ellipticCurve;

    /** clé privée */
    private String privateKey;

    /** clé publique */
    private String publicKey;
}
