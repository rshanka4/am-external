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
 * Copyright 2015-2018 ForgeRock AS.
 */

import _ from "lodash";

import $ from "jquery";
import Router from "org/forgerock/commons/ui/common/main/Router";
import TreeNavigation from "org/forgerock/openam/ui/common/components/TreeNavigation";
import UMAService from "org/forgerock/openam/ui/user/uma/services/UMAService";
import NestedListPartial from "templates/user/uma/views/resource/_NestedList";
var LabelTreeNavigationView = TreeNavigation.extend({
    template: "user/uma/views/resource/LabelTreeNavigationTemplate",
    partials: {
        "templates/user/uma/views/resource/_NestedList": NestedListPartial
    },
    findActiveNavItem (fragment) {
        var myLabelsRoute = Router.configuration.routes.umaResourcesMyLabelsResource,
            isCurrentRouteForResource = Router.currentRoute === myLabelsRoute,
            subFragment = isCurrentRouteForResource ? _.initial(fragment.split("/")).join("/") : fragment,
            anchor = this.$el.find(`.sidenav ol > li > a[href='#${subFragment}']`),
            parentOls;

        if (anchor.length) {
            this.$el.find(".sidenav ol").removeClass("in");

            parentOls = anchor.parentsUntil(this.$el.find(".sidenav"), "ol.collapse");
            parentOls.addClass("in").parent().children("span[data-toggle]").attr("aria-expanded", "true");
            anchor.parent().addClass("active");

            if (anchor.attr("aria-expanded") === "false") {
                anchor.attr("aria-expanded", "true");
            }
        }
    },
    navigateToPage (event) {
        this.$el.find(".sidenav li").removeClass("active");
        $(event.currentTarget).addClass("active");
        this.renderSidePageContentOnly = true;
    },
    render (args, callback) {
        var self = this,
            userLabels,
            sortedUserLabels;

        this.args = args;
        this.callback = callback;

        UMAService.labels.all().then((data) => {
            if (!_.some(data.result, (label) => {
                return label.name.toLowerCase() === "starred";
            })) {
                UMAService.labels.create("starred", "STAR");
            }

            userLabels = _.filter(data.result, (label) => { return label.type.toLowerCase() === "user"; });
            sortedUserLabels = _.sortBy(userLabels, (label) => { return label.name; });

            self.data.labels = {
                starred: _.filter(data.result, (label) => { return label.type.toLowerCase() === "starred"; }),
                system: _.filter(data.result, (label) => { return label.type.toLowerCase() === "system"; }),
                user: sortedUserLabels
            };
            self.data.nestedLabels = [];

            _.each(self.data.labels.user, (label) => {
                self.addToParent(self.data.nestedLabels, label);
            });

            TreeNavigation.prototype.render.call(self, args, callback);
        });
    },

    addToParent (collection, label) {
        if (label.name.indexOf("/") === -1) {
            label.title = label.name;
            label.children = [];
            label.viewId = _.uniqueId("viewId_");
            collection.push(label);
        } else {
            var shift = label.name.split("/"),
                parentName = shift.shift(),
                parent;
            label.name = shift.join("/");
            parent = _.find(collection, { title: parentName });
            if (!parent) {
                parent = { title: parentName, children: [], viewId: _.uniqueId("viewId_") };
                collection.push(parent);
            }
            this.addToParent(parent.children, label);
        }
    },

    addUserLabels (userLabels) {
        var self = this;

        this.data.nestedLabels = [];
        this.data.labels.user = _.sortBy(userLabels, (label) => { return label.name; });

        _.each(this.data.labels.user, (label) => {
            self.addToParent(self.data.nestedLabels, label);
        });

        this.renderSidePageContentOnly = true;
        TreeNavigation.prototype.render.call(this, this.args, this.callback);
    }
});

export default new LabelTreeNavigationView();
