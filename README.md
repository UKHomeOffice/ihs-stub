# IHS Stub [![Build Status](https://travis-ci.org/UKHomeOffice/ihs-stub.svg?branch=master)](https://travis-ci.org/UKHomeOffice/ihs-stub)

## Synopsis

Stub conforming to the IHS (Immigration Health Surcharge) payment request API, mocking the interactions of the user.

This project acts as a stub for IHS to enable development of that integration without needing a full IHS instance running.

The stub allows the user to mock a successful or failed IHS payment.

## Before Usage

In order to fully mimic the IHS system, you will need to regenerate keystore.jks with the IHS keys. The current keystore uses dummy keys, which enables development of this stub independently.

When directed to the stub, the user will be presented with a developer page that allows them to see the details from the decrypted token, along with the ability to mark the request as either successful or failed (mimicking the response of a successful or failed IHS payment).

If successful, the completionURL will be sent an x-www-form-urlencoded POST request, that contains ihs_token in the body, with the value of the signed IHS token.

If unsuccessful, the failureURL will be sent a GET request, that contains an Error_Code and Error_Description.

Both the completionURL and failureURL are configured in application.conf and is tested in IhsControllerSpec.

## Usage

To use this stub run "./sbt run <port>".

## Motivation

This project exists because it is useful for the Home Office Visa exemplar and for any other projects that need to allow the user to make payment to IHS.

This stub is designed to be used as a replacement for IHS during development, and is not designed for production use.

## Tests

Tests can be executed by running "./sbt test".

However if you are learning how the service works through running individual tests, this is best done in an IDE such as IntelliJ.

## Contributors

If you want to contribute to the project you can do it by creating a pull request.

For more information on how to effectively contribute, please consult the CONTRIBUTING.md file.

## Known issues

At present there are no known issues

