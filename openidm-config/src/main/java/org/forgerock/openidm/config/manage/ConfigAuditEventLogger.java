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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openidm.config.manage;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.audit.events.AuditEvent;
import org.forgerock.audit.events.ConfigAuditEventBuilder;
import org.forgerock.http.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.SecurityContext;
import org.forgerock.openidm.patch.JsonPatch;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilizing the ConfigAuditEventBuilder this class will send log events of config changes to the commons
 * audit module.
 *
 * @see ConfigAuditEventBuilder
 */
public class ConfigAuditEventLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigAuditEventLogger.class);

    public static final String CONFIG_AUDIT_EVENT_NAME = "CONFIG";
    public static final String AUDIT_CONFIG_REST_PATH = "audit/config";

    /**
     * Calls buildAuditEvent() and invokes the request to the audit path.
     *
     * @param connectionFactory factory used to make crest call to audit service.
     */
    public final Promise<ResourceResponse, ResourceException> log(ConfigAuditState configAuditState, Request request,
            Context context, ConnectionFactory connectionFactory) {
        try {

            JsonValue before = configAuditState.getBefore();
            JsonValue after = configAuditState.getAfter();

            // Get authenticationId from security context, if it exists.
            String authenticationId = (context.containsContext(SecurityContext.class))
                    ? context.asContext(SecurityContext.class).getAuthenticationId() : null;

            // Build the event utilizing the config builder.
            AuditEvent auditEvent = ConfigAuditEventBuilder.configEvent()
                    .resourceOperationFromRequest(request)
                    .authenticationFromSecurityContext(context)
                    .runAs(authenticationId)
                    .transactionIdFromRootContext(context)
                    .revision(configAuditState.getRevision())
                    .timestamp(System.currentTimeMillis())
                    .eventName(CONFIG_AUDIT_EVENT_NAME)
                    .before(null != before ? before.toString() : "")
                    .after(null != after ? after.toString() : "")
                    .changedFields(getChangedFields(before, after))
                    .toEvent();

            return connectionFactory.getConnection().create(context,
                    Requests.newCreateRequest(AUDIT_CONFIG_REST_PATH, auditEvent.getValue())).asPromise();
        } catch (ResourceException e) {
            LOGGER.error("had trouble logging audit event for config changes.", e);
            return e.asPromise();
        } catch (Exception e) {
            LOGGER.error("had trouble logging audit event for config changes.", e);
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    private String[] getChangedFields(JsonValue before, JsonValue after) {
        if (null == before && null == after) {
            return new String[0];
        }

        if (null != before && null == after) {
            after = json(object());
        } else if (before == null) {
            before = json(object());
        }

        List<String> changedFields = new ArrayList<>();
        for (JsonValue change : JsonPatch.diff(before, after)) {
            changedFields.add(change.get(JsonPatch.PATH_PTR).asString());
        }
        return changedFields.toArray(new String[changedFields.size()]);
    }

}