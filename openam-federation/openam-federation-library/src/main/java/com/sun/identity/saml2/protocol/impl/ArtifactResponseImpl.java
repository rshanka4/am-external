/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: ArtifactResponseImpl.java,v 1.2 2008/06/25 05:47:59 qcheng Exp $
 *
 * Portions Copyrighted 2019-2021 ForgeRock AS.
 */



package com.sun.identity.saml2.protocol.impl;

import static org.forgerock.openam.utils.StringUtils.isNotBlank;

import java.text.ParseException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.protocol.ArtifactResponse;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * The <code>ArtifactResopnse</code> message has the complex type
 * <code>ArtifactResponseType</code>.
 * <p>
 * <pre>
 * &lt;complexType name="ArtifactResponseType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:protocol}StatusResponseType"&gt;
 *       &lt;sequence&gt;
 *         &lt;any/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
public class ArtifactResponseImpl extends StatusResponseImpl
	implements ArtifactResponse {
    private static final Logger logger = LoggerFactory.getLogger(ArtifactResponseImpl.class);
    public static final String ARTIFACT_RESPONSE = "ArtifactResponse";
    private String anyString = null;
    
    private void parseElement(Element element)
        throws SAML2Exception {
        // make sure that the input xml block is not null
        if (element == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("ArtifactResponseImpl.parseElement: "
                    + "element input is null.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("nullInput"));
        }
        // Make sure this is an ArtifactResponse.
        String tag = null;
        tag = element.getLocalName();
        if ((tag == null) || (!tag.equals("ArtifactResponse"))) {
            if (logger.isDebugEnabled()) {
                logger.debug("ArtifactResponseImpl.parseElement: "
                    + "not ArtifactResponse.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("wrongInput"));
        }

        // handle the attributes of <ArtifactResponse> element
        NamedNodeMap atts = ((Node)element).getAttributes();
        if (atts != null) {
            int length = atts.getLength();
            for (int i = 0; i < length; i++) {
                Attr attr = (Attr) atts.item(i);
                String attrName = attr.getName();
                String attrValue = attr.getValue().trim();
                if (attrName.equals("ID")) {
                    responseId = attrValue;
                } else if (attrName.equals("InResponseTo")) {
                    inResponseTo = attrValue;
                } else if (attrName.equals("Version")) {
                    version = attrValue;
                } else if (attrName.equals("IssueInstant")) {
                    try {
                        issueInstant = DateUtils.stringToDate(attrValue);
                    } catch (ParseException pe) {
                        throw new SAML2Exception(pe.getMessage());
                    }
                } else if (attrName.equals("Destination")) {
                    destination = attrValue;
                } else if (attrName.equals("Consent")) {
                    consent = attrValue;
                }
            }
        }

        // handle child elements
        NodeList nl = element.getChildNodes();
        Node child;
        String childName;
        int length = nl.getLength();
        for (int i = 0; i < length; i++) {
            child = nl.item(i);
            if ((childName = child.getLocalName()) != null) {
                if (childName.equals("Issuer")) {
                    if (issuer != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("ArtifactResponseImpl."
				+ "parseElement: included more than one "
				+ "Issuer.");
                        }
                        throw new SAML2Exception(
                            SAML2SDKUtils.bundle.getString("moreElement"));
                    }
                    if (signatureString != null ||
                        extensions != null ||
                        status != null ||
			anyString != null)
                    {
                        if (logger.isDebugEnabled()) {
                            logger.debug("ArtifactResponseImpl."
				+ "parseElement:wrong sequence.");
                        }
                        throw new SAML2Exception(
                            SAML2SDKUtils.bundle.getString("schemaViolation"));
                    }
                    issuer = AssertionFactory.getInstance().createIssuer(
                        (Element) child);
                } else if (childName.equals("Signature")) {
                    if (signatureString != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("ArtifactResponseImpl."
				+ "parseElement:included more than one "
				+ "Signature.");
                        }
                        throw new SAML2Exception(
                            SAML2SDKUtils.bundle.getString("moreElement"));
                    }
                    if (extensions != null || status != null ||
			anyString != null)
		    {
                        if (logger.isDebugEnabled()) {
                            logger.debug("ArtifactResponseImpl."
				+ "parseElement:wrong sequence.");
                        }
                        throw new SAML2Exception(
                            SAML2SDKUtils.bundle.getString("schemaViolation"));
                    }
                    signatureString = XMLUtils.print((Element) child);
                    isSigned = true;
                } else if (childName.equals("Extensions")) {
                    if (extensions != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("ArtifactResponseImpl."
				+ "parseElement:included more than one "
				+ "Extensions.");
                        }
                        throw new SAML2Exception(
                            SAML2SDKUtils.bundle.getString("moreElement"));
                    }
                    if (status != null || anyString != null)
		    {
                        if (logger.isDebugEnabled()) {
                            logger.debug("ArtifactResponseImpl."
				+ "parseElement:wrong sequence.");
                        }
                        throw new SAML2Exception(
                            SAML2SDKUtils.bundle.getString("schemaViolation"));
                    }
                    extensions = ProtocolFactory.getInstance().createExtensions(
                        (Element) child);
                } else if (childName.equals("Status")) {
                    if (status != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("ArtifactResponseImpl."
				+ "parseElement: included more than one "
				+ "Status.");
                        }
                        throw new SAML2Exception(
                            SAML2SDKUtils.bundle.getString("moreElement"));
                    }
		    if (anyString != null) {
			 if (logger.isDebugEnabled()) {
                            logger.debug("ResponseImpl.parse"
                                + "Element:wrong sequence.");
                        }
                        throw new SAML2Exception(
                            SAML2SDKUtils.bundle.getString("schemaViolation"));
		    }
                    status = ProtocolFactory.getInstance().createStatus(
                        (Element) child);
                } else {
		    if (anyString != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("ArtifactResponseImpl."
				+ "parseElement: included more than one "
				+ "any element.");
                        }
                        throw new SAML2Exception(
                            SAML2SDKUtils.bundle.getString("moreElement"));
	
		    }
		    anyString = XMLUtils.print((Element) child);
                }
            }
        }

        super.validateData();
	isMutable = false;
    }

    /**
     * Constructor. Caller may need to call setters to populate the
     * object.
     */
    public ArtifactResponseImpl() {
        super(ARTIFACT_RESPONSE);
        isMutable = true;
    }

    /**
     * Constructor with <code>ArtifactResponse</code> in
     * <code>Element</code> format.
     *
     * @param element the Document Element
     * @throws SAML2Exception if there is an error.
     */
    public ArtifactResponseImpl(org.w3c.dom.Element element)
        throws SAML2Exception {
        super(ARTIFACT_RESPONSE);
        parseElement(element);
        if (isSigned) {
            signedXMLString = XMLUtils.print(element);
         }
    }

    /**
     * Constructor with <code>ArtifactResponse</code> in xml string 
     * format.
     */
    public ArtifactResponseImpl(String xmlString)
        throws SAML2Exception {
        super(ARTIFACT_RESPONSE);
        Document doc = XMLUtils.toDOMDocument(xmlString);
        if (doc == null) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseElement(doc.getDocumentElement());
        if (isSigned) {
            signedXMLString = xmlString;
         }
    }

    /**
     * Gets the <code>any</code> element of the response.
     *
     * @return <code>any</code> element in xml string format.
     * @see #setAny(String)
     */
    public String getAny() {
	return anyString;
    }

    /**
     * Sets the <code>any</code> element of the response.
     *
     * @param value new <code>any</code> element in xml string format.
     * @throws SAML2Exception if the object is immutable.
     * @see #getAny()
     */
    public void setAny(String value)
        throws SAML2Exception {
	if (isMutable) {
	    anyString = value;
	} else {
	    throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString("objectImmutable"));
	}
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        DocumentFragment fragment = super.toDocumentFragment(document, includeNSPrefix, declareNS);
        if (isSigned && signedXMLString != null) {
            return fragment;
        }

        Element rootElement = (Element) fragment.getFirstChild();

        if (isNotBlank(anyString)) {
            List<Node> parsed = SAML2Utils.parseSAMLFragment(anyString);
            for (Node node : parsed) {
                rootElement.appendChild(document.adoptNode(node));
            }
        }

        return fragment;
    }
}
