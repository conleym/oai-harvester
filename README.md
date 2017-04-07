# Unizin OAI Harvester

An [OAI-PMH 2.0 harvester](https://www.openarchives.org/OAI/openarchivesprotocol.html) library
and a [dropwizard](http://www.dropwizard.io/) harvester service.

The service includes an embedded database for harvest configuration and web services for harvest management.
Harvests add raw XML records to a DynamoDB table.

Also included are [DynamoDB trigger](http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Streams.Lambda.html) scripts to place added or updated records on an SQS queue for further processing.

## Development Quick Start

This project requires Java 8 or above.

1. Install [Maven](https://maven.apache.org/install.html).
1. Install and run [DynamoDB Local](http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Tools.DynamoDBLocal.html).
1. Import as maven project into IntelliJ, Eclipse, or your environment of choice.
1. Run `org.unizin.cmp.oai.harvester.service.HarvestServiceApplication.java`. The program itself requires two arguments: `server` `config/dev.yml`, the latter assuming the working directory to be the `unizin-oai-harvest-service` project root.

## Open Source

This software is written for and maintained by Unizin. We offer it without
guarantees because it may be useful to your projects. All proposed contributions
to this repository are reviewed by Unizin.
