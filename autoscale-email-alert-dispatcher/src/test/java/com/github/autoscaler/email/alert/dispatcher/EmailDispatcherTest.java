package com.github.autoscaler.email.alert.dispatcher;

import com.github.autoscaler.api.ScalerException;
import org.junit.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;

public class EmailDispatcherTest {
    @Test
    public void testEmail() throws ScalerException, ScalerException {
        EmailDispatcherConfiguration emailDispatcherConfiguration = Mockito.mock(EmailDispatcherConfiguration.class);
        when(emailDispatcherConfiguration.getPort()).thenReturn("80");
        when(emailDispatcherConfiguration.getEmailAddressTo()).thenReturn("rgopalakris3@opentext.com");
        when(emailDispatcherConfiguration.getHost()).thenReturn("dummy");
        when(emailDispatcherConfiguration.getUsername()).thenReturn("dummy");
        when(emailDispatcherConfiguration.getPassword()).thenReturn(("dummy"));
        EmailDispatcher emailDispatcher = new EmailDispatcher(emailDispatcherConfiguration);
        emailDispatcher.dispatch("hello");
    }
}
