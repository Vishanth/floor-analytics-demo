/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * you may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.iot.senseme.api.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.core.util.Utils;
import org.wso2.iot.senseme.api.constants.DeviceTypeConstants;
import org.wso2.iot.senseme.api.exception.DeviceTypeException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Contains utility methods used by senseme plugin.
 */
public class DeviceTypeUtils {

    private static Log log = LogFactory.getLog(DeviceTypeUtils.class);

    public static void cleanupResources(Connection conn, PreparedStatement stmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                log.warn("Error occurred while closing result set", e);
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                log.warn("Error occurred while closing prepared statement", e);
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                log.warn("Error occurred while closing database connection", e);
            }
        }
    }

    public static void cleanupResources(PreparedStatement stmt, ResultSet rs, Connection conn) {
        cleanupResources(conn, stmt, rs);
    }

    public static void setupDeviceManagementSchema() throws DeviceTypeException {
        try {
            Context ctx = new InitialContext();
            DataSource dataSource = (DataSource) ctx.lookup(DeviceTypeConstants.DATA_SOURCE_NAME);
            DeviceSchemaInitializer initializer =
                    new DeviceSchemaInitializer(dataSource);
            log.info("Initializing device management repository database schema");
            initializer.createRegistryDatabase();
        } catch (NamingException e) {
            log.error("Error while looking up the data source: " + DeviceTypeConstants.DATA_SOURCE_NAME);
        } catch (DeviceTypeException e) {
            throw new DeviceTypeException("Error occurred while initializing Iot Device " +
                    "Management database schema", e);
        } catch (Exception e) {
            log.error("Error while looking up the data source: " + DeviceTypeConstants.DATA_SOURCE_NAME);
        }
    }

    public static String replaceMqttProperty(String urlWithPlaceholders) {
        String MQTT_BROKER_HOST = null;
        String MQTT_PORT = null;
        if(!DeviceTypeConstants.MQTT_BROKER_HOST.startsWith("$")){
            MQTT_BROKER_HOST = "\\$".concat(DeviceTypeConstants.MQTT_BROKER_HOST);
        }
        if(!DeviceTypeConstants.MQTT_PORT.startsWith("$")){
            MQTT_PORT = "\\$".concat(DeviceTypeConstants.MQTT_PORT);
        }
        urlWithPlaceholders = Utils.replaceSystemProperty(urlWithPlaceholders);
        urlWithPlaceholders = urlWithPlaceholders.replaceAll(MQTT_PORT, "" +
                (DeviceTypeConstants.DEFAULT_MQTT_PORT + getPortOffset()));
        urlWithPlaceholders = urlWithPlaceholders.replaceAll(MQTT_BROKER_HOST,
                System.getProperty(DeviceTypeConstants.DEFAULT_CARBON_LOCAL_IP_PROPERTY, "localhost"));
        return urlWithPlaceholders;
    }

    private static int getPortOffset() {
        ServerConfiguration carbonConfig = ServerConfiguration.getInstance();
        String portOffset = System.getProperty("portOffset", carbonConfig.getFirstProperty(
                DeviceTypeConstants.CARBON_CONFIG_PORT_OFFSET));
        try {
            if ((portOffset != null)) {
                return Integer.parseInt(portOffset.trim());
            } else {
                return DeviceTypeConstants.CARBON_DEFAULT_PORT_OFFSET;
            }
        } catch (NumberFormatException e) {
            return DeviceTypeConstants.CARBON_DEFAULT_PORT_OFFSET;
        }
    }

}