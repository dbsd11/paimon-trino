/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.paimon.trino;

import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;
import io.airlift.bootstrap.Bootstrap;
import io.airlift.json.JsonModule;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.trino.filesystem.manager.FileSystemModule;
import io.trino.plugin.base.classloader.ClassLoaderSafeConnectorMetadata;
import io.trino.plugin.base.classloader.ClassLoaderSafeConnectorPageSinkProvider;
import io.trino.plugin.base.classloader.ClassLoaderSafeConnectorPageSourceProvider;
import io.trino.plugin.base.classloader.ClassLoaderSafeConnectorSplitManager;
import io.trino.spi.classloader.ThreadContextClassLoader;
import io.trino.spi.connector.Connector;
import io.trino.spi.connector.ConnectorContext;
import io.trino.spi.connector.ConnectorFactory;
import io.trino.spi.type.TypeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/** Trino {@link ConnectorFactory}. */
public class TrinoConnectorFactory implements ConnectorFactory {

    private static final Logger LOG = LoggerFactory.getLogger(TrinoConnectorFactory.class);

    @Override
    public String getName() {
        return "paimon";
    }

    @Override
    public Connector create(
            String catalogName, Map<String, String> config, ConnectorContext context) {
        return create(catalogName, config, context, new EmptyModule());
    }

    public Connector create(
            String catalogName,
            Map<String, String> config,
            ConnectorContext context,
            Module module) {
        config = new HashMap<>(config);

        ClassLoader classLoader = TrinoConnectorFactory.class.getClassLoader();
        try (ThreadContextClassLoader ignored = new ThreadContextClassLoader(classLoader)) {
            Bootstrap app =
                    new Bootstrap(
                            new JsonModule(),
                            new TrinoModule(config),
                            // bind the trino file system module
                            newFileSystemModule(catalogName, context),
                            binder -> {
                                binder.bind(TypeManager.class).toInstance(context.getTypeManager());
                                binder.bind(OpenTelemetry.class)
                                        .toInstance(context.getOpenTelemetry());
                                binder.bind(Tracer.class).toInstance(context.getTracer());
                            },
                            module);

            Injector injector =
                    app.doNotInitializeLogging()
                            .setRequiredConfigurationProperties(Map.of())
                            .setOptionalConfigurationProperties(config)
                            .initialize();

            TrinoMetadata trinoMetadata = injector.getInstance(TrinoMetadataFactory.class).create();
            TrinoSplitManager trinoSplitManager = injector.getInstance(TrinoSplitManager.class);
            TrinoPageSourceProvider trinoPageSourceProvider =
                    injector.getInstance(TrinoPageSourceProvider.class);
            TrinoPageSinkProvider trinoPageSinkProvider =
                    injector.getInstance(TrinoPageSinkProvider.class);
            TrinoNodePartitioningProvider trinoNodePartitioningProvider =
                    injector.getInstance(TrinoNodePartitioningProvider.class);
            TrinoSessionProperties trinoSessionProperties =
                    injector.getInstance(TrinoSessionProperties.class);
            TrinoTableOptions trinoTableOptions = injector.getInstance(TrinoTableOptions.class);

            return new TrinoConnector(
                    new ClassLoaderSafeConnectorMetadata(trinoMetadata, classLoader),
                    new ClassLoaderSafeConnectorSplitManager(trinoSplitManager, classLoader),
                    new ClassLoaderSafeConnectorPageSourceProvider(
                            trinoPageSourceProvider, classLoader),
                    new ClassLoaderSafeConnectorPageSinkProvider(
                            trinoPageSinkProvider, classLoader),
                    trinoNodePartitioningProvider,
                    trinoTableOptions,
                    trinoSessionProperties);
        }
    }

    /** Empty module for paimon connector factory. */
    public static class EmptyModule implements Module {
        @Override
        public void configure(Binder binder) {}
    }

    private static FileSystemModule newFileSystemModule(
            String catalogName, ConnectorContext context) {
        Constructor<?> constructor = FileSystemModule.class.getConstructors()[0];
        try {
            if (constructor.getParameterCount() == 0) {
                return (FileSystemModule) constructor.newInstance();
            } else {
                // for trino 440
                return (FileSystemModule)
                        constructor.newInstance(
                                catalogName, context.getNodeManager(), context.getOpenTelemetry());
            }
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
