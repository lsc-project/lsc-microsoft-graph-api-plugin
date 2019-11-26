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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.lsc.plugins.connectors.msgraphapi.beans.AuthenticationResponse;

import com.auth0.jwt.JWT;

class MsGraphApiAuthenticationTest {

    private final static String CLIENT_ID = System.getenv("TEST_MS_GRAPH_API_CLIENT_ID");
    private final static String CLIENT_SECRET = System.getenv("TEST_MS_GRAPH_API_CLIENT_SECRET");
    private final static String TENANT = System.getenv("TEST_MS_GRAPH_API_TENANT");

    private final MsGraphApiAuthentication msGraphApiAuthentication;

    MsGraphApiAuthenticationTest() {
        msGraphApiAuthentication = new MsGraphApiAuthentication();
    }

    @Test
    void shouldObtainValidAccessToken() throws AuthorizationException {
        AuthenticationResponse response = msGraphApiAuthentication.authenticate(TENANT, CLIENT_ID, CLIENT_SECRET);
        assertThat(response.getAccessToken()).isNotBlank();
        assertThatCode(() -> JWT.decode(response.getAccessToken())).doesNotThrowAnyException();
    }

    @Test
    void shouldThrowIfInvalidTenant() {
        assertThatThrownBy(() -> msGraphApiAuthentication.authenticate("NOT_A_TENANT", CLIENT_ID, CLIENT_SECRET)).isInstanceOf(AuthorizationException.class);
    }

    @Test
    void shouldThrowIfInvalidClientId() {
        assertThatThrownBy(() -> msGraphApiAuthentication.authenticate(TENANT, "NOT_A_CLIENT_ID", CLIENT_SECRET)).isInstanceOf(AuthorizationException.class);
    }

    @Test
    void shouldThrowIfInvalidClientSecret() {
        assertThatThrownBy(() -> msGraphApiAuthentication.authenticate(TENANT, CLIENT_ID, "NOT_A_SECRET")).isInstanceOf(AuthorizationException.class);
    }

}
