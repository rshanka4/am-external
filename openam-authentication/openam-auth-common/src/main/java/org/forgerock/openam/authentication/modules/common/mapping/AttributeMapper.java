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
 * Copyright 2011-2017 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.common.mapping;

import com.sun.identity.authentication.spi.AuthLoginException;

import java.util.Map;
import java.util.Set;

/**
 * Translates from a source to a map of attributes.
 * @param <T> The type of source.
 *
 * @supported.all.api
 */
public interface AttributeMapper<T> {

    /**
     * Initialise the instance for i18n.
     * @param bundleName The name of the bundle for exceptions thrown by the getAttributes method.
     */
    void init(String bundleName);

    /**
     * Maps from values found in the source to a map of keys in the result, according to a provided map of keys in the
     * source to keys in the result.
     * @param attributeMapConfiguration The map of keys in the source to keys in the result.
     * @param source The source of values.
     * @return A map of attribute keys to values found.
     * @throws AuthLoginException
     */
    Map<String, Set<String>> getAttributes(Map<String, String> attributeMapConfiguration, T source)
            throws AuthLoginException;

}
