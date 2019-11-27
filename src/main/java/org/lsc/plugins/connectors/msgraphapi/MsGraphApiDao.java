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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.lsc.exception.LscServiceException;
import org.lsc.plugins.connectors.msgraphapi.beans.User;
import org.lsc.plugins.connectors.msgraphapi.beans.UsersListResponse;
import org.lsc.plugins.connectors.msgraphapi.generated.MsGraphApiUsersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

public class MsGraphApiDao {
    public static final String USER_PATH = "/users";

    protected static final Logger LOGGER = LoggerFactory.getLogger(MsGraphApiDao.class);
    private final Client client;
    private final Optional<Integer> pageSize;
    private final String pivot;

    private WebTarget usersClient;

    private final String authorizationBearer;
    private final Optional<String> filter;

    public MsGraphApiDao(String token, MsGraphApiUsersService serviceConfiguration) {
        authorizationBearer = "Bearer " + token;
        this.filter = getStringParameter(serviceConfiguration.getFilter());
        this.pivot = getStringParameter(serviceConfiguration.getPivot()).orElse("mail");
        this.pageSize = Optional.ofNullable(serviceConfiguration.getPageSize()).filter(size -> size > 0);
        LOGGER.debug("bearer " + authorizationBearer);
        client = ClientBuilder.newClient()
            .register(JacksonFeature.class);
        usersClient = client
            .target("https://graph.microsoft.com")
            .path("v1.0")
            .path(USER_PATH);
    }

    private Optional<String> getStringParameter(String parameter) {
        return Optional.ofNullable(parameter).filter(filter -> !filter.trim().isEmpty());
    }

    public List<User> getUsersList() throws LscServiceException {
        WebTarget target = usersClient.queryParam("$select", pivot);

        if (filter.isPresent()) {
            target = target.queryParam("$filter", filter.get());
        }
        if (pageSize.isPresent()) {
            target = target.queryParam("$top", pageSize.get());
        }
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
            .map(map -> map.get(pivot))
            .filter(Objects::nonNull)
            .map(pivotValue -> new User(pivot, pivotValue.toString())).collect(Collectors.toList());
    }

    private UsersListResponse getUsersListResponse(WebTarget target) throws LscServiceException {
        LOGGER.debug("GETting users list or following page: " + target.getUri().toString());

        Response response = null;
        try {
            response = target.request()
                .header(HttpHeaders.AUTHORIZATION, authorizationBearer)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
            if (checkResponse(response)) {
                return response.readEntity(UsersListResponse.class);
            }
            throw new LscServiceException(response.readEntity(String.class));
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private static boolean checkResponse(Response response) {
        return Response.Status.Family.familyOf(response.getStatus()) == Response.Status.Family.SUCCESSFUL;
    }

    public Map<String, Object> getUserDetails(String pivotValue) throws LscServiceException {

        Response response = null;
        try {
            String pivotFilter = pivot + " eq '" + pivotValue + "'";
            WebTarget target = filter
                .map(filter -> usersClient.queryParam("$filter", "(" + filter + ") and " + pivotFilter))
                .orElseGet(() -> usersClient.queryParam("$filter", pivotFilter));

            LOGGER.debug("GETting users detail : " + target.getUri().toString());

            response = target
                .request()
                .header(HttpHeaders.AUTHORIZATION, authorizationBearer)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
            Map<String, Object> EMPTY_BEAN_RESPONSE = getStringObjectImmutableMap();
            if (checkResponse(response)) {
                return response.readEntity(UsersListResponse.class).getValue().stream().findFirst().orElse(EMPTY_BEAN_RESPONSE);
            }
            if(response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                return EMPTY_BEAN_RESPONSE;
            }
            throw new LscServiceException(response.readEntity(String.class));
        } finally {
            if (response != null) {
                response.close();
            }
        }

    }

    private Map<String, Object> getStringObjectImmutableMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put(pivot, null);
        return map;
    }
}
