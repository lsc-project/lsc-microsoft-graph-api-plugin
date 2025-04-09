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

import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.LinkedHashMap;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;

import org.lsc.LscDatasets;
import org.lsc.beans.IBean;
import org.lsc.configuration.ConnectionType;
import org.lsc.configuration.PluginConnectionType;
import org.lsc.configuration.TaskType;
import org.lsc.exception.LscServiceCommunicationException;
import org.lsc.exception.LscServiceConfigurationException;
import org.lsc.exception.LscServiceException;
import org.lsc.plugins.connectors.msgraphapi.beans.User;
import org.lsc.plugins.connectors.msgraphapi.generated.MsGraphApiConnectionSettings;
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
    private final MsGraphApiConnectionSettings settings;

    public MsGraphApiUsersSrcService(TaskType task) throws LscServiceConfigurationException {
        try {
            if (task.getPluginSourceService().getAny() == null || task.getPluginSourceService().getAny().size() != 1 || !((task.getPluginSourceService().getAny().get(0) instanceof MsGraphApiUsersService))) {
                throw new LscServiceConfigurationException("Unable to identify the msgraphapi service configuration " + "inside the plugin source node of the task: " + task.getName());
            }

            service = (MsGraphApiUsersService) task.getPluginSourceService().getAny().get(0);
            beanClass = (Class<IBean>) Class.forName(task.getBean());
            if (task.getPluginSourceService().getConnection() == null || task.getPluginSourceService().getConnection().getReference() == null || ! (task.getPluginSourceService().getConnection().getReference() instanceof PluginConnectionType)) {
                throw new LscServiceConfigurationException("Unable to identify the msgraphapi service connection " + "inside the plugin source node of the task: " + task.getName());
            }
            PluginConnectionType pluginConnectionType = (PluginConnectionType)task.getPluginSourceService().getConnection().getReference();
            if (pluginConnectionType.getAny() == null || pluginConnectionType.getAny().size() != 1 || !(pluginConnectionType.getAny().get(0) instanceof MsGraphApiConnectionSettings)) {
                throw new LscServiceConfigurationException("Unable to identify the msgraphapi connection settings " + "inside the connection node of the task: " + task.getName());
            }
            settings = (MsGraphApiConnectionSettings) pluginConnectionType.getAny().get(0);

            String token = new MsGraphApiAuthentication()
                .authenticate(settings.getTenant(), settings.getAuthenticationURL(), settings.getScope(), settings.getClientId(), settings.getClientSecret())
                .getAccessToken();

            dao = new MsGraphApiDao(token, settings, service);

        } catch (ClassNotFoundException | AuthorizationException e) {
            throw new LscServiceConfigurationException(e);
        }
    }

    @Override
    public IBean getBean(String pivotAttributeName, LscDatasets pivotAttributes, boolean fromSameService) throws LscServiceException {
        LOGGER.debug(String.format("Call to getBean(%s, %s, %b)", pivotAttributeName, pivotAttributes, fromSameService));
        if (pivotAttributes.getAttributesNames().size() < 1) {
            return null;
        }
        String pivotAttribute = pivotAttributes.getAttributesNames().get(0);
        String pivotValue = pivotAttributes.getStringValueAttribute(pivotAttribute);
        if (fromSameService) {
            return getBeanFromSameService(pivotAttributeName, pivotAttributes, pivotValue);
        } else {
            return getBeanForClean(pivotAttributeName, pivotValue);
        }
    }

    private IBean getBeanForClean(String pivotAttributeName, String pivotValue) throws LscServiceException {
        try {
            Optional<User> maybeUser = dao.getFirstUserWithId(pivotValue);
            if (maybeUser.isPresent()) {
                return userIdToBean(maybeUser.get().getId());
            } else {
                return null;
            }
        } catch (ProcessingException e) {
            LOGGER.error(String.format("ProcessingException while getting bean %s/%s (%s)",
                pivotAttributeName, pivotValue, e));
            LOGGER.error(e.toString(), e);
            throw new LscServiceCommunicationException(e);
        } catch (NotFoundException e) {
            LOGGER.debug(String.format("%s/%s not found", pivotAttributeName, pivotValue));
            return null;
        } catch (WebApplicationException e) {
            LOGGER.error(String.format("WebApplicationException while getting bean %s/%s (%s)",
                pivotAttributeName, pivotValue, e));
            LOGGER.debug(e.toString(), e);
            throw new LscServiceException(e);
        } catch (InstantiationException | IllegalAccessException e) {
            LOGGER.error("Bad class name: " + beanClass.getName() + "(" + e + ")");
            LOGGER.debug(e.toString(), e);
            throw new LscServiceException(e);
        }
    }

    private IBean getBeanFromSameService(String pivotAttributeName, LscDatasets pivotAttributes, String pivotValue) throws LscServiceException {
        String idValue = pivotAttributes.getStringValueAttribute(ID);
        if (idValue == null) {
            return null;
        }
        try {
            Map<String, Object> user = dao.getUserDetails(idValue);
            return mapToBean(idValue, user);
        } catch (ProcessingException e) {
            LOGGER.error(String.format("ProcessingException while getting bean %s/%s with id %s (%s)",
                pivotAttributeName, pivotValue, idValue, e));
            LOGGER.error(e.toString(), e);
            throw new LscServiceCommunicationException(e);
        } catch (NotFoundException e) {
            LOGGER.debug(String.format("%s/%s with id %s not found", pivotAttributeName, idValue, pivotValue));
            return null;
        } catch (WebApplicationException e) {
            LOGGER.error(String.format("WebApplicationException while getting bean %s/%s with id %s (%s)",
                pivotAttributeName, pivotValue, idValue, e));
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

        LscDatasets datasets = new LscDatasets();
        /* In new version, graphApi uses LinkedHashMap values
         * like: Key={childKey=value, childKey2=value2, ...}
         * Then in this case you could retrieve in bean
         * attribute values as "<Key>/<childKey>" = "<childValue>".
         * ex: "onPremisesExtensionAttributes/extensionAttribute1" = "toto"
        */
        for (Map.Entry entry : user.entrySet()) {
            if (entry.getValue() instanceof java.util.LinkedHashMap) {
                LinkedHashMap innerMap = (LinkedHashMap) entry.getValue();
                Set<String> keys = innerMap.keySet();
                for ( String key : keys ) {
                    datasets.put(entry.getKey() + "/" + key,
                        innerMap.get(key) == null ? new LinkedHashSet<>() : innerMap.get(key));
                }
            }
            else {
                datasets.put((String)entry.getKey(),
                    entry.getValue() == null ? new LinkedHashSet<>() : entry.getValue());
            }
        }

        bean.setDatasets(datasets);

        return bean;
    }

    private IBean userIdToBean(String idValue) throws InstantiationException, IllegalAccessException {
        IBean bean = beanClass.newInstance();

        bean.setMainIdentifier(idValue);
        bean.setDatasets(new LscDatasets(ImmutableMap.of("id", idValue)));
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

    public Collection<Class<? extends ConnectionType>> getSupportedConnectionType() {
        Collection<Class<? extends ConnectionType>> list = new ArrayList<Class<? extends ConnectionType>>();
        return list;
    }

}
