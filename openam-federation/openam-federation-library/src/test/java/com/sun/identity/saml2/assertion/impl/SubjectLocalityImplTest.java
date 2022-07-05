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

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class SubjectLocalityImplTest {

    @DataProvider
    public Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<saml:SubjectLocality xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "Address=\"testAddress\" DNSName=\"test.example.com\"/>" },
                { true, false, "<saml:SubjectLocality " +
                        "Address=\"testAddress\" DNSName=\"test.example.com\"/>" },
                { false, false, "<SubjectLocality " +
                        "Address=\"testAddress\" DNSName=\"test.example.com\"/>" }
        };
    }

    @Test(dataProvider = "xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        SubjectLocalityImpl subjectLocality = new SubjectLocalityImpl();
        subjectLocality.setAddress("testAddress");
        subjectLocality.setDNSName("test.example.com");

        // When
        String xml = subjectLocality.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }

}