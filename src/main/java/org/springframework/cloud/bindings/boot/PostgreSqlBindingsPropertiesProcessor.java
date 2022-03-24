/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.bindings.boot;

import org.springframework.cloud.bindings.Binding;
import org.springframework.cloud.bindings.Bindings;
import org.springframework.core.env.Environment;

import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.cloud.bindings.boot.Guards.isTypeEnabled;

/**
 * An implementation of {@link BindingsPropertiesProcessor} that detects {@link Binding}s of type: {@value TYPE}.
 *
 * @see <a href="https://jdbc.postgresql.org/documentation/80/connect.html">JDBC URL Format</a>
 */
public final class PostgreSqlBindingsPropertiesProcessor implements BindingsPropertiesProcessor {

    /**
     * The {@link Binding} type that this processor is interested in: {@value}.
     **/
    public static final String TYPE = "postgresql";
    public static final String SSL_MODE = "sslmode";
    public static final String SSL_ROOT_CERT = "sslrootcert";
    public static final String OPTIONS = "options";

    @Override
    public void process(Environment environment, Bindings bindings, Map<String, Object> properties) {
        if (!isTypeEnabled(environment, TYPE)) {
            return;
        }

        bindings.filterBindings(TYPE).forEach(binding -> {
            MapMapper map = new MapMapper(binding.getSecret(), properties);
            //jdbc properties
            map.from("password").to("spring.datasource.password");
            map.from("host", "port", "database").to("spring.datasource.url",
                    (host, port, database) -> String.format("jdbc:postgresql://%s:%s/%s", host, port, database));
            String sslOptions = buildSslModeAndOptions(binding);
            if (!"".equals(sslOptions)) {
                properties.put("spring.datasource.url", properties.get("spring.datasource.url") + "?" + sslOptions);
            }

            map.from("username").to("spring.datasource.username");

            // jdbcURL takes precedence
            map.from("jdbc-url").to("spring.datasource.url");

            properties.put("spring.datasource.driver-class-name", "org.postgresql.Driver");

            //r2dbc properties
            map.from("password").to("spring.r2dbc.password");
            map.from("host", "port", "database").to("spring.r2dbc.url",
                    (host, port, database) -> String.format("r2dbc:postgresql://%s:%s/%s", host, port, database));
            map.from("username").to("spring.r2dbc.username");

            // r2dbcURL takes precedence
            map.from("r2dbc-url").to("spring.r2dbc.url");
        });
    }

    private String buildSslModeAndOptions(Binding binding) {
        //process ssl params
        //https://www.postgresql.org/docs/14/libpq-connect.html
        String sslmode = binding.getSecret().getOrDefault(SSL_MODE, "");
        String sslRootCert = binding.getSecret().getOrDefault(SSL_ROOT_CERT, "");
        StringBuilder sslparam = new StringBuilder();
        if (!"".equals(sslmode)) {
            sslparam.append(SSL_MODE).append("=").append(sslmode);
        }
        if (!"".equals(sslRootCert)) {
            if (!"".equals(sslmode)) {
                sslparam.append("&");
            }
            sslparam.append(SSL_ROOT_CERT).append("=")
                    .append(binding.getPath()).append(FileSystems.getDefault().getSeparator())
                    .append(sslRootCert);
        }
        //cockroachdb cloud uses options parameter to pass in the cluster routing-id
        //https://www.cockroachlabs.com/docs/v21.2/connection-parameters#additional-connection-parameters
        String options = binding.getSecret().getOrDefault(OPTIONS, "");
        String crdbOption = "";
        List<String> postgreOptions = new ArrayList<>();
        if (!options.equals("")) {
            String[] allOpts = options.split("&");
            for (String o : allOpts) {
                String[] keyval = o.split("=");
                if (keyval.length != 2 || keyval[0].length() == 0 || keyval[1].length() == 0) {
                    continue;
                }
                if (keyval[0].equals("--cluster")) {
                    crdbOption = keyval[0] + "=" + keyval[1];
                } else {
                    postgreOptions.add("-c " + keyval[0] + "=" + keyval[1]);
                }
            }
        }
        String combinedOptions = crdbOption;
        if (postgreOptions.size() > 0) {
            String otherOpts = String.join(" ", postgreOptions);
            if (!combinedOptions.equals("")) {
                combinedOptions = combinedOptions + " " + otherOpts;
            } else {
                combinedOptions = otherOpts;
            }
        }
        if (!"".equals(combinedOptions)) {
            combinedOptions = "options=" + combinedOptions;
        }
        if (sslparam.length() > 0 && !combinedOptions.equals("")) {
            combinedOptions = sslparam + "&" + combinedOptions;
        } else if (sslparam.length() > 0) {
            combinedOptions = sslparam.toString();
        }
        return combinedOptions;
    }

}
