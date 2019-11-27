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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.lsc.plugins.connectors.msgraphapi.beans.User;
import org.lsc.plugins.connectors.msgraphapi.beans.UsersListResponse;
import org.lsc.plugins.connectors.msgraphapi.generated.MsGraphApiUsersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MsGraphApiDao {
    public static final String USER_PATH = "/users";

    protected static final Logger LOGGER = LoggerFactory.getLogger(MsGraphApiDao.class);
    private final Client client;

    private WebTarget usersClient;

    private final String authorizationBearer;
    private final Optional<String> filter;

    public MsGraphApiDao(String token, MsGraphApiUsersService serviceConfiguration) {
        authorizationBearer = "Bearer " + token;
        this.filter = Optional.ofNullable(serviceConfiguration.getFilter()).filter(filter -> !filter.trim().isEmpty());
        LOGGER.debug("bearer " + authorizationBearer);
        client = ClientBuilder.newClient()
            .register(JacksonFeature.class);
        usersClient = client
            .target("https://graph.microsoft.com")
            .path("v1.0")
            .path(USER_PATH);
    }

    public List<User> getUsersList() {
        WebTarget target = usersClient.queryParam("$select", "mail");

        if (filter.isPresent()) {
            target = target.queryParam("$filter", filter.get());
        }
        LOGGER.debug("GETting users list: " + target.getUri().toString());
        UsersListResponse users = getUsersListResponse(target);

        List<UsersListResponse> usersResponsesPages = new ArrayList<>();
        usersResponsesPages.add(users);

        String nextLink = users.getNextLink();
        while (StringUtils.isNotBlank(nextLink)) {
            UsersListResponse lastResponse = getUsersListResponse(client.target(nextLink));
            usersResponsesPages.add(lastResponse);
            nextLink = lastResponse.getNextLink();
        }

        return usersResponsesPages
            .stream()
            .flatMap(response -> response.getValue().stream())
            .map(map -> map.get("mail"))
            .filter(Objects::nonNull)
            .map(mail -> new User(mail.toString())).collect(Collectors.toList());

    }

    private UsersListResponse getUsersListResponse(WebTarget target) {
        return target.request()
            .header(HttpHeaders.AUTHORIZATION, authorizationBearer)
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .get(UsersListResponse.class);
    }
}
