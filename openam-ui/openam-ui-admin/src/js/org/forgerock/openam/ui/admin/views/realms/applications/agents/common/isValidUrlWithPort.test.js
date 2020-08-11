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
 * Copyright 2018-2019 ForgeRock AS.
 */

import { expect } from "chai";

import isValidUrlWithPort from "./isValidUrlWithPort";

describe("org/forgerock/openam/ui/admin/views/realms/applications/agents/common/isValidUrlWithPort", () => {

    describe("the URL is valid", () => {
        it("finds AM URLs with path valid", () => {
            expect(isValidUrlWithPort("http://openam.example.com:8080/openam")).to.be.true;
        });
        it("finds AM URLs with empty context path valid", () => {
            expect(isValidUrlWithPort("http://openam.example.com:8080/")).to.be.true;
        });
        it("accepts empty context path without the explicit '/'", () => {
            expect(isValidUrlWithPort("http://openam.example.com:8080")).to.be.true;
        });
        it("finds AM URLs with HTTPS valid", () => {
            expect(isValidUrlWithPort("https://openam.example.com:443/openam")).to.be.true;
        });
    });

    describe("the URL is invalid", () => {
        it("requires explicit port definitions", () => {
            expect(isValidUrlWithPort("https://openam.example.com/openam")).to.be.false;
        });
        it("finds host only URLs invalid", () => {
            expect(isValidUrlWithPort("openam.example.com")).to.be.false;
        });
        it("requires explicit protocol definition", () => {
            expect(isValidUrlWithPort("openam.example.com:8080/openam")).to.be.false;
        });
    });
});
