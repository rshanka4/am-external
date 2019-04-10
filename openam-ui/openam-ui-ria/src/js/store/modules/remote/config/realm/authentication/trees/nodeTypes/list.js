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
 * Copyright 2017-2018 ForgeRock AS.
 */
import { createAction, handleActions } from "redux-actions";
import { keyBy } from "lodash";

// Types
const SET = "remote/config/realm/authentication/trees/nodeTypes/list/SET";

// Actions
export const set = createAction(SET);

// Reducer
const initialState = {};
export default handleActions({
    [SET]: (state, action) => keyBy(action.payload, "_id")
}, initialState);
