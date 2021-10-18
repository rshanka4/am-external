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
 * Copyright 2015-2021 ForgeRock AS.
 */

import "./webpack/setWebpackPublicPath";
import "babel-polyfill";
import "regenerator-runtime/runtime";
import "whatwg-fetch";

import $ from "jquery";

import { getTheme } from "ThemeManager";
import { init } from "org/forgerock/commons/ui/common/main/i18n/manager";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import loadTemplate from "org/forgerock/openam/ui/common/util/theme/loadTemplate";
import prependPublicPath from "webpack/prependPublicPath";

const data = window.pageData;
const deviceTemplate = data.done ? "user/DeviceDoneTemplate" : "user/DeviceTemplate";
const language = data.locale ? data.locale.split(" ")[0] : undefined;

const loadThemedTemplates = async (templates, theme) => {
    return await Promise.all(templates.map((template) => loadTemplate(template, theme)));
};

init({
    language,
    loadPath:  prependPublicPath("locales/__lng__/__ns__.json"),
    namespace: "device"
}).then(() => {
    Configuration.globalData = { realm : data.realm };

    const render = async () => {
        const theme = await getTheme();
        data.theme = theme;
        const templates = ["common/LoginBaseTemplate", "common/FooterTemplate",
            "common/LoginHeaderTemplate", deviceTemplate];
        const [LoginBaseTemplate, FooterTemplate, LoginHeaderTemplate,
            DeviceTemplate] = await loadThemedTemplates(templates, theme);
        $("html").attr("lang", language);
        $("#wrapper").html(LoginBaseTemplate(data));
        $("#footer").html(FooterTemplate(data));
        $("#loginBaseLogo").html(LoginHeaderTemplate(data));
        $("#content").html(DeviceTemplate(data));
    };
    render();
});
