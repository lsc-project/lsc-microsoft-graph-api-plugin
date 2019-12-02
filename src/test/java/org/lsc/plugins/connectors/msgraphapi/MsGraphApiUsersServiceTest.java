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
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.assertj.core.util.Maps;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.lsc.LscDatasets;
import org.lsc.beans.IBean;
import org.lsc.configuration.PluginConnectionType;
import org.lsc.configuration.PluginSourceServiceType;
import org.lsc.configuration.ServiceType;
import org.lsc.configuration.TaskType;
import org.lsc.exception.LscServiceException;
import org.lsc.plugins.connectors.msgraphapi.generated.MsGraphApiConnectionSettings;
import org.lsc.plugins.connectors.msgraphapi.generated.MsGraphApiUsersService;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

class MsGraphApiUsersServiceTest {
    private static final boolean FROM_SAME_SERVICE = true;

    private static TaskType task;
    private MsGraphApiUsersService usersService;
    private static PluginSourceServiceType pluginSourceService;
    private static int DEFAULT_PAGE_SIZE = 100;
    private static int PAGE_SIZE = 300;
    private static MsGraphApiConnectionSettings connectionSettings;

    @BeforeAll
    static void setup() throws AuthorizationException {
        String clientId = System.getenv("TEST_MS_GRAPH_API_CLIENT_ID");
        String clientSecret = System.getenv("TEST_MS_GRAPH_API_CLIENT_SECRET");
        String tenant = System.getenv("TEST_MS_GRAPH_API_TENANT");

        assumeTrue(StringUtils.isNotBlank(clientId));
        assumeTrue(StringUtils.isNotBlank(clientSecret));
        assumeTrue(StringUtils.isNotBlank(tenant));

        pluginSourceService = mock(PluginSourceServiceType.class);
        connectionSettings = mock(MsGraphApiConnectionSettings.class);
        task = mock(TaskType.class);
        PluginConnectionType connectionType = mock(PluginConnectionType.class);
        ServiceType.Connection connection = mock(ServiceType.Connection.class);

        when(connectionType.getAny()).thenReturn(ImmutableList.of(connectionSettings));
        when(connection.getReference()).thenReturn(connectionType);
        when(pluginSourceService.getConnection()).thenReturn(connection);
        when(connectionSettings.getClientId()).thenReturn(clientId);
        when(connectionSettings.getClientSecret()).thenReturn(clientSecret);
        when(connectionSettings.getTenant()).thenReturn(tenant);
        when(task.getBean()).thenReturn("org.lsc.beans.SimpleBean");
        when(task.getPluginSourceService()).thenReturn(pluginSourceService);
    }

