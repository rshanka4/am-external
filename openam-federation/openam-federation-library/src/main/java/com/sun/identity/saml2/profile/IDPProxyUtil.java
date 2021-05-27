/*
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
 * $Id: IDPProxyUtil.java,v 1.18 2009/11/20 21:41:16 exu Exp $
 *
 * Portions Copyrighted 2010-2020 ForgeRock AS.
 */

package com.sun.identity.saml2.profile;

import static org.forgerock.http.util.Uris.urlEncodeQueryParameterNameOrValue;
import static org.forgerock.openam.saml2.Saml2EntityRole.SP;
import static org.forgerock.openam.utils.Time.currentTimeMillis;
import static org.forgerock.openam.utils.Time.newDate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.federation.saml2.SAML2TokenRepositoryException;
import org.forgerock.openam.saml2.audit.SAML2EventLogger;
import org.forgerock.openam.saml2.plugins.Saml2CredentialResolver;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.EncryptedAssertion;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2FailoverUtils;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.common.SOAPCommunicator;
import com.sun.identity.saml2.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.jaxb.metadata.EndpointType;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorType;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorType;
import com.sun.identity.saml2.jaxb.metadata.SSODescriptorType;
import com.sun.identity.saml2.logging.LogUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.plugins.SAML2IDPFinder;
import com.sun.identity.saml2.plugins.SAML2ServiceProviderAdapter;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.IDPEntry;
import com.sun.identity.saml2.protocol.IDPList;
import com.sun.identity.saml2.protocol.LogoutRequest;
import com.sun.identity.saml2.protocol.LogoutResponse;
import com.sun.identity.saml2.protocol.NameIDPolicy;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.RequesterID;
import com.sun.identity.saml2.protocol.Response;
import com.sun.identity.saml2.protocol.Scoping;
import com.sun.identity.saml2.protocol.impl.RequesterIDImpl;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * Utility class to be used for IDP Proxying.
 */
public class IDPProxyUtil {

    // IDP proxy finder 
    // private static SAML2IDPFinder proxyFinder = null;

    private static final Logger logger = LoggerFactory.getLogger(IDPProxyUtil.class);

    private static SAML2MetaManager sm = null;

    private static Logger debug = LoggerFactory.getLogger(IDPProxyUtil.class);
    private static SessionProvider sessionProvider = null;


    static {
        try {
             sm = new SAML2MetaManager();
             sessionProvider = SessionManager.getProvider();
         } catch (Exception ex) {
             logger.error("IDPSSOFederate:Static Init Failed", ex);
         }
    }

    private IDPProxyUtil() {
    }

    /**
     * Gets the preferred IDP Id to be proxied. This method makes use of an
     * SPI to determine the preferred IDP.
     * @param authnRequest original Authn Request.
     * @param hostedEntityId hosted provider ID 
     * @param realm Realm 
     * @param request HttpServletRequest
     * @param response HttpServletResponse 
     * @exception SAML2Exception for any SAML2 failure.
     * @return String Provider id of the preferred IDP to be proxied.
     */
    public static String getPreferredIDP(
        AuthnRequest authnRequest,
        String hostedEntityId,
        String realm,
        HttpServletRequest request,
        HttpServletResponse response)
        throws SAML2Exception
    {
        SAML2IDPFinder proxyFinder = getIDPProxyFinder(realm, hostedEntityId);
        List idpProviderIDs = proxyFinder.getPreferredIDP(
            authnRequest, hostedEntityId, realm,
            request, response);
        if ((idpProviderIDs == null) || idpProviderIDs.isEmpty()) {
            return null;
        }

        return (String)idpProviderIDs.get(0);
    }

