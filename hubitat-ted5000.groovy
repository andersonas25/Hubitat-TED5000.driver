x/*
 * TED5000
 *
 *  Copyright 2019 Daniel Terryn
 *
 *  Licensed Virtual Image Switch the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Change History:
 *
 *    Date        Who            What
 *    ----        ---            ----
 *    2019-05-21  Daniel Terryn  Original Creation
 *    2019-09-10  Alan Anderson  Hacked to support old style TED5000
 * 
 */
metadata {
    definition (name: "TED5000", author: "alan_a", namespace: "alan_a") {
        capability "Polling"
        capability "Refresh"
        capability "Sensor"
        capability "Power Meter"
        capability "Energy Meter"
        capability "Voltage Measurement"
        capability "Configuration"
        capability "Initialize"
        
        attribute "Solar_power", "string"
        attribute "Solar_today", "string"
        attribute "Solar_peak", "string"
        attribute "Solar_saving_MTD", "string"
        attribute "Solar_saving_TDY", "string"
        attribute "Solar_power_average", "string"
        attribute "Solar_power_factor", "string"
        attribute "Net_power", "string"
        attribute "Adjusted_load", "string"
        attribute "Voltage" , "string"
        attribute "cost_today", "string"
        attribute "daily_max_power", "string"
        attribute "daily_min_power", "string"
        attribute "daily_total_power", "string"
        attribute "power_factor", "string"

        command  "clearAlertMessage"

    }
    preferences {
        input ( name: "ip", type: "text", title: "TED IP Address", description: "IP Address in form 192.168.1.226", required: true, displayDuringSetup: true )
        input ( name: "port", type: "text", title: "TED Port", description: "port in form of 8090", required: true, displayDuringSetup: true )
        input ( name: "username", type: "text", title: "Username", required: false, displayDuringSetup: true )
        input ( name: "password", type: "password", title: "Password", required: false, displayDuringSetup: true )
        input ( name: 'pollInterval', type: 'enum', title: 'Update interval (in seconds)', options: ['10', '30', '60', '120', '300'], required: true, displayDuringSetup: true )
        input ( name: "configLoggingLevelIDE", title: "IDE Live Logging Level:\nMessages with this level and higher will be logged to the IDE.", type: "enum",
            options: [
                "0" : "None",
                "1" : "Error",
                "2" : "Warning",
                "3" : "Info",
                "4" : "Debug",
                "5" : "Trace"
            ],
            defaultValue: "3", displayDuringSetup: true, required: false )      
    }
}


def installed() {
    logger("Executing 'installed()'", "info")
    initialize()
}

def initialize() {
    logger("Executing 'initialize()'", "debug")
    updated()
}

def updated() {
    logger("Executing 'updated()'", "debug")
    configure()
}

def configure() {
    logger("Executing 'configure()'", "info")
    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 3
    updateDeviceNetworkID()

    unschedule()
    schedule("0/${settings.pollInterval} * * * * ? *", refresh)
    
    refresh()
    childDevices.each {
        try
        {
            it.setLoggingLevel(state.loggingLevelIDE)
        }
        catch (e) {}    
    }
}

def poll() {
    logger('Executing poll()', "debug")
    refresh()
}

def refresh() {
    logger("Executing 'refresh()'", "debug")
    sendAsyncHttp("api/LiveData.xml", "parse_LiveData")
}



def valueChangeEvent(def deviceAttribute, def newValue, def newUnit)
{
    def success = false
    def oldValue = null
    if (state?.data_points)
    {
        if (state?.data_points["${deviceAttribute}"])
        {
            oldValue = state?.data_points["${deviceAttribute}"]
            if (state?.data_points["${deviceAttribute}"] == newValue)
                return false
            else if (state?.data_points["${deviceAttribute}"].toString().equals(newValue.toString()))
                return false
        }
    }
    else
        state.data_points = [:]
    
    state.data_points["${deviceAttribute}"] = newValue
    
    logger("----> Send new ${deviceAttribute} state, old: ${oldValue}, new: ${newValue}", "debug")
    sendEvent(name: deviceAttribute, value: newValue, unit:  newUnit)

    def nowDay = new Date().format("MMM dd", location.timeZone)
    def nowTime = new Date().format("h:mm a", location.timeZone)
    
    return true;
}



