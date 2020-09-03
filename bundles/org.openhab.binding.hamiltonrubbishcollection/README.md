# HCC NZ Rubbish Collection Binding

_This binding will keep track of the your rubbish collection days in Hamilton, New Zealand._

Visit the [Fight the Landfill](https://www.fightthelandfill.co.nz/) website for more information on the *Hamilton City Council* rubbish collection service.

## Supported Things

A single supported thing called `collection`.

## Thing Configuration

The thing supports one setting labelled `address` which is your street number and name as it appears on Google. *__IE:__ 1 Victoria Street*

> Note: The above address will not return any data.

*__If the address is not valid or rubbish collection service does not apply (for example, a business address) then an `CONFIGURATION_ERROR` will occur.__*

## Channels

| channel   | type   | description                                              |
| --------- | ------ | -------------------------------------------------------- |
| day       | String | The rubbish collection day of the week                   |
| redbin    | Date   | The next red bin (household rubbish) collection day      |
| yellowbin | Date   | The next yellow bin (recycling and glass) collection day |
