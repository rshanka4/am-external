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

package com.sun.identity.saml2.protocol.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

public class StatusDetailImplTest {

    @DataProvider
    public Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<samlp:StatusDetail xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\">" +
                        "<foo>bar</foo>" +
                        "<test/>" +
                        "</samlp:StatusDetail>" },
                { true, false, "<samlp:StatusDetail>" +
                        "<foo>bar</foo>" +
                        "<test/>" +
                        "</samlp:StatusDetail>" },
                { false, false, "<StatusDetail>" +
                        "<foo>bar</foo>" +
                        "<test/>" +
                        "</StatusDetail>" }
        };
    }

    @Test(dataProvider = "xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        StatusDetailImpl statusDetail = new StatusDetailImpl();
        statusDetail.setAny(List.of("<foo>bar</foo>", "<test/>"));

        // When
        String xml = statusDetail.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}