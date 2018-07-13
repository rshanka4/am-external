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
 * Copyright 2014-2017 ForgeRock AS.
 */

require.config({
    map: {
        "*" : {
            "ThemeManager" : "org/forgerock/openam/ui/common/util/ThemeManager",
            "Router": "org/forgerock/openam/ui/common/SingleRouteRouter",
            // TODO: Remove this when there are no longer any references to the "underscore" dependency
            "underscore"   : "lodash"
        }
    },
    paths: {
        "handlebars": "libs/handlebars-4.0.5",
        "i18next": "libs/i18next-1.7.3-min",
        "jquery": "libs/jquery-2.1.1-min",
        "lodash": "libs/lodash-3.10.1-min",
        "redux": "libs/redux-3.5.2-min",
        "text": "libs/text-2.0.15"
    },
    shim: {
        "handlebars": {
            exports: "handlebars"
        },
        "i18next": {
            deps: ["jquery", "handlebars"],
            exports: "i18n"
        },
        "lodash": {
            exports: "_"
        }
    }
});

require([
    "jquery",
    "lodash",
    "handlebars",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openam/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/i18nManager",
    "ThemeManager"
], function ($, _, HandleBars, Configuration, Constants, i18nManager, ThemeManager) {

    // Helpers for the code that hasn't been properly migrated to require these as explicit dependencies:
    window.$ = $;
    window._ = _;

    var formTemplate,
        baseTemplate,
        footerTemplate,
        loginHeaderTemplate,
        templatePaths = [
            "templates/user/AuthorizeTemplate.html",
            "templates/common/LoginBaseTemplate.html",
            "templates/common/FooterTemplate.html",
            "templates/common/LoginHeaderTemplate.html"
        ],
        data = window.pageData || {},
        KEY_CODE_ENTER = 13,
        KEY_CODE_SPACE = 32;

    i18nManager.init({
        serverLang: data.serverLang,
        paramLang: {
            locale: data.locale
        },
        defaultLang: Constants.DEFAULT_LANGUAGE,
        nameSpace: "authorize"
    });

    if (data.oauth2Data) {
        _.each(data.oauth2Data.displayScopes, function (obj) {
            if (_.isEmpty(obj.values)) {
                delete obj.values;
            }
            return obj;
        });

        _.each(data.oauth2Data.displayClaims, function (obj) {
            if (_.isEmpty(obj.values)) {
                delete obj.values;
            }
            return obj;
        });

        if (_.isEmpty(data.oauth2Data.displayScopes) && _.isEmpty(data.oauth2Data.displayClaims)) {
            data.noScopes = true;
        }
    } else {
        data.noScopes = true;
    }

    Configuration.globalData = { realm : data.realm };

    ThemeManager.getTheme().always(function (theme) {

        // add prefix to templates for custom theme when path is defined
        var themePath = Configuration.globalData.theme.path;
        templatePaths = _.map(templatePaths, function (templatePath) {
            return `text!${themePath}${templatePath}`;
        });

        require(templatePaths, function (AuthorizeTemplate, LoginBaseTemplate, FooterTemplate, LoginHeaderTemplate) {
            data.theme = theme;
            baseTemplate = HandleBars.compile(LoginBaseTemplate);
            formTemplate = HandleBars.compile(AuthorizeTemplate);
            footerTemplate = HandleBars.compile(FooterTemplate);
            loginHeaderTemplate = HandleBars.compile(LoginHeaderTemplate);

            $("#wrapper").html(baseTemplate(data));
            $("#footer").html(footerTemplate(data));
            $("#loginBaseLogo").html(loginHeaderTemplate(data));
            $("#content").html(formTemplate(data)).find(".panel-heading").bind("click keyup", function (e) {
                // keyup is required so that the collapsed panel can be opened with the keyboard alone,
                // and without relying on a mouse click event.
                if (e.type === "keyup" && e.keyCode !== KEY_CODE_ENTER && e.keyCode !== KEY_CODE_SPACE) {
                    return;
                }
                $(this).toggleClass("expanded").next(".panel-collapse").slideToggle();
            });
        });
    });
});
