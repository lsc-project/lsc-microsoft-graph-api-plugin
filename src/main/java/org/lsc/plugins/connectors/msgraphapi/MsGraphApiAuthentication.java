/*
 ****************************************************************************
 * Ldap Synchronization Connector provides tools to synchronize
 * electronic identities from a list of data sources including
 * any database with a JDBC connector, another LDAP directory,
 * flat files...
 *
 *                  ==LICENSE NOTICE==
 *
 * Copyright (c) 2008 - 2019 LSC Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:

 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of the LSC Project nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *                  ==LICENSE NOTICE==
 *
 *               (c) 2008 - 2019 LSC Project
 *         Raphael Ouazana <rouazana@linagora.com>
 ****************************************************************************
 */
package org.lsc.plugins.connectors.msgraphapi;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.map.ObjectMapper;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.lsc.plugins.connectors.msgraphapi.beans.AuthenticationResponse;

public class MsGraphApiAuthentication {
    private static final String DEFAULT_AUTHENTICATION_URL = "https://login.microsoftonline.com/";
    private static final String DEFAULT_USERS_URL = "https://graph.microsoft.com";
    private static final String GRAPH_DEFAULT_SCOPE = "/.default";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public AuthenticationResponse authenticate(String tenant, String authenticationURL, String usersURL, String clientId, String clientSecret) throws AuthorizationException {

        if( authenticationURL == null || authenticationURL.isEmpty() )
        {
            authenticationURL = DEFAULT_AUTHENTICATION_URL;
        }

        String scope;
        if( usersURL == null || usersURL.isEmpty() )
        {
            usersURL = DEFAULT_USERS_URL;
        }
        scope = usersURL.replaceAll("/$", "") + GRAPH_DEFAULT_SCOPE;

        WebTarget authTarget = ClientBuilder.newClient()
            .register(JacksonFeature.class)
            .target(authenticationURL)
            .path(tenant)
            .path("oauth2/v2.0/token");
        Form authForm = new Form("client_id", clientId)
            .param("scope", scope)
            .param("client_secret", clientSecret)
            .param("grant_type", "client_credentials");

        Response response = null;
        try {
            response = authTarget.request().post(Entity.form(authForm));
            if (! checkResponse(response)) {
                throw new AuthorizationException(response.readEntity(String.class));
            }
            return response.readEntity(AuthenticationResponse.class);
        } catch (Exception e) {
            throw new AuthorizationException(e);
        } finally {
            response.close();
        }
    }

    private static boolean checkResponse(Response response) {
        return Response.Status.Family.familyOf(response.getStatus()) == Response.Status.Family.SUCCESSFUL;
    }


}
