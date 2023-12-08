## Description

The plugin provides ability to render markdown files from Bitbucket on the Confluence page. Rendering is performed using [flexmark-java](https://github.com/vsch/flexmark-java) library.


## Parameters

| Name          | Description                              |
|---------------|------------------------------------------|
| BitbucketURL  | URL of Bitbucket server.                 |
| ProjectKey    | Project key.                             |
| Repository    | Repository.                              |
| Path          | Path to markdown file in the repository. |
| Revision      | Commit ID or ref. For example: `master`. |
| User          | User name.                               |
| Password      | User password.                           |


## Features

HTTPS protocol is supported only.

Relatively located PNG images are embedded using base64 format.

Supported extensions:
* Tables
* Table of Contents
* Code block