    /**
     * Sends a new AuthnRequest to the authenticating provider. 
     * @param authnRequest original AuthnRequest sent by the service provider.
     * @param preferredIDP IDP to be proxied. 
     * @param hostedEntityId hosted provider ID
     * @param request HttpServletRequest 
     * @param response HttpServletResponse
     * @param realm Realm
     * @param relayState the Relay State 
     * @param originalBinding The binding used to send the original AuthnRequest.
     * @exception SAML2Exception for any SAML2 failure.
     * @exception IOException if there is a failure in redirection.
     */
    public static void sendProxyAuthnRequest(
            AuthnRequest authnRequest,
            String preferredIDP,
            String hostedEntityId,
            HttpServletRequest request,
            HttpServletResponse response,
            String realm,
            String relayState,
            String originalBinding)
            throws SAML2Exception, IOException {
        String classMethod = "IDPProxyUtil.sendProxyAuthnRequest: ";
        String destination = null;
        SPSSODescriptorType localDescriptor = null;
        IDPSSODescriptorType idpDescriptor = null;
        String binding;
        try {
            idpDescriptor = IDPSSOUtil.metaManager.getIDPSSODescriptor(realm, preferredIDP);
            List<EndpointType> ssoServiceList = idpDescriptor.getSingleSignOnService();
            EndpointType endpoint = getMatchingSSOEndpoint(ssoServiceList, originalBinding);
            if (endpoint == null) {
                logger.error(classMethod + "Single Sign-on service is not found for the proxying IDP.");
                throw new SAML2Exception(SAML2Utils.bundle.getString("ssoServiceNotFoundIDPProxy"));
            }
            binding = endpoint.getBinding();
            destination = endpoint.getLocation();

            localDescriptor = IDPSSOUtil.metaManager.getSPSSODescriptor(realm, hostedEntityId);
        } catch (SAML2MetaException e) {
            logger.error(classMethod, e);
            throw new SAML2Exception(e.getMessage());
        }

        AuthnRequest newAuthnRequest = getNewAuthnRequest(hostedEntityId, destination, realm, authnRequest);
        // invoke SP Adapter class if registered
        SAML2ServiceProviderAdapter spAdapter = SAML2Utils.getSPAdapterClass(hostedEntityId, realm);
        if (spAdapter != null) {
            spAdapter.preSingleSignOnRequest(hostedEntityId, preferredIDP, realm, request, response, newAuthnRequest);
        }
        if (logger.isDebugEnabled()) {
            logger.debug(classMethod + "New Authentication request:" + newAuthnRequest.toXMLString());
        }
        String requestID = newAuthnRequest.getID();

        // save the AuthnRequest in the IDPCache so that it can be
        // retrieved later when the user successfully authenticates
        IDPCache.authnRequestCache.put(requestID, newAuthnRequest);

        // save the original AuthnRequest
        IDPCache.proxySPAuthnReqCache.put(requestID, authnRequest);


        boolean signingNeeded = idpDescriptor.isWantAuthnRequestsSigned() || localDescriptor.isAuthnRequestsSigned();

        // check if relayState is present and get the unique
        // id which will be appended to the SSO URL before
        // redirecting
        String relayStateID = null;
        if (relayState != null && relayState.length()> 0) {
            IDPCache.relayStateCache.put(requestID, relayState);
            relayStateID = SPSSOFederate.getRelayStateID(relayState,
                    authnRequest.getID());
        }

        if (binding.equals(SAML2Constants.HTTP_POST)) {
            if (signingNeeded) {
                SPSSOFederate.signAuthnRequest(realm, preferredIDP, hostedEntityId, newAuthnRequest);
            }
            String authXMLString = newAuthnRequest.toXMLString(true,true);

            String encodedReqMsg = SAML2Utils.encodeForPOST(authXMLString);
            SAML2Utils.postToTarget(request, response, "SAMLRequest",
                    encodedReqMsg, "RelayState", relayStateID, destination);
        } else {

            String authReqXMLString = newAuthnRequest.toXMLString(true,true);

            if (logger.isDebugEnabled()) {
                logger.debug(classMethod + " AuthnRequest: " +
                        authReqXMLString);
            }

            String encodedXML = SAML2Utils.encodeForRedirect(authReqXMLString);
            StringBuffer queryString =
                    new StringBuffer().append(SAML2Constants.SAML_REQUEST)
                            .append(SAML2Constants.EQUAL)
                            .append(encodedXML);
            //TODO:  should it be newAuthnRequest???
            if (relayStateID != null && relayStateID.length() > 0) {
                queryString.append("&").append(SAML2Constants.RELAY_STATE)
                        .append("=")
                        .append(urlEncodeQueryParameterNameOrValue(relayStateID));
            }

            StringBuffer redirectURL =
                    new StringBuffer().append(destination)
                            .append(destination.contains("?") ? "&" : "?");

            if (signingNeeded) {
                String signedQueryStr = SPSSOFederate.signQueryString(queryString.toString(), hostedEntityId, realm,
                    preferredIDP);
                redirectURL.append(signedQueryStr);
            } else {
                redirectURL.append(queryString);
            }
            response.sendRedirect(redirectURL.toString());
        }

        String[] data = { destination };
        LogUtil.access(Level.INFO, LogUtil.REDIRECT_TO_SP,data, null);
        AuthnRequestInfo reqInfo = new AuthnRequestInfo(newAuthnRequest, relayState);
        synchronized(SPCache.requestHash) {
            SPCache.requestHash.put(requestID, reqInfo);
        }

        try {
            // sessionExpireTime is counted in seconds
            long sessionExpireTime = currentTimeMillis() / 1000 + SPCache.interval;
            SAML2FailoverUtils.saveSAML2TokenWithoutSecondaryKey(requestID, new AuthnRequestInfoCopy(reqInfo),
                    sessionExpireTime);
            if (logger.isDebugEnabled()) {
                logger.debug(classMethod + " SAVE AuthnRequestInfoCopy for requestID " + requestID);
            }
        } catch(SAML2TokenRepositoryException se) {
            logger.error(classMethod + " SAVE AuthnRequestInfoCopy for requestID "
                    + requestID + ", failed!", se);
        }
    }

    private static EndpointType getMatchingSSOEndpoint(List<EndpointType> endpoints,
            String preferredBinding) {
        EndpointType preferredEndpoint = null;
        boolean isFirst = true;
        for (EndpointType endpoint : endpoints) {
            if (isFirst) {
                //If there is no match, we should use the first endpoint in the list
                preferredEndpoint = endpoint;
                isFirst = false;
            }
            if (preferredBinding.equals(endpoint.getBinding())) {
                preferredEndpoint = endpoint;
                break;
            }
        }

        return preferredEndpoint;
    }

