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

import static org.lsc.plugins.connectors.msgraphapi.MsGraphApiDao.ID;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.lsc.LscDatasets;
import org.lsc.beans.IBean;
import org.lsc.configuration.TaskType;
import org.lsc.exception.LscServiceCommunicationException;
import org.lsc.exception.LscServiceConfigurationException;
import org.lsc.exception.LscServiceException;
import org.lsc.plugins.connectors.msgraphapi.beans.User;
import org.lsc.plugins.connectors.msgraphapi.generated.MsGraphApiConnectionType;
import org.lsc.plugins.connectors.msgraphapi.generated.MsGraphApiUsersService;
import org.lsc.service.IService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

public class MsGraphApiUsersSrcService implements IService {

    protected static final Logger LOGGER = LoggerFactory.getLogger(MsGraphApiUsersSrcService.class);
    /**
     * Preceding the object feeding, it will be instantiated from this class.
     */
    private final Class<IBean> beanClass;

    private final MsGraphApiUsersService service;
    private final MsGraphApiDao dao;
    private final MsGraphApiConnectionType connection;

    public MsGraphApiUsersSrcService(TaskType task) throws LscServiceConfigurationException {
        try {
            if (task.getPluginSourceService().getAny() == null || task.getPluginSourceService().getAny().size() != 1 || !((task.getPluginSourceService().getAny().get(0) instanceof MsGraphApiUsersService))) {
                throw new LscServiceConfigurationException("Unable to identify the James service configuration " + "inside the plugin source node of the task: " + task.getName());
            }

            service = (MsGraphApiUsersService) task.getPluginSourceService().getAny().get(0);
            beanClass = (Class<IBean>) Class.forName(task.getBean());
            connection = (MsGraphApiConnectionType) service.getConnection().getReference();

            String token = new MsGraphApiAuthentication()
                .authenticate(connection.getTenant(), connection.getClientId(), connection.getClientSecret())
                .getAccessToken();

            dao = new MsGraphApiDao(token, service);

        } catch (ClassNotFoundException | AuthorizationException e) {
            throw new LscServiceConfigurationException(e);
        }
    }

    @Override
    public IBean getBean(String pivoteAttributeName, LscDatasets pivotAttributes, boolean fromSameService) throws LscServiceException {
        LOGGER.debug(String.format("Call to getBean(%s, %s, %b)", pivoteAttributeName, pivotAttributes, fromSameService));
        if (pivotAttributes.getAttributesNames().size() < 1) {
            return null;
        }
        String pivotAttribute = pivotAttributes.getAttributesNames().get(0);
        String pivotValue = pivotAttributes.getStringValueAttribute(pivotAttribute);
        String idValue = pivotAttributes.getStringValueAttribute(ID);
        if (idValue == null) {
            return null;
        }
        try {
            Map<String, Object> user = dao.getUserDetails(idValue);
            return mapToBean(idValue, user);
        } catch (ProcessingException e) {
            LOGGER.error(String.format("ProcessingException while getting bean %s/%s with id %s (%s)",
                pivoteAttributeName, pivotValue, idValue, e));
            LOGGER.error(e.toString(), e);
            throw new LscServiceCommunicationException(e);
        } catch (NotFoundException e) {
            LOGGER.debug(String.format("%s/%s with id %s not found", pivoteAttributeName, idValue, pivotValue));
            return null;
        } catch (WebApplicationException e) {
            LOGGER.error(String.format("WebApplicationException while getting bean %s/%s with id %s (%s)",
                pivoteAttributeName, pivotValue, idValue, e));
            LOGGER.debug(e.toString(), e);
            throw new LscServiceException(e);
        } catch (InstantiationException | IllegalAccessException e) {
            LOGGER.error("Bad class name: " + beanClass.getName() + "(" + e + ")");
            LOGGER.debug(e.toString(), e);
            throw new LscServiceException(e);
        }
    }

    @VisibleForTesting
    IBean mapToBean(String idValue, Map<String, Object> user) throws InstantiationException, IllegalAccessException {
        IBean bean = beanClass.newInstance();

        bean.setMainIdentifier(idValue);
        bean.setDatasets(new LscDatasets(user));
        return bean;
    }

    @Override
    public Map<String, LscDatasets> getListPivots() throws LscServiceException {
        try {
            List<User> userList = dao.getUsersList();

            Map<String, LscDatasets> listPivots = new HashMap<String, LscDatasets>();
            for (User user: userList) {
                listPivots.put(user.getValue(), user.toDatasets());
            }
            return ImmutableMap.copyOf(listPivots);
        } catch (ProcessingException e) {
            LOGGER.error(String.format("ProcessingException while getting pivot list (%s)", e));
            LOGGER.debug(e.toString(), e);
            throw new LscServiceCommunicationException(e);
        } catch (WebApplicationException e) {
            LOGGER.error(String.format("WebApplicationException while getting pivot list (%s)", e));
            LOGGER.debug(e.toString(), e);
            throw new LscServiceException(e);
        }
    }
}
