/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2018 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.webauthn.data;

import org.forgerock.json.jose.jwk.JWK;

/**
 * Represents https://www.w3.org/TR/webauthn/#attested-credential-data.
 */
public class AttestedCredentialData {

    /** AAGUID of the authenticator. **/
    public final byte[] aaguid;

    /** byte length of credentialId. **/
    public final int credentialIdLength;

    /** https://www.w3.org/TR/webauthn/#credential-id. **/
    public final byte[] credentialId;

    /** https://www.w3.org/TR/webauthn/#credential-public-key. **/
    public final JWK publicKey;

    /** The algorithm to use with the key. */
    public final String algorithm;

    /**
     * THe constructor.
     *
     * @param aaguid the aaguid.
     * @param credentialIdLength the credential id length of bytes.
     * @param credentialId the credential id.
     * @param publicKey the public key.
     * @param algorithm the algorithm to use.
     */
    public AttestedCredentialData(byte[] aaguid, int credentialIdLength, byte[] credentialId, JWK publicKey,
                                  String algorithm) {
        this.aaguid = aaguid;
        this.credentialIdLength = credentialIdLength;
        this.credentialId = credentialId;
        this.publicKey = publicKey;
        this.algorithm = algorithm;
    }
}