def parse_LiveData(response, data) {
    logger("parse_LiveData", "info")
    def status = response.status          // => http status code of the response
    def xml = parseXML(response.getData())

    if (status.toInteger() == 200)
    {
		valueChangeEvent("Net_power", (xml.Power.Total.PowerNow).toInteger(), "W")
        valueChangeEvent("Adjusted_load", (xml.Power.MTU1.PowerNow).toInteger(), "W")
		valueChangeEvent("voltage", ((xml.Voltage.Total.VoltageNow).toDouble() / 10.0), "V")
		valueChangeEvent("cost_today", ((xml.Cost.Total.CostTDY).toDouble() / 100.0), "\044")
		valueChangeEvent("daily_max_power", (xml.Power.Total.PeakTdy).toInteger(), "W")
		valueChangeEvent("daily_min_power", (xml.Power.Total.MinTdy).toInteger(), "W")
		valueChangeEvent("daily_total_power", (xml.Power.Total.PowerTDY).toInteger(), "W")
		valueChangeEvent("power_factor", ((xml.Power.MTU1.PF).toDouble() / 10.0), "%") 
		valueChangeEvent("Solar_power", (xml.Power.MTU2.PowerNow).toInteger(), "W")
		valueChangeEvent("Solar_peak", (xml.Power.MTU2.PeakTdy).toInteger(), "W")
		valueChangeEvent("Solar_today", (xml.Power.MTU2.PowerTDY).toInteger(), "W")
		valueChangeEvent("Solar_power_average", (xml.Power.MTU2.PowerAvg).toInteger(), "W")
		valueChangeEvent("Solar_power_factor", ((xml.Power.MTU2.PF).toDouble() /10.0) , "%")
        valueChangeEvent("Solar_saving_MTD", ((xml.Cost.MTU2.CostMTD).toDouble() / 100.0), "\044")
        valueChangeEvent("Solar_saving_TDY", ((xml.Cost.MTU2.CostTDY).toDouble() / 100.0), "\044")
     
    }  
}

private getHostAddress() {
    def ip = settings.ip
    def port = settings.port
    
    logger("Using ip: ${ip} and port: ${port} for device: ${device.id}", "debug")
    return ip + ":" + port
}

def sendAsyncHttp(message, handler)
{
    def requestParams =
    [
        uri:  "http://"+getHostAddress()+"/"+message,
        requestContentType: "application/x-www-form-urlencoded",
    ]
    if ((settings.username != null) && (settings.password != null)) 
    {
        def headers = [:]
        headers.put("Authorization", encodeCredentialsBasic(settings.username, settings.password))  
        requestParams.put("headers", headers)
    }
    asynchttpGet(handler, requestParams)
    
}
def clearAlertMessage()
{
    state.remove('alertMessage')
}

private encodeCredentialsBasic(username, password) {
    return "Basic " + "${username}:${password}".bytes.encodeBase64()
}

def updateDeviceNetworkID() {
    logger("Executing 'updateDeviceNetworkID'", "debug")
}


/**
 *  logger()
 *
 *  Wrapper function for all logging.
 **/
private logger(msg, level = "debug") {

    switch(level) {
        case "error":
            if (state.loggingLevelIDE >= 1) log.error msg
            break

        case "warn":
            if (state.loggingLevelIDE >= 2) log.warn msg
            break

        case "info":
            if (state.loggingLevelIDE >= 3) log.info msg
            break

        case "debug":
            if (state.loggingLevelIDE >= 4) log.debug msg
            break

        case "trace":
            if (state.loggingLevelIDE >= 5) log.trace msg
            break

        default:
            log.debug msg
            break
    }
}

