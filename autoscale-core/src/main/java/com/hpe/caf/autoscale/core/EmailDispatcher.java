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

import java.util.Date;
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
    private Date lastSent;
    private int lastPriorityThreshold;

    private final String host;
    private final String port;
    private final String username;
    private final String password;
    private final String emailAddressTo;
    private final String emailAddressFrom;

    private static final String SUBJECT = "Autoscaler status alert.";
    private static final String FROMNAME = "Autoscaler";
    private static final Logger LOG = LoggerFactory.getLogger(EmailDispatcher.class);
    private static final Object LOCK = new Object();

    public EmailDispatcher()
    {
        final String usernameEnv = System.getenv("CAF_SMTP_USERNAME");
        final String passwordEnv = System.getenv("CAF_SMTP_PASSWORD");
        final String addressFromEnv = System.getenv("CAF_SMTP_EMAIL_ADDRESS_FROM");
        this.username = usernameEnv != null ? usernameEnv : "";
        this.password = passwordEnv != null ? passwordEnv : "";
        this.host = System.getenv("CAF_SMTP_HOST");
        this.port = System.getenv("CAF_SMTP_PORT");
        this.emailAddressTo = System.getenv("CAF_SMTP_EMAIL_ADDRESS_TO");
        this.emailAddressFrom = addressFromEnv != null ? addressFromEnv : "apollo-autoscaler@microfocus.com";
    }

    public void dispatchEmail(final String messageContent, final int priorityThreshold)
    {
        final Date date = new Date();
        if ((emailAddressTo == null || emailAddressTo.length() == 0)
            || (lastSent == null || ((date.getTime() - lastSent.getTime() <= 20 * 60 * 1000) && priorityThreshold == lastPriorityThreshold))) {
            return;
        }

        synchronized (LOCK) {
            if (lastSent == null || ((date.getTime() - lastSent.getTime() >= 20 * 60 * 1000 || !(priorityThreshold == lastPriorityThreshold)))) {
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
                        lastSent = date;
                        lastPriorityThreshold = priorityThreshold;
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
    }
}
