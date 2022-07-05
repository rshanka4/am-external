/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: StatusMessageImpl.java,v 1.3 2008/06/25 05:48:13 qcheng Exp $
 *
 * Portions Copyrighted 2017-2021 ForgeRock AS.
 */
package com.sun.identity.xacml.context.impl;

import org.forgerock.openam.annotations.SupportedAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.xacml.common.XACMLConstants;
import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.common.XACMLSDKUtils;
import com.sun.identity.xacml.context.StatusMessage;

/**
 * The <code>StatusMessage</code> element is a container of 
 * one or more <code>StatusMessage</code>s issuded by authorization authority.
 */
@SupportedAll
public class StatusMessageImpl implements StatusMessage {

    private static final Logger logger = LoggerFactory.getLogger(StatusMessageImpl.class);
    private boolean mutable = true;
    private String value = null;

    /** 
     * Constructs a <code>StatusMessage</code> object
     */
    public StatusMessageImpl() throws XACMLException {
    }

    /** 
     * Constructs a <code>StatusMessage</code> object from an XML string
     *
     * @param xml string representing a <code>StatusMessage</code> object
     * @throws XACMLException If the XML string could not be processed.
     */
    public StatusMessageImpl(String xml) throws XACMLException {
        Document document = XMLUtils.toDOMDocument(xml);
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            processElement(rootElement);
            makeImmutable();
        } else {
            logger.error(
                "StatusMessageImpl.processElement(): invalid XML input");
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "errorObtainingElement"));
        }
    }

    /** 
     * Constructs a <code>StatusMessage</code> object from an XML DOM element
     *
     * @param element XML DOM element representing a <code>StatusMessage</code> 
     * object
     *
     * @throws XACMLException If the DOM element could not be processed.
     */
    public StatusMessageImpl(Element element) throws XACMLException {
        processElement(element);
        makeImmutable();
    }

    /**
     * Returns the <code>value</code> of this object
     *
     * @return the <code>value</code> of this object
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the <code>value</code> of this object
     *
     * @exception XACMLException if the object is immutable
     */
    public void setValue(String value) throws XACMLException {
        if (!mutable) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("objectImmutable"));
        }

        if (value == null) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("null_not_valid")); //i18n
        }

        if (!XACMLSDKUtils.isValidStatusMessage(value)) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("invalid_value")); //i18n
        }
        this.value = value;
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        DocumentFragment fragment = document.createDocumentFragment();
        Element statusMessageElement = XMLUtils.createRootElement(document, XACMLConstants.CONTEXT_NS_PREFIX,
                XACMLConstants.CONTEXT_NS_URI, XACMLConstants.STATUS_MESSAGE, includeNSPrefix, declareNS);
        fragment.appendChild(statusMessageElement);
        statusMessageElement.setTextContent(value);
        return fragment;
    }

    /**
    * Checks if the object is mutable
    *
    * @return <code>true</code> if the object is mutable,
    *         <code>false</code> otherwise
    */
    public boolean isMutable() {
        return mutable;
    }
    
   /**
    * Makes the object immutable
    */
    public void makeImmutable() {
        mutable = false;
    }

    private void processElement(Element element) throws XACMLException {
        if (element == null) {
            logger.error(
                "StatusMessageImpl.processElement(): invalid root element");
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_element"));
        }
        String elemName = element.getLocalName();
        if (elemName == null) {
            logger.error(
                "StatusMessageImpl.processElement(): local name missing");
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "missing_local_name"));
        }

        if (!elemName.equals(XACMLConstants.STATUS_MESSAGE)) {
            logger.error(
                    "StatusMessageImpl.processElement(): invalid local name " 
                    + elemName);
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                    "invalid_local_name"));
        }
        String elementValue = element.getTextContent();
        if (elementValue == null) {
            throw new XACMLException(
                    XACMLSDKUtils.xacmlResourceBundle.getString("null_not_valid"));
        }
        if (!XACMLSDKUtils.isValidStatusMessage(elementValue.trim())) {
            throw new XACMLException(
                    XACMLSDKUtils.xacmlResourceBundle.getString("invalid_value"));
        } else {
            this.value = elementValue;
        }
    }

}
