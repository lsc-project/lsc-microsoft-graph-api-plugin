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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lsc.LscDatasets;
import org.lsc.configuration.PluginSourceServiceType;
import org.lsc.configuration.ServiceType;
import org.lsc.configuration.TaskType;
import org.lsc.exception.LscServiceException;
import org.lsc.plugins.connectors.msgraphapi.generated.MsGraphApiConnectionType;
import org.lsc.plugins.connectors.msgraphapi.generated.MsGraphApiUsersService;

import com.google.common.collect.ImmutableList;

class MsGraphApiUsersServiceTest {

    private static TaskType task;
    private MsGraphApiUsersService usersService;
    private static PluginSourceServiceType pluginSourceService;
    private static ServiceType.Connection connection;
    private static int DEFAULT_PAGE_SIZE = 100;
    private static int PAGE_SIZE = 300;

    @BeforeAll
    static void setup() throws AuthorizationException {
        MsGraphApiAuthentication msGraphApiAuthentication = new MsGraphApiAuthentication();

        pluginSourceService = mock(PluginSourceServiceType.class);
        MsGraphApiConnectionType pluginConnection = mock(MsGraphApiConnectionType.class);
        connection = mock(ServiceType.Connection.class);
        task = mock(TaskType.class);

        when(pluginConnection.getClientId()).thenReturn(System.getenv("TEST_MS_GRAPH_API_CLIENT_ID"));
        when(pluginConnection.getClientSecret()).thenReturn(System.getenv("TEST_MS_GRAPH_API_CLIENT_SECRET"));
        when(pluginConnection.getTenant()).thenReturn(System.getenv("TEST_MS_GRAPH_API_TENANT"));
        when(connection.getReference()).thenReturn(pluginConnection);
        when(task.getBean()).thenReturn("org.lsc.beans.SimpleBean");
        when(task.getPluginSourceService()).thenReturn(pluginSourceService);
    }

    @BeforeEach
    void defineService() {
        usersService = mock(MsGraphApiUsersService.class);
        when(usersService.getConnection()).thenReturn(connection);
        when(pluginSourceService.getAny()).thenReturn(ImmutableList.of(usersService));
    }

    @Test
    void listPivotShouldReturnEmptyWhenNoResult() throws LscServiceException {
        when(usersService.getFilter()).thenReturn("mail eq '5e922b0a-52d0-453b-9d97-9133f901f193'");
        MsGraphApiUsersSrcService testee = new MsGraphApiUsersSrcService(task);

        Map<String, LscDatasets> listPivots = testee.getListPivots();
        assertThat(listPivots).isEmpty();
    }

    @Test
    void listPivotShouldReturnOneUserWhenOneResult() throws LscServiceException {
        MsGraphApiUsersSrcService testee = new MsGraphApiUsersSrcService(task);

        Map<String, LscDatasets> listPivots = testee.getListPivots();

        String first = listPivots.keySet().stream().findFirst().get();

        when(usersService.getFilter()).thenReturn("mail eq '" + first + "'");
        testee = new MsGraphApiUsersSrcService(task);

        Map<String, LscDatasets> actual = testee.getListPivots();
        assertThat(actual).hasSize(1);
    }

    @Test
    void listPivotShouldReturnAllResultsWhenMoreThanOnePage() throws LscServiceException {
        when(usersService.getPageSize()).thenReturn(PAGE_SIZE);
        MsGraphApiUsersSrcService testee = new MsGraphApiUsersSrcService(task);
        Map<String, LscDatasets> listPivots = testee.getListPivots();
        assertThat(listPivots.keySet().size()).isGreaterThan(PAGE_SIZE);
    }

    @Test
    void listPivotShouldReturnAllResultsWhenMoreThanOnePageWithDefaultPagination() throws LscServiceException {
        MsGraphApiUsersSrcService testee = new MsGraphApiUsersSrcService(task);
        Map<String, LscDatasets> listPivots = testee.getListPivots();
        assertThat(listPivots.keySet().size()).isGreaterThan(DEFAULT_PAGE_SIZE);
    }

    @Test
    void listPivotShouldReturnAllResultsWhenMoreThanThreePages() throws LscServiceException {
        when(usersService.getPageSize()).thenReturn(PAGE_SIZE);
        MsGraphApiUsersSrcService testee = new MsGraphApiUsersSrcService(task);
        Map<String, LscDatasets> listPivots = testee.getListPivots();
        assertThat(listPivots.keySet().size()).isGreaterThan(3 * PAGE_SIZE);
    }

    @Test
    void listPivotShouldThrowsWhenInvalidPageSize() throws LscServiceException {
        when(usersService.getPageSize()).thenReturn(1000);
        MsGraphApiUsersSrcService testee = new MsGraphApiUsersSrcService(task);
        assertThatThrownBy(testee::getListPivots).isInstanceOf(LscServiceException.class);
    }

    @Test
    void listPivotShouldReturnOneUserWhenOneResultWithConfiguredPivot() throws LscServiceException {
        when(usersService.getPivot()).thenReturn("id");
        MsGraphApiUsersSrcService testee = new MsGraphApiUsersSrcService(task);

        Map<String, LscDatasets> listPivots = testee.getListPivots();

        String first = listPivots.keySet().stream().findFirst().get();

        when(usersService.getFilter()).thenReturn("id eq '" + first + "'");
        testee = new MsGraphApiUsersSrcService(task);

        Map<String, LscDatasets> actual = testee.getListPivots();
        assertThat(actual).hasSize(1);
    }

}