    /**
     * Constructs new authentication request by using the original request
     * that is sent by the service provider to the proxying IDP.
     * @param hostedEntityId hosted provider ID
     * @param destination The destination where the new AuthnRequest will be sent to.
     * @param realm Realm
     * @param origRequest Original Authn Request
     * @return AuthnRequest new authn request.
     * @exception SAML2Exception for failure in creating new authn request.
     * @return AuthnRequest object
     */
    private static AuthnRequest getNewAuthnRequest(String hostedEntityId, String destination, String realm,
            AuthnRequest origRequest) throws SAML2Exception {
        String classMethod = "IDPProxyUtil.getNewAuthnRequest: ";
        // New Authentication request should only be a single sign-on request.
        try {
            AuthnRequest newRequest = ProtocolFactory.getInstance().createAuthnRequest();
            String requestID = SAML2Utils.generateID();
            if (requestID == null || requestID.isEmpty()) {
                throw new SAML2Exception(SAML2Utils.bundle.getString("cannotGenerateID"));
            }
            newRequest.setID(requestID);

            SPSSODescriptorType localDescriptor = IDPSSOUtil.metaManager.getSPSSODescriptor(realm, hostedEntityId);

            newRequest.setDestination(XMLUtils.escapeSpecialCharacters(destination));
            newRequest.setConsent(origRequest.getConsent());
            newRequest.setIsPassive(origRequest.isPassive());
            newRequest.setForceAuthn(origRequest.isForceAuthn());
            newRequest.setAttributeConsumingServiceIndex(origRequest.
                getAttributeConsumingServiceIndex());
            newRequest.setAssertionConsumerServiceIndex(origRequest.
                getAssertionConsumerServiceIndex());
            String protocolBinding = origRequest.getProtocolBinding();
            newRequest.setProtocolBinding(protocolBinding);

            OrderedSet acsSet = SPSSOFederate.getACSUrl(
                localDescriptor,protocolBinding);
            String acsURL = (String) acsSet.get(0);

            newRequest.setAssertionConsumerServiceURL(acsURL);
            Issuer issuer = AssertionFactory.getInstance().createIssuer();
            issuer.setValue(hostedEntityId);

            newRequest.setIssuer(issuer);
            NameIDPolicy origNameIDPolicy = origRequest.getNameIDPolicy();
            if (origNameIDPolicy != null) {
                NameIDPolicy newNameIDPolicy = ProtocolFactory.getInstance().createNameIDPolicy();
                newNameIDPolicy.setFormat(origNameIDPolicy.getFormat());
                newNameIDPolicy.setSPNameQualifier(hostedEntityId);
                newNameIDPolicy.setAllowCreate(origNameIDPolicy.isAllowCreate());

                newRequest.setNameIDPolicy(newNameIDPolicy);
            }
            newRequest.setRequestedAuthnContext(origRequest.
                getRequestedAuthnContext());
            newRequest.setExtensions(origRequest.getExtensions());
            newRequest.setIssueInstant(newDate());
            newRequest.setVersion(SAML2Constants.VERSION_2_0);
            Scoping scoping = origRequest.getScoping();
            if (scoping != null) {
                Scoping newScoping = ProtocolFactory.getInstance().
                    createScoping();
                Integer proxyCountInt = scoping.getProxyCount();
                int proxyCount = 1;
                if (proxyCountInt != null) {
                    proxyCount = scoping.getProxyCount().intValue();
                    newScoping.setProxyCount(new Integer(proxyCount-1));
                }
                newScoping.setIDPList(scoping.getIDPList());

                //Set the requesterIDs
                newScoping.setRequesterIDs(scoping.getRequesterIDs());
                addRequesterIDToScope(newScoping, origRequest.getIssuer().getValue());

                newRequest.setScoping(newScoping);
            } else {
                //handling the alwaysIdpProxy case -> the incoming request
                //did not contained a Scoping field
                SPSSOConfigElement spConfig = getSPSSOConfigByAuthnRequest(realm, origRequest);
                Map<String, List<String>> spConfigAttrMap = SAML2MetaUtils.getAttributes(spConfig);
                scoping = ProtocolFactory.getInstance().createScoping();
                String proxyCountParam = SPSSOFederate.getParameter(spConfigAttrMap,
                        SAML2Constants.IDP_PROXY_COUNT);
                if (proxyCountParam != null && (!proxyCountParam.equals(""))) {
                    int proxyCount = Integer.valueOf(proxyCountParam);
                    if (proxyCount <= 0) {
                        scoping.setProxyCount(0);
                    } else {
                        //since this is a remote SP configuration, we should
                        //decrement the proxycount by one
                        scoping.setProxyCount(proxyCount - 1);
                    }
                }

                //Set the requesterIDs
                addRequesterIDToScope(scoping, origRequest.getIssuer().getValue());

                List<String> proxyIdPs = spConfigAttrMap.get(
                        SAML2Constants.IDP_PROXY_LIST);
                if (proxyIdPs != null && !proxyIdPs.isEmpty()) {
                    List<IDPEntry> list = new ArrayList<IDPEntry>();
                    for (String proxyIdP : proxyIdPs) {
                        IDPEntry entry = ProtocolFactory.getInstance().
                                createIDPEntry();
                        entry.setProviderID(proxyIdP);
                        list.add(entry);
                    }
                    IDPList idpList = ProtocolFactory.getInstance().
                            createIDPList();
                    idpList.setIDPEntries(list);
                    scoping.setIDPList(idpList);
                    newRequest.setScoping(scoping);
                }
            }
            return newRequest;
        } catch (Exception ex) {
            logger.error(classMethod +
                "Error in creating new authn request.", ex);
            throw new SAML2Exception(ex);
        }
    }

    public static void addRequesterIDToScope(Scoping scoping, String requesterId) throws SAML2Exception {
        List<RequesterID> requesterIDs = new ArrayList<>();
        if (scoping.getRequesterIDs() != null) {
            requesterIDs.addAll(scoping.getRequesterIDs());
        }

        RequesterID requesterID = new RequesterIDImpl();
        requesterID.setValue(requesterId);
        requesterIDs.add(requesterID);

        scoping.setRequesterIDs(requesterIDs);
    }

    /**
     * Checks if the identity provider is configured for proxying the
     * authentication requests for a requesting service provider.
     * @param authnRequest Authentication Request.
     * @param realm Realm
     * @return <code>true</code> if the IDP is configured for proxying.
     * @exception SAML2Exception for any failure.
     */
    public static boolean isIDPProxyEnabled(AuthnRequest authnRequest,
        String realm)
        throws SAML2Exception
    {
        SPSSOConfigElement spConfig;
        Map spConfigAttrsMap = null;
        Scoping scoping = authnRequest.getScoping();

        if (scoping == null) {
            //let's check if always IdP proxy and IdP Proxy itself is enabled
            spConfig = getSPSSOConfigByAuthnRequest(realm, authnRequest);
            if (spConfig != null) {
                spConfigAttrsMap = SAML2MetaUtils.getAttributes(spConfig);
                Boolean alwaysEnabled = SPSSOFederate.getAttrValueFromMap(
                        spConfigAttrsMap, SAML2Constants.ALWAYS_IDP_PROXY);
                Boolean proxyEnabled = SPSSOFederate.getAttrValueFromMap(
                        spConfigAttrsMap, SAML2Constants.ENABLE_IDP_PROXY);
                if (alwaysEnabled != null && alwaysEnabled
                        && proxyEnabled != null && proxyEnabled) {
                    return true;
                }
            }
            return false;
        }

        Integer proxyCountInt = scoping.getProxyCount();
        int proxyCount = 0;
        if (proxyCountInt == null) {
            //Proxy count missing, IDP Proxy allowed
            proxyCount = 1;
        } else {
            proxyCount = proxyCountInt.intValue();
        }

        if (proxyCount <= 0) {
            return false;
        }
        spConfig =
            IDPSSOUtil.metaManager.getSPSSOConfig(realm,
            authnRequest.getIssuer().getValue());
        if (spConfig != null) {
            spConfigAttrsMap = SAML2MetaUtils.getAttributes(spConfig);
        }
        Boolean enabledString = SPSSOFederate.getAttrValueFromMap(
            spConfigAttrsMap, SAML2Constants.ENABLE_IDP_PROXY);
        if (enabledString == null) {
            return false;
        }
        return (enabledString.booleanValue());
    }

