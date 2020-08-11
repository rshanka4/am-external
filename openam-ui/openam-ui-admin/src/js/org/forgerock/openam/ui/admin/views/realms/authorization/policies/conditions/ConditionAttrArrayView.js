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
 * Copyright 2015-2019 ForgeRock AS.
 */

import "selectize";

import _ from "lodash";
import $ from "jquery";

import ConditionAttrArrayTemplate from
    "templates/admin/views/realms/authorization/policies/conditions/ConditionAttrArray";
import ConditionAttrBaseView from
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/conditions/ConditionAttrBaseView";
import PoliciesService from "org/forgerock/openam/ui/admin/services/realm/PoliciesService";

const DEFAULT_TIME_ZONE = "GMT";
const IDENTITY_PLACEHOLDER = "console.authorization.policies.edit.subjectTypes.Identity.placeholder";
const IDENTITY_TYPES = ["users", "groups"];
const MIN_QUERY_LENGTH = 1;
const SCRIPT_PLACEHOLDER = "console.authorization.policies.edit.conditionTypes.Script.placeholder";
const SCRIPT_TYPE = "scriptId";
const AUTHENTICATION_STRATEGY_ENUM_ARRAY = [
    "AuthenticateToServiceConditionAdvice", "AuthenticateToRealmConditionAdvice",
    "AuthenticateToTreeConditionAdvice", "AuthSchemeConditionAdvice", "AuthLevelConditionAdvice"
];
const AUTHENTICATION_STRATEGY_TYPE = "authenticationStrategy";
const AUTHENTICATION_STRATEGY_I18N_PATH = "console.authorization.policies.edit.conditionTypes.Transaction.props.";
const TIME_ZONE_PLACEHOLDER = "console.authorization.policies.edit.conditionTypes.SimpleTime.props.enterTimeZone";
const TIME_ZONE_TYPE = "enforcementTimeZone";

export default ConditionAttrBaseView.extend({
    template: ConditionAttrArrayTemplate,

    render (data, element, callback) {
        if (data.title === AUTHENTICATION_STRATEGY_TYPE) {
            data.multiple = false;
        }

        // default to multiple selection if this option is not specified
        if (data.multiple === undefined) {
            data.multiple = true;
        }

        this.initBasic(data, element, "field-float-selectize data-obj");

        this.parentRender(function () {
            const view = this;
            let title = "";
            let text = "";
            let itemData;
            let options;
            let item;
            let $item;
            let type;

            this.$el.find("select.selectize").each(function () {
                item = this;
                $item = $(this);
                type = $item.parent().find("label").data().title;
                options = {};

                if ($item.data().source) {
                    if (type === SCRIPT_TYPE) {
                        _.extend(options, {
                            placeholder: $.t(SCRIPT_PLACEHOLDER),
                            preload: true,
                            sortField: "value",
                            load (query, callback) {
                                view.loadFromDataSource.call(this, item, callback);
                            },
                            onChange (value) {
                                title = this.$input.parent().find("label").data().title;
                                text = this.$input.find(":selected").text();
                                view.data.itemData[title] = value ? value : "";
                                view.data.hiddenData[view.data.itemData.type] = text ? text : "";
                            }
                        });
                    } else if (type === TIME_ZONE_TYPE) {
                        _.extend(options, {
                            placeholder: $.t(TIME_ZONE_PLACEHOLDER),
                            preload: true,
                            sortField: "value",
                            render: {
                                item (item) {
                                    return `<span class='time-zone-selected'>${item.text}</span>`;
                                }
                            },
                            load () {
                                const selectize = this;
                                $.ajax({
                                    url: "timezones.json",
                                    dataType: "json",
                                    cache: true
                                }).then((data) => {
                                    _.each(data.timezones, (value) => {
                                        selectize.addOption({ value, text: value });
                                    });
                                });
                            },
                            onChange (value) {
                                view.data.itemData.enforcementTimeZone = value ? value : DEFAULT_TIME_ZONE;
                            }
                        });
                    } else if (_.includes(IDENTITY_TYPES, type)) {
                        _.extend(options, {
                            placeholder: $.t(IDENTITY_PLACEHOLDER),
                            sortField: "value",
                            load (query, callback) {
                                if (query.length < MIN_QUERY_LENGTH) {
                                    return callback();
                                }
                                view.queryIdentities.call(this, item, query, callback);
                            },
                            onItemAdd (item) {
                                view.getUniversalId(item, type);
                            },
                            onItemRemove (item) {
                                const universalid = _.findKey(view.data.hiddenData[type], (obj) => {
                                    return obj === item;
                                });

                                view.data.itemData.subjectValues = _.without(view.data.itemData.subjectValues,
                                    universalid);
                                delete view.data.hiddenData[type][universalid];
                            }
                        });
                    }
                } else if (type === AUTHENTICATION_STRATEGY_TYPE) {
                    _.extend(options, {
                        placeholder: $.t("common.form.select"),
                        valueField: "value",
                        labelField: "label",
                        options: _.map(AUTHENTICATION_STRATEGY_ENUM_ARRAY, (key) => {
                            return {
                                value: key,
                                label: $.t(`${AUTHENTICATION_STRATEGY_I18N_PATH}${key}`)
                            };
                        }),
                        items: [view.data.itemData.authenticationStrategy],
                        create: false,
                        onChange (value) {
                            title = this.$input.parent().find("label").data().title;
                            itemData = view.data.itemData;
                            itemData[title] = value ? value : "";
                        }
                    });
                } else {
                    _.extend(options, {
                        delimiter: false,
                        persist: false,
                        create (input) {
                            return {
                                value: input,
                                text: input
                            };
                        },
                        onChange (value) {
                            title = this.$input.parent().find("label").data().title;
                            itemData = view.data.itemData;
                            itemData[title] = value ? value : [];
                        }
                    });
                    if ($item.prev("label").data("title") === "dnsName") {
                        options.createFilter = function (text) {
                            return text.indexOf("*") === -1 || text.lastIndexOf("*") === 0;
                        };
                    }
                }

                _.extend(options, { plugins: ["restore_on_backspace"] });
                $item.selectize(options);
            });

            if (callback) {
                callback();
            }
        });
    },

    queryIdentities (item, query, callback) {
        const selectize = this;
        PoliciesService.queryIdentities($(item).data().source, query)
            .then((data) => {
                _.each(data.result, (value) => {
                    selectize.addOption({ value, text: value });
                });
                callback(data.result);
            }, (e) => {
                console.error("error", e);
                callback();
            });
    },

    getUniversalId (item, type) {
        const self = this;
        PoliciesService.getUniversalId(item, type).then((subject) => {
            self.data.itemData.subjectValues = _.union(self.data.itemData.subjectValues, subject.universalid);
            self.data.hiddenData[type][subject.universalid[0]] = item;
        });
    },

    loadFromDataSource (item, callback) {
        const selectize = this;
        PoliciesService.getDataByType($(item).data().source).then((data) => {
            _.each(data.result, (value) => {
                selectize.addOption({ value: value._id, text: value.name });
            });
            callback(data.result);
        }, (e) => {
            console.error("error", e);
            callback();
        });
    }
});
