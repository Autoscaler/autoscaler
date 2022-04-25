/*
 * Copyright 2015-2022 Micro Focus or one of its affiliates.
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

import com.github.autoscaler.api.AlertDispatcher;
import com.github.autoscaler.api.ScalerException;
import java.io.UnsupportedEncodingException;
import java.util.Objects;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailDispatcher implements AlertDispatcher
{
    private final String host;
    private final String port;
    private final String username;
    private final String password;
    private final String emailAddressTo;
    private final String emailAddressFrom;

    private static final String SUBJECT = "Autoscaler status alert.";
    private static final String FROMNAME = "Autoscaler";
    private static final Logger LOG = LoggerFactory.getLogger(EmailDispatcher.class);

    /**
     * Constructs an Email Dispatcher
     *
     * @param configurations Configurations to use when dispatching emails
     * @throws ScalerException When it is unable to send alert emails
     */
    public EmailDispatcher(final EmailDispatcherConfiguration configurations) throws ScalerException
    {
        Objects.requireNonNull(configurations);
        this.username = configurations.getUsername();
        this.password = configurations.getPassword();
        this.host = configurations.getHost();
        this.port = configurations.getPort();
        this.emailAddressTo = configurations.getEmailAddressTo();
        this.emailAddressFrom = configurations.getEmailAddressFrom();
    }

    @Override
    public void dispatch(final String emailBody) throws ScalerException
    {
        try {
            // Create a Properties object to contain connection configuration information.
            final Properties props = System.getProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.port", port);
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.auth", "true");

            // Create a Session object to represent a mail session with the specified properties. 
            final Session session = Session.getDefaultInstance(props);

            // Create a message with the specified information. 
            final MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(emailAddressFrom, FROMNAME));

            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(emailAddressTo));
            msg.setSubject(SUBJECT);
            msg.setContent(emailBody, "text/html");

            // Create a transport.
            final Transport transport = session.getTransport();

            // Send the message.
            try {
                LOG.debug("Sending email...");

                // Connect to the SMTP server using username and password specified above.
                transport.connect(host, username, password);

                // Send the email.
                transport.sendMessage(msg, msg.getAllRecipients());
                LOG.info("Email sent!");
            } catch (final MessagingException ex) {
                LOG.error("The email was not sent.", ex);
            } finally {
                // Close and terminate the connection.
                transport.close();
            }
        } catch (final UnsupportedEncodingException | MessagingException ex) {
            throw new ScalerException("Unable to send email alert.", ex);
        }
    }
}
