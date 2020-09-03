# HCC NZ Rubbish Collection Binding

_This binding will keep track of the your rubbish collection days for the Hamilton City Council (New Zealand)._

## Supported Things



## Discovery

_Describe the available auto-discovery features here. Mention for what it works and what needs to be kept in mind when using it._

## Binding Configuration

_If your binding requires or supports general configuration settings, please create a folder ```cfg``` and place the configuration file ```<bindingId>.cfg``` inside it. In this section, you should link to this file and provide some information about the options. The file could e.g. look like:_

```
# Configuration for the Philips Hue Binding
#
# Default secret key for the pairing of the Philips Hue Bridge.
# It has to be between 10-40 (alphanumeric) characters
# This may be changed by the user for security reasons.
secret=openHABSecret
```

_Note that it is planned to generate some part of this based on the information that is available within ```src/main/resources/ESH-INF/binding``` of your binding._

_If your binding does not offer any generic configurations, you can remove this section completely._

## Thing Configuration

The configuration requires your street address, IE: 1 Victoria Street

> Note: The above address will not return a valid address.

## Channels

_Here you should provide information about available channel types, what their meaning is and how they can be used._

_Note that it is planned to generate some part of this based on the XML files within ```src/main/resources/ESH-INF/thing``` of your binding._

| channel   | type   | description                                              |
| --------- | ------ | -------------------------------------------------------- |
| day       | String | The rubbish collection day of the week                   |
| redbin    | Date   | The next red bin (household rubbish) collection day      |
| yellowbin | Date   | The next yellow bin (recycling and glass) collection day |

## Full Example

_Provide a full usage example based on textual configuration files (*.things, *.items, *.sitemap).
