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
 * Copyright 2021 ForgeRock AS.
 */

package com.sun.identity.saml2.assertion.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AuthnContextImplTest {

    @DataProvider
    public Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<saml:AuthnContext xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">" +
                        "<saml:AuthnContextClassRef>" +
                        "testAcr</saml:AuthnContextClassRef>" +
                        "<saml:AuthnContextDeclRef>testDeclRef" +
                        "</saml:AuthnContextDeclRef>" +
                        "<saml:AuthenticatingAuthority>a</saml:AuthenticatingAuthority>" +
                        "<saml:AuthenticatingAuthority>b</saml:AuthenticatingAuthority></saml:AuthnContext>" },
                { true, false, "<saml:AuthnContext>" +
                        "<saml:AuthnContextClassRef>" +
                        "testAcr</saml:AuthnContextClassRef>" +
                        "<saml:AuthnContextDeclRef>testDeclRef" +
                        "</saml:AuthnContextDeclRef>" +
                        "<saml:AuthenticatingAuthority>a</saml:AuthenticatingAuthority>" +
                        "<saml:AuthenticatingAuthority>b</saml:AuthenticatingAuthority></saml:AuthnContext>" },
                { false, false, "<AuthnContext>" +
                        "<AuthnContextClassRef>" +
                        "testAcr</AuthnContextClassRef>" +
                        "<AuthnContextDeclRef>testDeclRef</AuthnContextDeclRef>" +
                        "<AuthenticatingAuthority>a</AuthenticatingAuthority>" +
                        "<AuthenticatingAuthority>b</AuthenticatingAuthority></AuthnContext>" }
        };
    }

    @Test(dataProvider = "xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        AuthnContextImpl authnContext = new AuthnContextImpl();
        authnContext.setAuthnContextClassRef("testAcr");
        authnContext.setAuthnContextDeclRef("testDeclRef");
        authnContext.setAuthenticatingAuthority(List.of("a", "b"));

        // When
        String xml = authnContext.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}