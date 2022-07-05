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
 * Copyright 2016-2021 ForgeRock AS.
 */
package org.forgerock.openam.guice;

import static com.sun.identity.saml2.common.SAML2Constants.SCRIPTED_IDP_ADAPTER;
import static com.sun.identity.saml2.common.SAML2Constants.SCRIPTED_IDP_ATTRIBUTE_MAPPER;

import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Named;
import javax.inject.Singleton;

import org.forgerock.openam.audit.context.AMExecutorServiceFactory;
import org.forgerock.openam.federation.config.Saml2DataStoreListener;
import org.forgerock.openam.saml2.plugins.Saml2CredentialResolver;
import org.forgerock.openam.saml2.plugins.SecretsSaml2CredentialResolver;
import org.forgerock.openam.saml2.service.Saml2ScriptContextProvider;
import org.forgerock.openam.scripting.persistence.config.defaults.ScriptContextDetailsProvider;
import org.forgerock.openam.services.datastore.DataStoreServiceChangeNotifier;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.sun.identity.saml2.plugins.IDPAttributeMapper;
import com.sun.identity.saml2.plugins.SAML2IdentityProviderAdapter;
import com.sun.identity.saml2.plugins.scripted.ScriptedIdpAdapter;
import com.sun.identity.saml2.plugins.scripted.ScriptedIdpAttributeMapper;

/**
 * Responsible for declaring the bindings required for the federation code base.
 */
public class FederationGuiceModule extends AbstractModule {

    /**
     * Tag for all operations for use within the federation session management code. e.g. Thread names, Debugger etc.
     */
    public static final String FEDERATION_SESSION_MANAGEMENT = "FederationSessionManagement";

    @Override
    protected void configure() {
        Multibinder.newSetBinder(binder(), ScriptContextDetailsProvider.class)
                .addBinding().to(Saml2ScriptContextProvider.class);
        bind(Saml2CredentialResolver.class).to(SecretsSaml2CredentialResolver.class);
        Multibinder.newSetBinder(binder(), DataStoreServiceChangeNotifier.class)
                .addBinding().to(Saml2DataStoreListener.class);
        bind(Key.get(IDPAttributeMapper.class,
                Names.named(SCRIPTED_IDP_ATTRIBUTE_MAPPER))).to(ScriptedIdpAttributeMapper.class);
        bind(Key.get(SAML2IdentityProviderAdapter.class,
                Names.named(SCRIPTED_IDP_ADAPTER))).to(ScriptedIdpAdapter.class);
    }

    @Provides
    @Singleton
    @Named(FEDERATION_SESSION_MANAGEMENT)
    public ScheduledExecutorService getFederationScheduledService(AMExecutorServiceFactory esf) {
        return esf.createCancellableScheduledService(1, FEDERATION_SESSION_MANAGEMENT);
    }
}
