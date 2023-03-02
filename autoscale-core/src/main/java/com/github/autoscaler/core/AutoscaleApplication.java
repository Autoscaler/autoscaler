/*
 * Copyright 2015-2023 Open Text.
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
package com.github.autoscaler.core;


import com.github.autoscaler.api.AlertDispatcherFactory;
import com.github.autoscaler.api.ScalerException;
import com.github.autoscaler.api.ServiceScaler;
import com.github.autoscaler.api.ServiceScalerProvider;
import com.github.autoscaler.api.ServiceSource;
import com.github.autoscaler.api.ServiceSourceProvider;
import com.github.autoscaler.api.WorkloadAnalyserFactory;
import com.github.autoscaler.api.WorkloadAnalyserFactoryProvider;
import com.hpe.caf.api.BootstrapConfiguration;
import com.hpe.caf.api.CafConfigurationDecoderProvider;
import com.hpe.caf.api.Cipher;
import com.hpe.caf.api.CipherException;
import com.hpe.caf.api.CipherProvider;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.ConfigurationDecoderProvider;
import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSourceProvider;
import com.hpe.caf.api.Decoder;
import com.hpe.caf.api.ElectionException;
import com.hpe.caf.api.ElectionFactory;
import com.hpe.caf.api.ElectionFactoryProvider;
import com.hpe.caf.api.ManagedConfigurationSource;
import com.hpe.caf.cipher.NullCipherProvider;
import com.hpe.caf.config.system.SystemBootstrapConfiguration;
import com.hpe.caf.election.NullElectionFactoryProvider;
import com.hpe.caf.naming.ServicePath;
import com.hpe.caf.util.ModuleLoader;
import com.hpe.caf.util.ModuleLoaderException;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.logging.LoggingUtil;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


/**
 * Wrapper around AutoscaleCore to expose functionality as a Dropwizard application.
 */
public class AutoscaleApplication extends Application<AutoscaleConfiguration>
{
    private static final Logger LOG = LoggerFactory.getLogger(AutoscaleApplication.class);


    /**
     * Standard entry point for an AutoscaleApplication.
     * @param args comamnd line arguments
     * @throws Exception if startup fails
     */
    public static void main(final String[] args)
            throws Exception
    {
        new AutoscaleApplication().run(args);
    }


    AutoscaleApplication() { }


    /**
     * Called upon startup. Determine required components from the classpath.
     * AutoscaleApplication requires the following advertised services on the classpath: a ConfigurationSourceProvider,
     * a ServiceSourceProvider, a ServiceScalerProvider, a Codec, an ElectionFactoryProvider, and at least one instance of a
     * WorkloadAnalyserFactoryProvider (but there can be more). This will create an instance of AutoscaleCore and set up health checks.
     * @param autoscaleConfiguration AutoscaleApplication configuration
     * @param environment to access health checks and metrics
     */
    @Override
    public void run(final AutoscaleConfiguration autoscaleConfiguration, final Environment environment)
        throws ScalerException, ConfigurationException, ModuleLoaderException, CipherException, ElectionException
    {
        LOG.info("Starting up");
        BootstrapConfiguration bootstrap = new SystemBootstrapConfiguration();
        ServicePath servicePath = bootstrap.getServicePath();
        Codec codec = ModuleLoader.getService(Codec.class);
        Cipher cipher = ModuleLoader.getService(CipherProvider.class, NullCipherProvider.class).getCipher(bootstrap);
        ConfigurationDecoderProvider decoderProvider = ModuleLoader.getService(ConfigurationDecoderProvider.class,
                                                                               CafConfigurationDecoderProvider.class);
        Decoder decoder = decoderProvider.getDecoder(bootstrap, codec);
        ManagedConfigurationSource config = ModuleLoader.getService(ConfigurationSourceProvider.class).getConfigurationSource(bootstrap, cipher, servicePath, decoder);
        ServiceSource source = ModuleLoader.getService(ServiceSourceProvider.class).getServiceSource(config, servicePath);
        ServiceScaler scaler = ModuleLoader.getService(ServiceScalerProvider.class).getServiceScaler(config);
        ElectionFactory electionFactory = ModuleLoader.getService(ElectionFactoryProvider.class, NullElectionFactoryProvider.class).getElectionManager( config);
        Collection<WorkloadAnalyserFactoryProvider> workloadProviders = ModuleLoader.getServices(WorkloadAnalyserFactoryProvider.class);
        Collection<AlertDispatcherFactory> alertDispatcherFactories = ModuleLoader.getServices(AlertDispatcherFactory.class);
        ScheduledExecutorService scheduler = getDefaultScheduledExecutorService(autoscaleConfiguration.getExecutorThreads());
        AutoscaleCore core = new AutoscaleCore(config, source, scaler, workloadProviders, electionFactory, scheduler, servicePath,
                                               alertDispatcherFactories);

        registerHealthChecks(environment, source, scaler, core);
        core.start(autoscaleConfiguration.getSourceRefreshPeriod());
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                core.shutdown();
                config.shutdown();
                scheduler.shutdownNow();
            }
        });
    }

    @Override
    public void initialize(Bootstrap<AutoscaleConfiguration> bootstrap)
    {
        bootstrap.setConfigurationSourceProvider(
            new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false, true))
        );
    }

    /**
     * Get a default implementation of a ScheduledExecutorService as used by the autoscaler.
     * @param nThreads the number of threads to make available in the thread pool
     * @return the default instance of a ScheduledExecutorService as used by the autoscale application
     */
    public static ScheduledExecutorService getDefaultScheduledExecutorService(final int nThreads)
    {
        return Executors.newScheduledThreadPool(nThreads);
    }


    private void registerHealthChecks(final Environment environment, final ServiceSource source, final ServiceScaler scaler, final AutoscaleCore core)
    {
        environment.healthChecks().register("source", new ScalerHealthCheck(source));
        environment.healthChecks().register("scaler", new ScalerHealthCheck(scaler));
        for ( Map.Entry<String, WorkloadAnalyserFactory> entry : core.getAnalyserFactoryMap().entrySet() ) {
            environment.healthChecks().register("workload." + entry.getKey(), new ScalerHealthCheck(entry.getValue()));
        }
        environment.healthChecks().register("scheduler",
            new ScalerHealthCheck(core.getAutoScaleScheduler()));
    }
    
    @Override
    protected void bootstrapLogging()
    {
        LoggingUtil.hijackJDKLogging();
    }
}
