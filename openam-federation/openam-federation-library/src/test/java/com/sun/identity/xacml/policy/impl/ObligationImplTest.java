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

package com.sun.identity.xacml.policy.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;

import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.xacml.common.XACMLConstants;

public class ObligationImplTest {

    @DataProvider
    public Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<xacml:Obligation " +
                        "xmlns:xacml=\"urn:oasis:names:tc:xacml:2.0:policy:schema:os\" " +
                        "FulfillOn=\"some day\" " +
                        "ObligationId=\"urn:test\">" +
                        "<test1 AttributeId=\"foo\" " +
                        "DataType=\"bar\"/>" +
                        "<test2 AttributeId=\"foo\" " +
                        "DataType=\"bar\"/>" +
                        "</xacml:Obligation>" },
                { true, false, "<xacml:Obligation " +
                        "FulfillOn=\"some day\" " +
                        "ObligationId=\"urn:test\">" +
                        "<test1 AttributeId=\"foo\" " +
                        "DataType=\"bar\"/>" +
                        "<test2 AttributeId=\"foo\" " +
                        "DataType=\"bar\"/>" +
                        "</xacml:Obligation>" },
                { false, false, "<Obligation " +
                        "FulfillOn=\"some day\" " +
                        "ObligationId=\"urn:test\">" +
                        "<test1 AttributeId=\"foo\" " +
                        "DataType=\"bar\"/>" +
                        "<test2 AttributeId=\"foo\" " +
                        "DataType=\"bar\"/>" +
                        "</Obligation>" }
        };
    }

    @Test(dataProvider = "xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        ObligationImpl obligation = new ObligationImpl();
        obligation.setObligationId(URI.create("urn:test"));
        obligation.setFulfillOn("some day");
        Document document = XMLUtils.newDocument();
        Element attr1 = document.createElement("test1");
        attr1.setAttribute(XACMLConstants.ATTRIBUTE_ID, "foo");
        attr1.setAttribute(XACMLConstants.DATA_TYPE, "bar");
        Element attr2 = document.createElement("test2");
        attr2.setAttribute(XACMLConstants.ATTRIBUTE_ID, "foo");
        attr2.setAttribute(XACMLConstants.DATA_TYPE, "bar");
        obligation.setAttributeAssignments(List.of(attr1, attr2));

        // When
        String xml = obligation.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }

}