    @BeforeEach
    void defineService() {
        usersService = mock(MsGraphApiUsersService.class);
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

    @Test
    void getBeanShouldReturnNullWhenEmptyDataset() throws Exception {
        MsGraphApiUsersSrcService testee = new MsGraphApiUsersSrcService(task);

        assertThat(testee.getBean("id", new LscDatasets(), FROM_SAME_SERVICE)).isNull();
    }

    @Test
    void getBeanShouldReturnNullWhenNoMatchingMail() throws Exception {
        MsGraphApiUsersSrcService testee = new MsGraphApiUsersSrcService(task);

        LscDatasets nonExistingIdDataset = new LscDatasets(ImmutableMap.of("mail", "502e48b1-40e7-4c6c-91ec-13a51b679849"));
        assertThat(testee.getBean("mail", nonExistingIdDataset, FROM_SAME_SERVICE))
            .isNull();
    }

    @Test
    void getBeanShouldReturnNullWhenNoMatchingId() throws Exception {
        when(usersService.getPivot()).thenReturn("id");
        MsGraphApiUsersSrcService testee = new MsGraphApiUsersSrcService(task);

        LscDatasets nonExistingIdDataset = new LscDatasets(ImmutableMap.of("id", "nonExistingId"));
        assertThat(testee.getBean("id", nonExistingIdDataset, FROM_SAME_SERVICE))
            .isNull();
    }

    @Test
    void getBeanShouldReturnMainIdentifierSetToIdWhenDefaultPivot() throws Exception {
        MsGraphApiUsersSrcService testee = new MsGraphApiUsersSrcService(task);

        Map<String, LscDatasets> pivots = testee.getListPivots();
        String firstUserPivotValue = pivots.keySet().stream().findFirst().get();
        IBean bean = testee.getBean("mail", pivots.get(firstUserPivotValue), FROM_SAME_SERVICE);

        assertThat(bean.getMainIdentifier()).isEqualTo(pivots.get(firstUserPivotValue).getStringValueAttribute("id"));
    }

    @Test
    void getBeanShouldReturnMainIdentifierSetToIdWhenMailAsPivot() throws Exception {
        when(usersService.getPivot()).thenReturn("mail");
        MsGraphApiUsersSrcService testee = new MsGraphApiUsersSrcService(task);

        Map<String, LscDatasets> pivots = testee.getListPivots();
        String firstUserPivotValue = pivots.keySet().stream().findFirst().get();
        IBean bean = testee.getBean("mail", pivots.get(firstUserPivotValue), FROM_SAME_SERVICE);

        assertThat(bean.getMainIdentifier()).isEqualTo(pivots.get(firstUserPivotValue).getStringValueAttribute("id"));
    }

    @Test
    void getBeanShouldReturnMainIdentifierSetToIdWhenIdAsAPivot() throws Exception {
        when(usersService.getPivot()).thenReturn("id");
        MsGraphApiUsersSrcService testee = new MsGraphApiUsersSrcService(task);

        Map<String, LscDatasets> pivots = testee.getListPivots();
        String firstUserPivotValue = pivots.keySet().stream().findFirst().get();
        IBean bean = testee.getBean("id", pivots.get(firstUserPivotValue), FROM_SAME_SERVICE);

        assertThat(bean.getMainIdentifier()).isEqualTo(pivots.get(firstUserPivotValue).getStringValueAttribute("id"));
    }

    @Test
    void getBeanShouldReturnIdAndMail() throws Exception {
        MsGraphApiUsersSrcService testee = new MsGraphApiUsersSrcService(task);

        Map<String, LscDatasets> pivots = testee.getListPivots();
        String firstUserPivotValue = pivots.keySet().stream().findFirst().get();
        IBean bean = testee.getBean("mail", pivots.get(firstUserPivotValue), FROM_SAME_SERVICE);

        assertThat(bean.getDatasetFirstValueById("id")).isEqualTo(pivots.get(firstUserPivotValue).getStringValueAttribute("id"));
        assertThat(bean.getDatasetFirstValueById("mail")).isEqualTo(pivots.get(firstUserPivotValue).getStringValueAttribute("mail"));
    }

    @Test
    void getBeanShouldReturnIdAndMailWhenMailPivot() throws Exception {
        when(usersService.getPivot()).thenReturn("mail");
        MsGraphApiUsersSrcService testee = new MsGraphApiUsersSrcService(task);

        Map<String, LscDatasets> pivots = testee.getListPivots();
        String firstUserPivotValue = pivots.keySet().stream().findFirst().get();
        IBean bean = testee.getBean("mail", pivots.get(firstUserPivotValue), FROM_SAME_SERVICE);

        assertThat(bean.getDatasetFirstValueById("id")).isEqualTo(pivots.get(firstUserPivotValue).getStringValueAttribute("id"));
        assertThat(bean.getDatasetFirstValueById("mail")).isEqualTo(pivots.get(firstUserPivotValue).getStringValueAttribute("mail"));
    }

    @Test
    void getBeanShouldReturnIdAndMailWhenIdPivot() throws Exception {
        when(usersService.getPivot()).thenReturn("id");
        MsGraphApiUsersSrcService testee = new MsGraphApiUsersSrcService(task);

        Map<String, LscDatasets> pivots = testee.getListPivots();
        String firstUserPivotValue = pivots.keySet().stream().findFirst().get();
        IBean bean = testee.getBean("id", pivots.get(firstUserPivotValue), FROM_SAME_SERVICE);

        assertThat(bean.getDatasetFirstValueById("id")).isEqualTo(pivots.get(firstUserPivotValue).getStringValueAttribute("id"));
        assertThat(bean.getDatasetFirstValueById("mail")).isNotBlank();
    }

    @Test
    void getBeanShouldReturnBusinessPhonesAsACollection() throws Exception {
        MsGraphApiUsersSrcService testee = new MsGraphApiUsersSrcService(task);

        Map<String, LscDatasets> pivots = testee.getListPivots();
        String firstUserPivotValue = pivots.keySet().stream().findFirst().get();
        IBean bean = testee.getBean("mail", pivots.get(firstUserPivotValue), FROM_SAME_SERVICE);

        assertThat(bean.getDatasetById("businessPhones")).isEmpty();
    }

    @Test
    void mapToBeanShouldPreserveEmptyArray() throws Exception{
        MsGraphApiUsersSrcService testee = new MsGraphApiUsersSrcService(task);

        IBean bean = testee.mapToBean("id", ImmutableMap.of("businessPhones", ImmutableList.of()));

        assertThat(bean.getDatasetById("businessPhones")).isEmpty();
    }

    @Test
    void mapToBeanShouldPreserveTwoValues() throws Exception{
        MsGraphApiUsersSrcService testee = new MsGraphApiUsersSrcService(task);

        IBean bean = testee.mapToBean("id", ImmutableMap.of("businessPhones", ImmutableList.of("123", "456")));

        assertThat(bean.getDatasetById("businessPhones")).hasSize(2);
    }

    @Test
    void mapToBeanShouldPreserveNull() throws Exception{
        MsGraphApiUsersSrcService testee = new MsGraphApiUsersSrcService(task);

        IBean bean = testee.mapToBean("id", Maps.newHashMap("givenName", null));

        assertThat(bean.getDatasetById("givenName")).isNull();
    }

    @Test
    void getBeanShouldNotReturnIdWhenSelectDoesntContainIdField() throws Exception {
        when(usersService.getSelect()).thenReturn("mail,mobilePhone");
        MsGraphApiUsersSrcService testee = new MsGraphApiUsersSrcService(task);

        Map<String, LscDatasets> pivots = testee.getListPivots();
        String firstUserPivotValue = pivots.keySet().stream().findFirst().get();
        IBean bean = testee.getBean("mail", pivots.get(firstUserPivotValue), FROM_SAME_SERVICE);

        assertThat(bean.getDatasetById("id")).isNull();
        assertThat(bean.getDatasetFirstValueById("mail")).isEqualTo(firstUserPivotValue);
    }

    @Test
    void getBeanShouldReturnIdAndUserPrincipalNameWhenSelectContainsThem() throws Exception {
        when(usersService.getSelect()).thenReturn("id,userPrincipalName");
        MsGraphApiUsersSrcService testee = new MsGraphApiUsersSrcService(task);

        Map<String, LscDatasets> pivots = testee.getListPivots();
        String firstUserPivotValue = pivots.keySet().stream().findFirst().get();
        IBean bean = testee.getBean("mail", pivots.get(firstUserPivotValue), FROM_SAME_SERVICE);

        assertThat(bean.getDatasetFirstValueById("id")).isNotBlank();
        assertThat(bean.getDatasetFirstValueById("userPrincipalName")).isNotBlank();
    }

    @Test
    void getBeanShouldReturnBeanWithIdWhenFromAnotherService() throws Exception {
        MsGraphApiUsersSrcService testee = new MsGraphApiUsersSrcService(task);

        Map<String, LscDatasets> pivots = testee.getListPivots();
        String firstUserPivotValue = pivots.keySet().stream().findFirst().get();
        LscDatasets datasets = new LscDatasets(ImmutableMap.of("mail", firstUserPivotValue));
        IBean bean = testee.getBean("mainIdentifierFromDestinationService", datasets, !FROM_SAME_SERVICE);

        assertThat(bean.getDatasetFirstValueById("id")).isNotBlank();
    }

    @Test
    void getBeanShouldReturnNullWhenNonExistingUserFromAnotherService() throws Exception {
        MsGraphApiUsersSrcService testee = new MsGraphApiUsersSrcService(task);

        LscDatasets datasets = new LscDatasets(ImmutableMap.of("mail", "5f8c4178-6ea7-465e-b5ab-ab6ae59e6ffe"));
        IBean bean = testee.getBean("mainIdentifierFromDestinationService", datasets, !FROM_SAME_SERVICE);

        assertThat(bean).isNull();
    }

    @Test
    void getBeanShouldReturnBeanWithIdWhenFromAnotherServiceAndIdPivot() throws Exception {
        when(usersService.getPivot()).thenReturn("id");
        MsGraphApiUsersSrcService testee = new MsGraphApiUsersSrcService(task);

        Map<String, LscDatasets> pivots = testee.getListPivots();
        String firstUserPivotValue = pivots.keySet().stream().findFirst().get();
        LscDatasets datasets = new LscDatasets(ImmutableMap.of("id", firstUserPivotValue));
        IBean bean = testee.getBean("mainIdentifierFromDestinationService", datasets, !FROM_SAME_SERVICE);

        assertThat(bean.getDatasetFirstValueById("id")).isNotBlank();
    }

    @Test
    void getBeanShouldReturnNullWhenNonExistingUserFromAnotherServiceAndIdPivot() throws Exception {
        when(usersService.getPivot()).thenReturn("id");
        MsGraphApiUsersSrcService testee = new MsGraphApiUsersSrcService(task);

        LscDatasets datasets = new LscDatasets(ImmutableMap.of("id", "655681ac-8515-4ecf-837e-d9b271910576"));
        IBean bean = testee.getBean("mainIdentifierFromDestinationService", datasets, !FROM_SAME_SERVICE);

        assertThat(bean).isNull();
    }

    @Test
    void getBeanShouldReturnBeanWithIdWhenFromAnotherServiceWithDifferentPivotName() throws Exception {
        when(usersService.getPivot()).thenReturn("mail");
        MsGraphApiUsersSrcService testee = new MsGraphApiUsersSrcService(task);

        Map<String, LscDatasets> pivots = testee.getListPivots();
        String firstUserPivotValue = pivots.keySet().stream().findFirst().get();
        LscDatasets datasets = new LscDatasets(ImmutableMap.of("email", firstUserPivotValue));
        IBean bean = testee.getBean("mainIdentifierFromDestinationService", datasets, !FROM_SAME_SERVICE);

        assertThat(bean.getDatasetFirstValueById("id")).isNotBlank();
    }

    @Test
    void getBeanShouldReturnBeanWithIdWhenFromAnotherServiceWithDifferentPivotNameAndDefaultPivot() throws Exception {
        MsGraphApiUsersSrcService testee = new MsGraphApiUsersSrcService(task);

        Map<String, LscDatasets> pivots = testee.getListPivots();
        String firstUserPivotValue = pivots.keySet().stream().findFirst().get();
        LscDatasets datasets = new LscDatasets(ImmutableMap.of("email", firstUserPivotValue));
        IBean bean = testee.getBean("mainIdentifierFromDestinationService", datasets, !FROM_SAME_SERVICE);

        assertThat(bean.getDatasetFirstValueById("id")).isNotBlank();
    }

    @Test
    void getBeanShouldReturnBeanWithIdWhenFromAnotherServiceWithDifferentPivotNameAndIdPivot() throws Exception {
        when(usersService.getPivot()).thenReturn("id");
        MsGraphApiUsersSrcService testee = new MsGraphApiUsersSrcService(task);

        Map<String, LscDatasets> pivots = testee.getListPivots();
        String firstUserPivotValue = pivots.keySet().stream().findFirst().get();
        LscDatasets datasets = new LscDatasets(ImmutableMap.of("uuid", firstUserPivotValue));
        IBean bean = testee.getBean("mainIdentifierFromDestinationService", datasets, !FROM_SAME_SERVICE);

        assertThat(bean.getDatasetFirstValueById("id")).isNotBlank();
    }
}
