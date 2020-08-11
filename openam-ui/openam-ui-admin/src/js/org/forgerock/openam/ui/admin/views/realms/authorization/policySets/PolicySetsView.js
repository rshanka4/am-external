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
 * Copyright 2014-2019 ForgeRock AS.
 */

import "backbone.paginator";
import "org/forgerock/commons/ui/common/backgrid/extension/ThemeablePaginator";

import _ from "lodash";
import $ from "jquery";
import Backbone from "backbone";

import AbstractListView from "org/forgerock/openam/ui/admin/views/realms/authorization/common/AbstractListView";
import Backgrid from "org/forgerock/commons/ui/common/backgrid/Backgrid";
import BackgridUtils from "org/forgerock/openam/ui/common/util/BackgridUtils";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import IconAndDisplayNameCellTemplate from "templates/admin/backgrid/cell/IconAndDisplayNameCell";
import PoliciesService from "org/forgerock/openam/ui/admin/services/realm/PoliciesService";
import PolicySetModel from "org/forgerock/openam/ui/admin/models/authorization/PolicySetModel";
import PolicySetsTemplate from "templates/admin/views/realms/authorization/policySets/PolicySetsTemplate";
import PolicySetsToolbarTemplate from "templates/admin/views/realms/authorization/policySets/PolicySetsToolbarTemplate";
import RealmHelper from "org/forgerock/openam/ui/common/util/RealmHelper";
import Router from "org/forgerock/commons/ui/common/main/Router";
import RowActionsCellTemplate from "templates/admin/backgrid/cell/RowActionsCell";
import URLHelper from "org/forgerock/openam/ui/common/util/URLHelper";

export default AbstractListView.extend({
    template: PolicySetsTemplate,
    // Used in AbstractListView
    toolbarTemplate: PolicySetsToolbarTemplate,
    events: {
        "click [data-add-entity]":      "addNewPolicySet",
        "click [data-import-policies]": "startImportPolicies",
        "click [data-export-policies]": "exportPolicies",
        "click [data-add-resource]":    "addResource",
        "change [name=upload]":         "readImportFile"
    },
    render (args, callback) {
        this.realmPath = args[0];
        PoliciesService.listResourceTypes().then(_.bind(function (resourceTypes) {
            if (resourceTypes.resultCount < 1) {
                this.data.hasResourceTypes = false;
                this.parentRender(this.renderToolbar);
            } else {
                const PolicySets = Backbone.PageableCollection.extend({
                    url: URLHelper.substitute("__api__/applications"),
                    model: PolicySetModel,
                    state: BackgridUtils.getState(),
                    queryParams: BackgridUtils.getQueryParams({
                        filterName: "eq",
                        _queryFilter: [
                            `name+eq+${encodeURIComponent('"^(?!sunAMDelegationService$).*"')}`
                        ]
                    }),
                    parseState: BackgridUtils.parseState,
                    parseRecords: BackgridUtils.parseRecords,
                    sync (method, model, options) {
                        options.beforeSend = function (xhr) {
                            xhr.setRequestHeader("Accept-API-Version", "protocol=1.0,resource=2.0");
                        };
                        return BackgridUtils.sync(method, model, options);
                    }
                });

                this.data.selectedItems = [];
                this.data.hasResourceTypes = true;
                this.data.items = new PolicySets();
                this.data.items.fetch({ reset: true }).then((response) => {
                    if (response.resultCount > 0) {
                        this.data.hasPolicySets = true;
                        this.renderTable(callback);
                    } else {
                        this.data.hasPolicySets = false;
                        this.parentRender(this.renderToolbar);
                    }
                }, () => {
                    Router.routeTo(Router.configuration.routes.realms, { args: [], trigger: true });
                });
            }
        }, this));
    },

    renderTable (callback) {
        const self = this;
        const ClickableRow = BackgridUtils.ClickableRow.extend({
            callback (e) {
                const $target = $(e.target);

                if ($target.parents().hasClass("fr-col-btn-2")) {
                    return;
                }
                self.editRecord(e, this.model.id, Router.configuration.routes.realmsPolicySetEdit);
            }
        });

        const columns = [
            {
                name: "displayName",
                label: $.t("console.authorization.policySets.list.grid.0"),
                cell: BackgridUtils.TemplateCell.extend({
                    iconClass: "fa-folder",
                    template: IconAndDisplayNameCellTemplate,
                    rendered () {
                        this.$el.find("i.fa").addClass(this.iconClass);
                    }
                }),
                headerCell: BackgridUtils.FilterHeaderCell,
                sortType: "toggle",
                editable: false
            },
            {
                name: "",
                cell: BackgridUtils.TemplateCell.extend({
                    className: "fr-col-btn-2",
                    template: RowActionsCellTemplate,
                    events: {
                        "click [data-edit-item]": "editItem",
                        "click [data-delete-item]": "deleteItem"
                    },
                    editItem (e) {
                        self.editRecord(e, this.model.id, Router.configuration.routes.realmsPolicySetEdit);
                    },
                    deleteItem (e) {
                        self.onDeleteClick(e, { type: $.t("console.authorization.common.policySet") },
                            this.model.id);
                    }
                }),
                sortable: false,
                editable: false
            }
        ];

        const grid = new Backgrid.Grid({
            columns,
            row: ClickableRow,
            collection: this.data.items,
            className: "backgrid table table-hover",
            emptyText: $.t("console.common.noResults")
        });

        const paginator = new Backgrid.Extension.ThemeablePaginator({
            collection: this.data.items,
            windowSize: 3
        });

        this.bindDefaultHandlers();
        this.parentRender(() => {
            this.renderToolbar();
            this.$el.find(".table-container").append(grid.render().el);
            this.$el.find(".panel-body").append(paginator.render().el);

            if (callback) { callback(); }
        });
    },

    startImportPolicies () {
        this.$el.find("[name=upload]").trigger("click");
    },

    importPolicies (e) {
        PoliciesService.importPolicies(e.target.result).then(() => {
            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "policiesUploaded");
        }, (e) => {
            const applicationNotFoundInRealm = " application not found in realm";
            const responseText = e ? e.responseText : "";
            const messages = $($.parseXML(responseText)).find("message");
            const message = messages.length ? messages[0].textContent : "";
            const index = message ? message.indexOf(applicationNotFoundInRealm) : -1;

            if (index > -1) {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, {
                    key: "policiesImportFailed",
                    applicationName: message.slice(0, index)
                });
            } else {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "policiesUploadFailed");
            }
        });
    },

    readImportFile () {
        const file = this.$el.find("[name=upload]")[0].files[0];
        const reader = new FileReader();
        reader.onload = this.importPolicies;
        if (file) {
            reader.readAsText(file, "UTF-8");
        }
    },

    exportPolicies () {
        const realm = this.realmPath === "/" ? "" : RealmHelper.encodeRealm(this.realmPath);
        this.$el.find("[data-export-policies]").attr("href",
            `${Constants.host}${Constants.context}/xacml${realm}/policies`);
    },

    addResource () {
        Router.routeTo(Router.configuration.routes.realmsResourceTypeNew, {
            args: [encodeURIComponent(this.realmPath)],
            trigger: true
        });
    },

    addNewPolicySet () {
        Router.routeTo(Router.configuration.routes.realmsPolicySetNew, {
            args: [encodeURIComponent(this.realmPath)],
            trigger: true
        });
    }
});
