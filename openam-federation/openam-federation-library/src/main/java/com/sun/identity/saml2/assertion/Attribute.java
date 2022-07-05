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
 * $Id: Attribute.java,v 1.2 2008/06/25 05:47:40 qcheng Exp $
 *
 * Portions Copyrighted 2015-2021 ForgeRock AS.
 */

package com.sun.identity.saml2.assertion;

import java.util.List;
import java.util.Map;

import org.forgerock.openam.annotations.SupportedAll;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sun.identity.saml2.assertion.impl.AttributeImpl;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.XmlSerializable;
import com.sun.identity.saml2.key.EncryptionConfig;

/**
 * The <code>Attribute</code> element identifies an attribute by name and
 * optionally includes its value(s). It has the <code>AttributeType</code>
 * complex type.
 * <p>
 * <pre>
 * &lt;complexType name="AttributeType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}
 *         AttributeValue" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="FriendlyName"
 *       type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="Name" use="required"
 *       type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="NameFormat"
 *       type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@SupportedAll
@JsonDeserialize(as=AttributeImpl.class)
public interface Attribute extends XmlSerializable {

    /**
     * Makes the object immutable.
     */
    void makeImmutable();

    /**
     * Returns the mutability of the object.
     *
     * @return <code>true</code> if the object is mutable;
     *                <code>false</code> otherwise.
     */
    boolean isMutable();

    /**
     * Returns the <code>AttributeValue</code>(s) of the <code>Attribute</code>.
     *
     * @return List of xml String representing <code>AttributeValue</code>(s)
     *                 of the <code>Attribute</code>.
     * @see #setAttributeValue(List)
     */
    List getAttributeValue();

    /**
     * Sets the <code>AttributeValue</code>(s) of the <code>Attribute</code>.
     *
     * @param value List of xml String representing the new
     *                 <code>AttributeValue</code> element(s).
     * @throws SAML2Exception if the object is immutable.
     * @see #getAttributeValue()
     */
    void setAttributeValue(List value) throws SAML2Exception;

    /**
     * Returns the <code>AttributeValue</code>(s) of the <code>Attribute</code>.
     *
     * @return List of String representing the value of
     *                 <code>AttributeValue</code>(s).
     * @see #setAttributeValueString(List)
     */
    List<String> getAttributeValueString();

    /**
     * Sets the value of <code>AttributeValue</code> element(s).
     *
     * @param value List of String representing the value of the new
     *          <code>AttributeValue</code> element(s).
     * @throws SAML2Exception if the object is immutable.
     * @see #getAttributeValueString()
     */
    void setAttributeValueString(List value) throws SAML2Exception;

    /**
     * Returns the <code>Name</code> of the attribute.
     *
     * @return the <code>Name</code> of the attribute.
     * @see #setName(String)
     */
    String getName();

    /**
     * Sets the <code>Name</code> of the attribute.
     *
     * @param value new <code>Name</code> of the attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getName()
     */
    void setName(String value) throws SAML2Exception;

    /**
     * Returns the <code>NameFormat</code> of the attribute.
     *
     * @return the value of <code>NameFormat</code>.
     * @see #setNameFormat(String)
     */
    String getNameFormat();

    /**
     * Sets the <code>NameFormat</code> of the attribute.
     *
     * @param value new <code>NameFormat</code> of the attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getNameFormat()
     */
    void setNameFormat(String value) throws SAML2Exception;

    /**
     * Returns the <code>FriendlyName</code> of the attribute.
     *
     * @return the value of <code>FriendlyName</code> of the attribute.
     * @see #setFriendlyName(String)
     */
    String getFriendlyName();

    /**
     * Sets the <code>FriendlyName</code> of the attribute.
     *
     * @param value new <code>FriendlyName</code> of the attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getFriendlyName()
     */
    void setFriendlyName(String value) throws SAML2Exception;

    /**
     * Returns the <code>anyAttribute</code> of the attribute.
     *
     * @return A Map containing name/value pairs of <code>anyAttribute</code>.
     *                Both the name and value are String object types.
     * @see #setAnyAttribute(Map)
     */
    Map getAnyAttribute();

    /**
     * Sets the <code>anyAttribute</code> of the attribute.
     *
     * @param value Map of name/value pairs to be set. Both the name and value
     *                are String object types.
     * @throws SAML2Exception if the object is immutable.
     * @see #getAnyAttribute()
     */
    void setAnyAttribute(Map value) throws SAML2Exception;

    /**
     * Returns an <code>EncryptedAttribute</code> object.
     *
     * @param encryptionConfig The encryption config.
     * @param recipientEntityID Unique identifier of the recipient, it is used as the index to the cached secret key so
     * that the key can be reused for the same recipient; It can be null in which case the secret key will be generated
     * every time and will not be cached and reused. Note that the generation of a secret key is a relatively expensive
     * operation.
     * @return <code>EncryptedAttribute</code> object
     * @throws SAML2Exception if error occurs during the encryption process.
     */
    EncryptedAttribute encrypt(EncryptionConfig encryptionConfig, String recipientEntityID) throws SAML2Exception;
}

