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
package org.forgerock.openam.auth.node.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Meta data API to expose data concerning the evaluating tree, to nodes who care for that data.
 *
 * @supported.all.api
 */
public interface TreeMetaData {

    /**
     * Calculate maximum auth level which the evaluating tree can give from the specified node.
     *
     * @param nodeId the ID of the current node
     * @param outcome the outcome of the current node
     * @return maximum auth level which this tree can give
     * @throws NodeProcessException if some internal error occurs whilst attempting to retrieve the max auth level
     */
    default Optional<Integer> getMaxAuthLevel(UUID nodeId, String outcome) throws NodeProcessException {
        return Optional.empty();
    }

}
