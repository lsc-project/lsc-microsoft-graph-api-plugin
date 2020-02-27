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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.type.TypeReference;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.lsc.exception.LscServiceException;
import org.lsc.plugins.connectors.msgraphapi.beans.User;
import org.lsc.plugins.connectors.msgraphapi.beans.UsersListResponse;
import org.lsc.plugins.connectors.msgraphapi.generated.MsGraphApiUsersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MsGraphApiDao {
    public static final String USER_PATH = "/users";
    public static final String DEFAULT_PIVOT = "mail";
    public static final String ID = "id";
    private static final Logger LOGGER = LoggerFactory.getLogger(MsGraphApiDao.class);

    private final Client client;
    private WebTarget usersClient;
    private final String authorizationBearer;

    private final Optional<Integer> pageSize;
    private final String pivot;
    private final Optional<String> filter;
    private final Optional<String> select;

    public MsGraphApiDao(String token, MsGraphApiUsersService serviceConfiguration) {
        authorizationBearer = "Bearer " + token;
        this.filter = getStringParameter(serviceConfiguration.getFilter());
        this.select = getStringParameter(serviceConfiguration.getSelect());
        this.pivot = getStringParameter(serviceConfiguration.getPivot()).orElse(DEFAULT_PIVOT);
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
        return getUsersList(filter);
    }

    private List<User> getUsersList(Optional<String> computedFilter) {
        WebTarget target = pivot.equals(ID) ? usersClient.queryParam("$select", pivot) : usersClient.queryParam("$select","id," + pivot);

        if (computedFilter.isPresent()) {
            target = target.queryParam("$filter", computedFilter.get());
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
            .filter(this::hasPivots)
            .map(map -> new User(pivot, map.get(pivot).toString(), map.get(ID).toString())).collect(Collectors.toList());
    }

    private boolean hasPivots(Map<String, Object> map) {
        if (map.get(ID) == null || map.get(pivot) == null) {
            LOGGER.warn("The entry " + map.toString() + " has no pivot '" + pivot + "' or id and has been ignored.");
            return false;
        }
        return true;
    }

    private UsersListResponse getUsersListResponse(WebTarget target) {
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
            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                throw new NotFoundException("Not found when requesting " + target.getUri());
            }
            throw new ProcessingException(response.readEntity(String.class));
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private static boolean checkResponse(Response response) {
        return Response.Status.Family.familyOf(response.getStatus()) == Response.Status.Family.SUCCESSFUL;
    }

    public Map<String, Object> getUserDetails(String id) {

        Response response = null;
        try {
            WebTarget target = usersClient.path(id);
            if (select.isPresent()) {
                target = target.queryParam("$select", select.get());
            }
            LOGGER.debug("GETting users detail : " + target.getUri().toString());

            response = target
                .request()
                .header(HttpHeaders.AUTHORIZATION, authorizationBearer)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
            if (checkResponse(response)) {
                return response.readEntity(new GenericType<>(new TypeReference<Map<String, Object>>() {}.getType()));
            }
            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                throw new NotFoundException(id + " cannot be found");
            }
            throw new ProcessingException(response.readEntity(String.class));
        } finally {
            if (response != null) {
                response.close();
            }
        }

    }

    public Optional<User> getFirstUserWithId(String pivotValue) {
        String pivotFilter = pivot + " eq '" + pivotValue.replaceAll("'", "''") + "'";
        String computedFilter = filter.map(f -> "(" + f + ")" + " and " + pivotFilter)
            .orElse(pivotFilter);
        return getUsersList(Optional.of(computedFilter))
            .stream()
            .findFirst();
    }
}
