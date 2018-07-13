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
 * Copyright 2016-2017 ForgeRock AS.
 */
package org.forgerock.openam.plugin.configuration;

import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.configuration.AuditServiceConfigurationProvider;
import org.forgerock.openam.audit.context.TransactionIdConfiguration;

import com.google.inject.AbstractModule;

/**
 * Guice Module for configuring bindings for the OpenAM Fedlet Audit Configuration classes.
 *
 */
public class FedletAuditConfigurationGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(AuditEventPublisher.class).to(FedletAuditEventPublisherImpl.class);
        bind(AuditServiceConfigurationProvider.class).to(FedletAuditServiceConfigurationProviderImpl.class);
        bind(TransactionIdConfiguration.class).to(FedletTransactionIdConfigurationImpl.class);
    }
}
