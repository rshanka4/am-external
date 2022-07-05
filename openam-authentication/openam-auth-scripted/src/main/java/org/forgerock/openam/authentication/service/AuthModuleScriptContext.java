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
package org.forgerock.openam.authentication.service;

import org.forgerock.openam.scripting.domain.ScriptContext;

/**
 * Definitions of {@link ScriptContext}s for authentication module scripts.
 */
public enum AuthModuleScriptContext implements ScriptContext {

    /**
     * The default server-side authentication script context.
     */
    AUTHENTICATION_SERVER_SIDE,

    /**
     * The default client-side authentication script context.
     */
    AUTHENTICATION_CLIENT_SIDE;
}
