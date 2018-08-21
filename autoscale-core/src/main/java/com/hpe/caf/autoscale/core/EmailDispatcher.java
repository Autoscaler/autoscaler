/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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
package com.hpe.caf.autoscale.core;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailDispatcher
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

    public EmailDispatcher()
    {
        this.host = System.getenv("CAF_SMTP_HOST");
        this.port = System.getenv("CAF_SMTP_PORT");
        this.username = System.getenv("CAF_SMTP_USERNAME");
        this.password = System.getenv("CAF_SMTP_PASSWORD");
        this.emailAddressTo = System.getenv("CAF_SMTP_EMAIL_ADDRESS_TO");
        this.emailAddressFrom = System.getenv("CAF_SMTP_EMAIL_ADDRESS_FROM");
    }

    public void dispatchEmail(final String messageContent)
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
            msg.setContent(messageContent, "text/html");

            // Create a transport.
            final Transport transport = session.getTransport();

            // Send the message.
            try {
                LOG.debug("Sending email...");

                // Connect to the SMTP server using username and password specified above.
                transport.connect(host, username, password);

                // Send the email.
                transport.sendMessage(msg, msg.getAllRecipients());
                LOG.debug("Email sent!");
            } catch (final MessagingException ex) {
                LOG.error("The email was not sent.", ex);
            } finally {
                // Close and terminate the connection.
                transport.close();
            }
        } catch (final Exception ex) {
            LOG.error("The email was not sent.", ex);
        }
    }
}
