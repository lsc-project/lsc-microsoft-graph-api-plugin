# Microsoft Graph API plugin

[![Build Status](https://travis-ci.org/lsc-project/lsc-microsoft-graph-api-plugin.svg?branch=master)](https://travis-ci.org/lsc-project/lsc-microsoft-graph-api-plugin)

This a plugin for LSC, using Microsoft Graph API

### Goal

The object of this plugin is to synchronize users from a Microsoft Azure active directory to a referential.
For example it can be used to synchronize the users in an Azure AD to an LDAP repository.

### Configuration

The plugin connects to Microsoft Graph API as a [deamon app](https://docs.microsoft.com/en-us/azure/active-directory/develop/scenario-daemon-overview). More information on how to register it on Microsoft Azure can
be found [here](https://docs.microsoft.com/en-us/azure/active-directory/develop/scenario-daemon-app-registration).

The application must have the `Application permission` `User.Read.All` permission granted. The documentation about permissions and consent can be found [here](https://docs.microsoft.com/en-us/azure/active-directory/develop/v2-permissions-and-consent).

At the time being the plugin only allows to connect to the API using a client secret.

### Usage

There are examples of configuration in the `sample` directory. The `lsc.xml` file describes a synchronization from Microsoft Graph API to an LDAP repository.

The values to configure are:

#### Source service

##### Connection

  - `connections.pluginConnection.msGraphApiConnectionSettings.authenticationURL`: The base URL used for authentication (default is https://login.microsoftonline.com/) (optional)
  - `connections.pluginConnection.msGraphApiConnectionSettings.usersURL`: The base URL used for operations on users (default is https://graph.microsoft.com) (optional)
  - `connections.pluginConnection.msGraphApiConnectionSettings.scope`: The scope url used during authentication (default is https://graph.microsoft.com/.default) (optional)
  - `connections.pluginConnection.msGraphApiConnectionSettings.clientId`: The client id for the application
  - `connections.pluginConnection.msGraphApiConnectionSettings.clientSecret`: The client secret used to connect to the application
  - `connections.pluginConnection.msGraphApiConnectionSettings.tenant`: The  Azure AD  tenant

##### API parameters

  - `tasks.task.pluginSourceService.filter`: (Optional, default none) The filter to use for fetching the list of pivots. For the syntax to use in those filters the syntax can be found [here](https://docs.microsoft.com/en-us/graph/query-parameters#filter-parameter).
  - `tasks.task.pluginSourceService.pivot`: (Optional, default `mail`) The field to use as pivot.
  - `tasks.task.pluginSourceService.pageSize`: (Optional, default none) The page size used to paginate the results from the graph API. Default is no page size, but the API has a `100` default page size.
  - `tasks.task.pluginSourceService.select`: (Optional, default none) The comma separated list of fields to gather when getting the details of a user. The syntax to use can be found [here](https://docs.microsoft.com/en-us/graph/query-parameters#select-parameter). By default the API returns a default set of properties.

The jar of the Microsoft graph API LSC plugin must be copied in the `lib` directory of your LSC installation. Then you can launch it with the following command line:
```
`̀`` JAVA_OPTS="-DLSC.PLUGINS.PACKAGEPATH=org.lsc.plugins.connectors.msgraphapi.generated" bin/lsc --config /path/to/sample/msgraphapi-to-ldap/ --synchronize users --clean users --threads 5
```
### Packaging

WIP