    /**
     * Checks if the proxying is enabled. It will be checking if the proxy
     * service provider descriptor is set in the session manager for the
     * specific request ID.
     * @param requestID authentication request id which is created by the
     *     proxying IDP to the authenticating IDP.
     * @return true if the proxying is enabled.
     */
    public static boolean isIDPProxyEnabled(String requestID) {
        return IDPCache.proxySPAuthnReqCache.containsKey(requestID);
    }

    /**
     * Sends the proxy authentication response to the proxying service
     * provider which has originally requested for the authentication.
     * @param request HttpServletRequest 
     * @param response HttpServletResponse
     * @param out the print writer for writing out presentation
     * @param requestID request ID 
     * @param idpMetaAlias meta Alias 
     * @param newSession Session object
     * @param nameIDFormat name identifier format
     * @param saml2Auditor a <code>SAML2EventLogger</code> auditor object to hook into
     *                tracking information for the saml request
     * @throws SAML2Exception for any SAML2 failure.
     */
    private static void sendProxyResponse(
        HttpServletRequest request,
        HttpServletResponse response,
        PrintWriter out,
        String requestID,
        String idpMetaAlias,
        Object newSession,
        String nameIDFormat,
        SAML2EventLogger saml2Auditor)
        throws SAML2Exception
    {
        String classMethod = "IDPProxyUtil.sendProxyResponse: ";
        AuthnRequest origRequest = null;
        origRequest = (AuthnRequest)
            IDPCache.proxySPAuthnReqCache.get(requestID);
        if (logger.isDebugEnabled()) {
            try {
                logger.debug(classMethod +
                    origRequest.toXMLString());
            } catch (Exception ex) {
                logger.error(classMethod +
                    "toString(): Failed.", ex);
            }
        }
        IDPCache.proxySPAuthnReqCache.remove(requestID);
        String proxySPEntityId = origRequest.getIssuer().getValue();
        if (logger.isDebugEnabled()) {
            logger.debug( classMethod
                + ":Original requesting service provider id:"
                + proxySPEntityId);
        }
        // Save the SP provider id based on the token id
        IDPCache.spSessionPartnerBySessionID.put(sessionProvider.getSessionID(newSession), proxySPEntityId);

        //TODO: set AuthnContext
        /*AuthnContext authnContextStm;
        if (authnContextStmt != null) {
            String authnContext = authnContextStmt.getAuthnContextClassRef();
            session.setAuthnContext(authnContext);
        }*/

        String relayState = (String) IDPCache.relayStateCache.remove(requestID);
        if (relayState == null) {
            relayState = getSavedRelayStateFromStore(requestID);
        }
        IDPSSOUtil.doSSOFederate( request,
                                  response,
                                  out,
                                  origRequest,
                                  origRequest.getIssuer().getValue(),
                                  idpMetaAlias,
                                  nameIDFormat,
                                  relayState,
                                  newSession,
                                  saml2Auditor);
    }

    private static String getSavedRelayStateFromStore(String requestID) {
        String relayState = null;
        AuthnRequestInfo authnRequestInfo = (AuthnRequestInfo) SPCache.requestHash.get(requestID);
        if (authnRequestInfo != null) {
            relayState = authnRequestInfo.getRelayState();
        } else {
            try {
                AuthnRequestInfoCopy reqInfoCopy = (AuthnRequestInfoCopy) SAML2FailoverUtils.retrieveSAML2Token(requestID);
                if (reqInfoCopy != null) {
                    authnRequestInfo = reqInfoCopy.getAuthnRequestInfo();
                    relayState = authnRequestInfo.getRelayState();
                }
            } catch (SAML2Exception | SAML2TokenRepositoryException se) {
                logger.error("Unable to retrieve AuthnRequestInfoCopy from SAML2 repository for {}", requestID, se);
            }
        }
        return relayState;
    }

    /**
     * Sends back response with firstlevel and secondlevel status code if available for the original AuthnRequest.
     *
     * @param request The request.
     * @param response The response.
     * @param out The print writer for writing out presentation.
     * @param requestID The requestID of the proxied AuthnRequest.
     * @param idpMetaAlias The IdP's metaAlias.
     * @param hostEntityID The IdP's entity ID.
     * @param realm The realm where the IdP belongs to.
     * @param firstlevelStatusCodeValue First-level status code value passed.
     * @param secondlevelStatusCodeValue Second-level status code value passed.
     * @throws SAML2Exception If there was an error while sending the response with second-level status-code.
     */
    public static void sendResponseWithStatus(HttpServletRequest request, HttpServletResponse response,
                                                          PrintWriter out, String requestID, String idpMetaAlias,
                                                          String hostEntityID, String realm,
                                                          String firstlevelStatusCodeValue,
                                                          String secondlevelStatusCodeValue)
            throws SAML2Exception {

        AuthnRequest origRequest = (AuthnRequest) IDPCache.proxySPAuthnReqCache.remove(requestID);
        String relayState = (String) IDPCache.relayStateCache.remove(requestID);
        if (relayState == null) {
            relayState = getSavedRelayStateFromStore(requestID);
        }

        IDPSSOUtil.sendResponseWithStatus(request, response, out, idpMetaAlias, hostEntityID, realm, origRequest,
                relayState, origRequest.getIssuer().getValue(), firstlevelStatusCodeValue, secondlevelStatusCodeValue);
    }

