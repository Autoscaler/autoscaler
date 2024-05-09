/*
 * Copyright 2015-2024 Open Text.
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
package com.github.autoscaler.email.alert.dispatcher;

import com.github.autoscaler.api.ScalerException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;

public class EmailDispatcherTest {
    @Test
    public void testEmailDispatch() {
        EmailDispatcherConfiguration emailDispatcherConfiguration = Mockito.mock(EmailDispatcherConfiguration.class);
        when(emailDispatcherConfiguration.getPort()).thenReturn("80");
        when(emailDispatcherConfiguration.getEmailAddressTo()).thenReturn("rg@ot.com");
        when(emailDispatcherConfiguration.getHost()).thenReturn("dummyHost");
        when(emailDispatcherConfiguration.getUsername()).thenReturn("dummyUserName");
        when(emailDispatcherConfiguration.getPassword()).thenReturn(("dummyPassword"));
        try {
            EmailDispatcher emailDispatcher = new EmailDispatcher(emailDispatcherConfiguration);
            emailDispatcher.dispatch("hello, this is a test email");
        } catch (Exception e) {
            Assert.fail("Error while trying to dispatch mail.");
        }
    }
}
