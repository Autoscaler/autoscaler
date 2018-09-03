## Email Alert Dispatcher

This module is designed to me the requirements of the autoscalers AlertDispatcher interface. This allows this module to work with the autoscaler to dispatch messages that will alert an admin of a potential issues with the services being managed.

To use this module the following configuration will need to be provided via environment variable.

### Email Alert Configuration

Configuration supported through the following environment variables:

- `CAF_AUTOSCALER_SMTP_HOST`  
Default: n/a  
Description: SMTP server host address.  

- `CAF_AUTOSCALER_SMTP_PORT`  
Default: n/a  
Description: SMTP server port.  

- `CAF_AUTOSCALER_SMTP_USERNAME`  
Default: "" (Empty String)  
Description: SMTP server username.  

- `CAF_AUTOSCALER_SMTP_PASSWORD`  
Default: "" (Empty String)  
Description: SMTP server password.  

- `CAF_AUTOSCALER_SMTP_EMAIL_ADDRESS_TO`  
Default: n/a  
Description: The monitored email address to send alert emails to. If this property is not set the autoscaler will not attempt to use the email alert functionality.  
  
- `CAF_AUTOSCALER_SMTP_EMAIL_ADDRESS_FROM`  
Default: `apollo-autoscaler@microfocus.com`  
Description: The email address to send alert emails from.  