    /**
     * Generates the AuthnResponse by the IDP Proxy and send to the service provider.
     *
     * @param request HttpServletRequest The HTTP request.
     * @param response HttpServletResponse The HTTP response.
     * @param out The print writer for writing out presentation.
     * @param metaAlias The meta alias.
     * @param respInfo ResponseInfo object.
     * @param newSession Session object.
     * @param auditor a <code>SAML2EventLogger</code> auditor
     * @throws SAML2Exception for any SAML2 failure.
     */
    public static void generateProxyResponse(HttpServletRequest request, HttpServletResponse response, PrintWriter out,
            String metaAlias, ResponseInfo respInfo, Object newSession, SAML2EventLogger auditor) throws SAML2Exception {
        Response saml2Resp = respInfo.getResponse();
        String requestID = saml2Resp.getInResponseTo();
        String nameidFormat = getNameIDFormat(saml2Resp, metaAlias);
        if (nameidFormat != null && logger.isDebugEnabled()) {
            logger.debug("NAME ID Format= " + nameidFormat);
        }

        // Save the SAML response received from the IdP in the request object, so that we can access the original
        // assertion when generating the new one.
        request.setAttribute(SAML2Constants.SAML_PROXY_IDP_RESPONSE_KEY, saml2Resp);
        sendProxyResponse(request, response, out, requestID, metaAlias, newSession, nameidFormat, auditor);
    }

    private static String getNameIDFormat(Response res, String metaAlias) {

        if (res == null) {
            return null;
        }

        Assertion assertion = null;
        List<Assertion> assertions = res.getAssertion();

        if(CollectionUtils.isEmpty(assertions)){
            // Check for Encrypted Assertions
            List<EncryptedAssertion> encryptedAssertions = res.getEncryptedAssertion();
            if(CollectionUtils.isEmpty(encryptedAssertions)){
                return null;
            } else {
                String realm = SAML2Utils.getRealm(SAML2MetaUtils.getRealmByMetaAlias(metaAlias));
                try {
                    String hostEntityId = sm.getEntityByMetaAlias(metaAlias);
                    Set<PrivateKey> decryptionKeys = InjectorHolder.getInstance(Saml2CredentialResolver.class)
                            .resolveValidDecryptionCredentials(realm, hostEntityId, SP);
                    assertion = encryptedAssertions.get(0).decrypt(decryptionKeys);
                } catch (SAML2Exception ex) {
                    logger.error("getNameIDFormat failed decrypting EncryptedAssertion", ex);
                    return null;
                }
            }
        } else {
            assertion = assertions.get(0);
        }

        Subject subject = assertion.getSubject();
        if (subject == null) {
            return null;
        }
        NameID nameID = subject.getNameID();
        if (nameID == null) {
            return null;
        }
        String format = nameID.getFormat();
        return format;
    }

