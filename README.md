Hubitat-TED5000.groovy

This is an adaptation of the TED5000 Pro Hubitat  device driver written by Daniel Terryn.  

The TED5000 can support up to four MTU’s.  Each can be set for different roles, Load, Generation, Adjusted Load, Standalone, and Standalone (NET).  This driver is customized for my TED5000 configuration with two MTU’s.  MTU1 is on the  line from the utility company and is set as “Adjusted Load”.  MTU2 is on the lines from my 2 kWh solar array and is set as “Generation”.   If your setup is different you may need to adjust the driver code as needed.

On your TED5000 go to the http://TED5000/api/LiveData.xml file to see what values it returns.  Use this as a guide to tweak the driver for your particular configuration. 

o install the driver in Hubitat: 
1. Go to the Drivers code page select new driver and paste in the entire hubitat-ted5000.groovy file. Then click save.  It should not report any errors.

2.  Go to devices page and select “add Virtual Device”.  On the Driver information page scroll down to the bottom of the “Type”  and select TED5000, in the “Device name” field  give a name for your device.

.  Fill in the IP address and port for your TED5000 and select a refresh interval, and logging level.  If you have authentication enabled then fill in the userid and password for your TED5000.

t this point you should start to see current States values being populated.  If not look at the log files for the TED5000 driver for a hint as to what is going wrong.   You may need to increase the log level to a higher value such as debug.

If everything is working you can select the following attributes for a dashboard item.

Using the LiveData.xml file from your TED5000 as a guide add state variables and attributes or rename the existing state variables and attributes as needed to fit your configuration.