    /**
     * Initiates the Single logout request by the IDP Proxy to the 
     * authenticating identity provider. 
     * @param request HttpServletRequest 
     * @param response HttpServletResponse
     * @param out The print writer for writing out presentation.
     * @param partner Authenticating identity provider 
     * @param spMetaAlias IDP proxy's meta alias acting as SP
     * @param realm Realm
     */
    public static void initiateSPLogoutRequest(
        HttpServletRequest request,
        HttpServletResponse response,
        PrintWriter out,
        String partner,
        String spMetaAlias,
        String realm,
        LogoutRequest logoutReq,
        SOAPMessage msg,
        IDPSession idpSession,
        String binding,
        String relayState
        )
    {
        Object ssoToken = idpSession.getSession();

        try {
            if (ssoToken == null) {
                SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                    "nullSSOToken",
                    SAML2Utils.bundle.getString("nullSSOToken"));
                return;
            }
            String[] values = SessionManager.getProvider().
            getProperty(ssoToken, SAML2Constants.SP_METAALIAS);
            String metaAlias = null;
            if (values != null && values.length > 0) {
                metaAlias = values[0];
            }
            if (metaAlias == null) {
                metaAlias = spMetaAlias;
            }
            HashMap paramsMap = new HashMap();
            paramsMap.put("spMetaAlias", metaAlias);
            paramsMap.put("idpEntityID", partner);
            paramsMap.put(SAML2Constants.ROLE, SAML2Constants.SP_ROLE);
            EndpointType endpoint = getSLOElement(realm, partner, binding, SAML2Constants.IDP_ROLE);
            binding = endpoint.getBinding();
            paramsMap.put("Destination", endpoint.getLocation());
            paramsMap.put(SAML2Constants.BINDING, binding);
            paramsMap.put("Consent", request.getParameter("Consent"));
            paramsMap.put("Extension", request.getParameter("Extension"));
            if (relayState != null) {
                paramsMap.put(SAML2Constants.RELAY_STATE, relayState);
            }
            idpSession.removeSessionPartner(partner);
            String sessionIndex = IDPSSOUtil.getSessionIndex(idpSession.getSession());
            IDPSSOUtil.saveIdPSessionToTokenRepository(sessionIndex, sessionProvider, idpSession,
                idpSession.getSession());
            SPSingleLogout.initiateLogoutRequest(request,response, out,
                binding, paramsMap, logoutReq, msg, ssoToken, null);
        } catch (SAML2Exception sse) {
            logger.error("Error sending Logout Request " , sse);
            try {
                SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                    "LogoutRequestCreationError",
                    SAML2Utils.bundle.getString(
                    "LogoutRequestCreationError"));
            } catch(Exception se) {
                logger.error("IDPProxyUtil." +
                     "initiateSPLogoutRequest: ", se);
            }
            return ;
        } catch (Exception e) {
            logger.error("Error initializing Request ",e);
            try {
                SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                    "LogoutRequestCreationError",
                    SAML2Utils.bundle.getString(
                    "LogoutRequestCreationError"));
            } catch(Exception mme) {
                logger.error("IDPProxyUtil." +
                     "initiateSPLogoutRequest: ", mme);
            }
            return;
        }
    }

    /**
     * Gets the SLO response service location of the authenticating 
     * identity provider
     * @param realm Realm
     * @param idpEntityID authenticating identity provider. 
     * @return location URL of the SLO response service, return null 
     * if not found.
     */
    public static String getLocation (String realm, String idpEntityID,
        String binding)
    {
        try {
            String location = null;
            // get IDPSSODescriptor
            IDPSSODescriptorType idpsso =
                sm.getIDPSSODescriptor(realm,idpEntityID);
            if (idpsso == null) {
                String[] data = {idpEntityID};
                LogUtil.error(Level.INFO,LogUtil.IDP_METADATA_ERROR,data,
                    null);
                throw new SAML2Exception(
                    SAML2Utils.bundle.getString("metaDataError"));
            }
            List slosList = idpsso.getSingleLogoutService();
            if (slosList == null) {
                String[] data = {idpEntityID};
                LogUtil.error(Level.INFO,LogUtil.SLO_NOT_FOUND,data,
                    null);
                throw new SAML2Exception(
                    SAML2Utils.bundle.getString("sloServiceListNotfound"));
            }

            location = LogoutUtil.getSLOServiceLocation(slosList,binding);
            if (logger.isDebugEnabled() && (location != null)
                && (!location.equals(""))) {
                logger.debug("Location URL: " +
                    location);
            }
            return location;
        } catch (SAML2Exception se) {
            return null;
        }
    }

    /**
     * Gets the SLO response service element of the Entity 
     * @param realm Realm
     * @param entityID Authenticating Identity Provider. 
     * @param binding Binding type. 
     * @param role Identity Provider role. 
     * @return SingleLogoutServiceElement of the SLO response service 
     * throws SAML2Exception for any SAML2 failure.
     */
    public static EndpointType getSLOElement(String realm, String entityID, String binding, String role)
        throws SAML2Exception
    {

        String classMethod = "IDPProxyUtil.getSLOElement:";
        SSODescriptorType descriptor;

        if (SAML2Constants.SP_ROLE.equals(role)) {
            descriptor = sm.getSPSSODescriptor(realm, entityID);
        } else if (SAML2Constants.IDP_ROLE.equals(role)) {
            descriptor = sm.getIDPSSODescriptor(realm, entityID);
        } else {
            logger.error("{} Requested role type for entityID {} is invalid.", classMethod, entityID);
            throw new SAML2Exception(SAML2Utils.bundle.getString("metaDataError"));
        }

        if (descriptor == null) {
            logger.error("{} Unable to retrieve metadata in realm {} with entityID {} and role {}",
                classMethod, realm, entityID, role);
            throw new SAML2Exception(SAML2Utils.bundle.getString("metaDataError"));
        }

        List<EndpointType> sloEndpoints = descriptor.getSingleLogoutService();

        if (sloEndpoints == null) {
            logger.error("{} SLO List from entityID {} is NULL.", classMethod, entityID);
            throw new SAML2Exception( SAML2Utils.bundle.getString("sloServiceListNotfound"));
        }
        EndpointType sloeElement = LogoutUtil.getMostAppropriateSLOServiceLocation(sloEndpoints, binding);
        if (sloeElement == null) {
            logger.error("{} SingleLogoutServiceElement from entityID {} is NULL.", classMethod, entityID);
            throw new SAML2Exception(SAML2Utils.bundle.getString("sloServiceNotfound"));
        }
        if (StringUtils.isEmpty(sloeElement.getLocation())) {
            logger.error("{} SLO Element from entityID {} has NULL Location.", classMethod, entityID);
            throw new SAML2Exception(SAML2Utils.bundle.getString("sloServiceNotfound"));
        }
        return sloeElement;
    }

    /**
     * Obtain the session partner list from the supplied IDPSession
     * @param request The http servlet request received.
     * @return The list of session partners
     */
    public static List getSessionPartners(HttpServletRequest request)
    {
        try {
            Object tmpsession = sessionProvider.getSession(request);
            String tokenID = sessionProvider.getSessionID(tmpsession);
            IDPSession idpSession = null;
            if (tokenID != null && !tokenID.equals("")) {
                idpSession = IDPCache.idpSessionsBySessionID.get(tokenID);
            }
            List partners= null;
            if (idpSession != null) {
                partners = idpSession.getSessionPartners();
            }

            if (logger.isDebugEnabled()) {
                if (partners != null &&  !partners.isEmpty()) {
                    Iterator iter = partners.iterator();
                    while(iter.hasNext()) {
                        SAML2SessionPartner partner = (SAML2SessionPartner)iter.next();
                        if (logger.isDebugEnabled()) {
                            logger.debug("SESSION PARTNER's Provider ID: {}", partner.getPartner());
                        }
                    }
                }
            }
            return partners;
        } catch (SessionException se) {
            return null;
        }
    }

    public static void sendProxyLogoutRequest(
        HttpServletRequest request,
        HttpServletResponse response,
        PrintWriter out,
        LogoutRequest logoutReq,
        List partners,
        String binding,
        String relayState)
    {
        try {
            Object tmpsession = sessionProvider.getSession(request);
            String tokenID = sessionProvider.getSessionID(tmpsession);
            IDPSession idpSession = null;
            if (tokenID != null && !tokenID.equals("")) {
                idpSession = IDPCache.idpSessionsBySessionID.get(tokenID);
            }

            Iterator iter = partners.iterator();
            SAML2SessionPartner partner =
                (SAML2SessionPartner)iter.next();
            if (logger.isDebugEnabled()) {
                logger.debug(
                    "CURRENT PARTNER's provider ID: " +
                    partner.getPartner());
                logger.debug("Starting IDP proxy logout.");
            }

            String metaAlias =
                SAML2MetaUtils.getMetaAliasByUri(request.getRequestURI()) ;
            String realm = SAML2Utils.
                getRealm(SAML2MetaUtils.getRealmByMetaAlias(metaAlias));
            String party = partner.getPartner();
            if (idpSession != null) {
                idpSession.removeSessionPartner(party);
                IDPCache.idpSessionsBySessionID.remove(tokenID);
                initiateSPLogoutRequest(request,response, out, party, metaAlias, realm,
                    logoutReq, null, idpSession, binding, relayState);
            }
        } catch (SessionException se) {
            logger.error(
                "sendProxyLogoutRequest: ", se);
        }
   }

   public static void sendProxyLogoutResponse(
       HttpServletResponse response,
       HttpServletRequest request,
       String originatingRequestID,
       Map<String, String> infoMap,
       String remoteEntity,
       String binding) throws SAML2Exception {

       String entityID = infoMap.get("entityid");
       if (StringUtils.isEmpty(entityID)) {
           throw new SAML2Exception(SAML2Utils.bundle.getString("nullIDPEntityID"));
       }
       if (logger.isDebugEnabled()) {
           logger.debug("Proxy IDP EntityID=" + entityID);
       }
       String realm = infoMap.get(SAML2Constants.REALM);
       if (StringUtils.isEmpty(realm)) {
           realm = "/";
       }
       if (logger.isDebugEnabled()) {
            logger.debug("Proxy IDP Realm=" + realm);
       }
       LogoutResponse logoutRes = LogoutUtil.generateResponse(null, originatingRequestID,
               SAML2Utils.createIssuer(entityID), realm, SAML2Constants.IDP_ROLE, remoteEntity);

       EndpointType endpoint = getSLOElement(realm, remoteEntity, binding, SAML2Constants.SP_ROLE);
       binding = endpoint.getBinding();

       String location = endpoint.getLocation();

       if (logger.isDebugEnabled()) {
            logger.debug("Proxy to: " + location);
       }
       logoutRes.setDestination(XMLUtils.escapeSpecialCharacters(location));
       String relayState = infoMap.get(SAML2Constants.RELAY_STATE);
       LogoutUtil.sendSLOResponse(response, request, logoutRes, location, relayState, realm, entityID,
               SAML2Constants.IDP_ROLE, remoteEntity, binding);
   }

    public static void sendProxyLogoutRequestSOAP(
        HttpServletRequest request,
        HttpServletResponse response,
        PrintWriter out,
        SOAPMessage msg,
        List partners,
        IDPSession idpSession)
    {

            Iterator iter = partners.iterator();
            SAML2SessionPartner partner =
                (SAML2SessionPartner)iter.next();
            if (logger.isDebugEnabled()) {
                logger.debug(
                    "CURRENT PARTNER's provider ID: " +
                    partner.getPartner());
                logger.debug("Starting IDP proxy logout.");
            }

            String metaAlias =
                SAML2MetaUtils.getMetaAliasByUri(request.getRequestURI()) ;
            String realm = SAML2Utils.
                getRealm(SAML2MetaUtils.getRealmByMetaAlias(metaAlias));
            String party = partner.getPartner();
            idpSession.removeSessionPartner(party);
            String sessionIndex = IDPSSOUtil.getSessionIndex(idpSession.getSession());
            IDPSSOUtil.saveIdPSessionToTokenRepository(sessionIndex, sessionProvider, idpSession,
                idpSession.getSession());
            initiateSPLogoutRequest(request,response, out, party, metaAlias, realm,
                null, msg ,idpSession, SAML2Constants.SOAP, null);

   }

   public static Map getSessionPartners(SOAPMessage message) {
       try {
            Map sessMap = new HashMap();
            Element reqElem = SOAPCommunicator.getInstance().getSamlpElement(message,
                    "LogoutRequest");
            LogoutRequest logoutReq =
                ProtocolFactory.getInstance().createLogoutRequest(reqElem);
            List siList = logoutReq.getSessionIndex();
            int numSI = 0;
            if (siList != null) {
                numSI = siList.size();
                if (debug.isDebugEnabled()) {
                    debug.debug(
                        "Number of session indices in the logout request is "
                        + numSI);
                }

                String sessionIndex = (String)siList.get(0);
                if (logger.isDebugEnabled()) {
                    logger.debug("getSessionPartners: " +
                        "SessionIndex= " +  sessionIndex);
                }
                IDPSession idpSession = IDPSSOUtil.retrieveCachedIdPSession(sessionIndex);

                if (idpSession == null) {
                    // session is in another server
                    return sessMap;
                }

                sessMap.put(SAML2Constants.SESSION_INDEX, sessionIndex);
                sessMap.put(SAML2Constants.IDP_SESSION, idpSession);
                Object session = idpSession.getSession();
                String tokenId = sessionProvider.getSessionID(session);
                IDPSession newIdpSession = (IDPSession)
                    IDPCache.idpSessionsBySessionID.get(tokenId);
                List partners= null;
                if (newIdpSession != null) {
                    partners = newIdpSession.getSessionPartners();
                }

                if (logger.isDebugEnabled()) {
                    if (partners != null &&  !partners.isEmpty()) {
                        Iterator iter = partners.iterator();
                        while(iter.hasNext()) {
                            SAML2SessionPartner partner =
                                (SAML2SessionPartner)iter.next();
                            if (logger.isDebugEnabled()) {
                                logger.debug(
                                    "SESSION PARTNER's Provider ID:  "
                                    + partner.getPartner());
                            }
                        }
                    }
                }
                sessMap.put(SAML2Constants.PARTNERS, partners);
                return sessMap;
           } else {
               if (logger.isDebugEnabled()) {
                   logger.debug("getSessionPartners: Number of " +
		          "session indices in the logout request is null");
               }
               return null;
           }
        } catch (SAML2Exception se) {
           logger.error("getSessionPartners: ", se);
           return null;
        }
   }

   public static void sendProxyLogoutResponseBySOAP(SOAPMessage reply, HttpServletResponse resp, PrintWriter out) {

       try {
           //  Need to call saveChanges because we're
           // going to use the MimeHeaders to set HTTP
           // response information. These MimeHeaders
           // are generated as part of the save.
           if (reply.saveRequired()) {
               reply.saveChanges();
           }
           resp.setStatus(HttpServletResponse.SC_OK);
           SAML2Utils.putHeaders(reply.getMimeHeaders(), resp);
           // Write out the message on the response stream
           ByteArrayOutputStream stream = new ByteArrayOutputStream();
           reply.writeTo(stream);
           out.println(stream.toString());
           out.flush();
       } catch (SOAPException se) {
            logger.error("sendProxyLogoutResponseBySOAP: ", se);
       } catch (IOException ie) {
            logger.error("sendProxyLogoutResponseBySOAP: ", ie);
       }
   }

   public static void sendIDPInitProxyLogoutRequest(
        HttpServletRequest request,
        HttpServletResponse response,
        PrintWriter out,
        LogoutResponse logoutResponse,
        String location,
        String spEntityID,
        String idpEntityID,
        String binding,
        String realm,
        String relayState) throws SAML2Exception {

        String logoutAll = request.getParameter(SAML2Constants.LOGOUT_ALL);
        HashMap paramsMap = new HashMap();
        IDPSSOConfigElement config = sm.getIDPSSOConfig(realm, spEntityID);
        paramsMap.put("metaAlias", config.getValue().getMetaAlias());
        paramsMap.put(SAML2Constants.ROLE, SAML2Constants.IDP_ROLE);
        paramsMap.put(SAML2Constants.BINDING, SAML2Constants.HTTP_REDIRECT);
        paramsMap.put("Destination", request.getParameter("Destination"));
        paramsMap.put("Consent", request.getParameter("Consent"));
        paramsMap.put("Extension", request.getParameter("Extension"));
        paramsMap.put("RelayState", relayState);

        Map logoutResponseMap =  new HashMap();
        if (logoutResponse != null) {
            logoutResponseMap.put("LogoutResponse", logoutResponse);
        }
        if (location != null && !location.equals("")) {
           logoutResponseMap.put("Location", location);
        }
        if (spEntityID != null && !spEntityID.equals("")) {
            logoutResponseMap.put("spEntityID", spEntityID);
        }
        if (idpEntityID != null && !idpEntityID.equals("")) {
            logoutResponseMap.put("idpEntityID", idpEntityID);
        }
        paramsMap.put("LogoutMap", logoutResponseMap);

        if (logoutAll != null) {
            paramsMap.put(SAML2Constants.LOGOUT_ALL, logoutAll);
        }

        IDPSingleLogout.initiateLogoutRequest(request, response, out, binding, paramsMap);

        /*TODO:
        if (binding.equalsIgnoreCase(SAML2Constants.SOAP)) {
        if (RelayState != null) {
            response.sendRedirect(RelayState);
        } else {
            %>
            <jsp:forward
                page="/saml2/jsp/default.jsp?message=idpSloSuccess" />
            <%
        }
        }
        */
   }

   public static List getSPSessionPartners(HttpServletRequest request)
   {
       try {
           Object tmpsession = sessionProvider.getSession(request);
           String tokenID = sessionProvider.getSessionID(tmpsession);
           String pid = null;
           if (tokenID != null && !tokenID.equals("")) {
               pid=(String)IDPCache.spSessionPartnerBySessionID.get(tokenID);
               IDPCache.spSessionPartnerBySessionID.remove(tokenID);
           }
           List partners= null;
           if (pid != null && !pid.equals("")) {
               partners = new ArrayList();
               logger.debug(
                   "SP SESSION PARTNER's Provider ID:  " + pid);
                   partners.add(pid);
            }
            return partners;
        } catch (SessionException se) {
            return null;
        }
    }

   /**
     * Returns an <code>IDPProxyFinder</code>
     *
     * @param realm the realm name
     * @param idpEntityID the entity id of the identity provider
     *
     * @return the <code>IDPProxyFinder</code>
     * @exception SAML2Exception if the operation is not successful
     */
    static SAML2IDPFinder getIDPProxyFinder(
                                 String realm, String idpEntityID)
        throws SAML2Exception {
        String classMethod = "IDPProxyUtil.getIDPProxyFinder: ";
        String idpProxyFinderName = null;
        SAML2IDPFinder idpProxyFinder = null;
        try {
            idpProxyFinderName = IDPSSOUtil.getAttributeValueFromIDPSSOConfig(
                realm, idpEntityID, SAML2Constants.PROXY_IDP_FINDER_CLASS);
            if (idpProxyFinderName == null || idpProxyFinderName.isEmpty()) {
                idpProxyFinderName =
                    SAML2Constants.DEFAULT_IDP_PROXY_FINDER;
                if (logger.isDebugEnabled()) {
                    logger.debug(classMethod + "use " +
                    SAML2Constants.DEFAULT_IDP_PROXY_FINDER);
                }
            }
            idpProxyFinder = (SAML2IDPFinder)
                IDPCache.idpProxyFinderCache.get(
                                           idpProxyFinderName);
            if (idpProxyFinder == null) {
                idpProxyFinder = (SAML2IDPFinder)
                    Class.forName(idpProxyFinderName).newInstance();
                IDPCache.idpProxyFinderCache.put(
                    idpProxyFinderName, idpProxyFinder);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug(classMethod +
                        "got the IDPProxyFinder from cache");
                }
            }
        } catch (Exception ex) {
            logger.error(classMethod +
                "Unable to get IDP Proxy Finder.", ex);
            throw new SAML2Exception(ex);
        }

        return idpProxyFinder;
    }

    private static SPSSOConfigElement getSPSSOConfigByAuthnRequest(
            String realm, AuthnRequest request) throws SAML2MetaException {
        return IDPSSOUtil.metaManager.getSPSSOConfig(
                realm, request.getIssuer().getValue());
    }
}
