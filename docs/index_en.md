---
title: Archiving image folders
identifier: intranda_step_archiveimagefolder
published: true
description: Goobi Step Plugin for copying image folders to external storage
keywords:
    - Goobi workflow
    - Plugin
    - Step Plugin
---
## Introduction
This step plugin for Goobi workflow copies image folders to an external storage connected via sftp (ssh) and creates a file that causes Goobi workflow to display a warning in the task. The warning is displayed in the task details and in the metadata editor. As an alternative to sftp, an s3 bucket can also be used.


## Installation
The plugin consists of the following file:

```bash
goobi_plugin_step_archiveimagefolder-base.jar
```

This file must be installed in the correct directory so that it is available at the following path after installation:

```bash
/opt/digiverso/goobi/plugins/step/plugin_intranda_step_archiveimagefolder-base.jar
```

In addition, there is a configuration file that must be located in the following place:

```bash
/opt/digiverso/goobi/config/plugin_intranda_step_archiveimagefolder.xml
```


## Overview and functionality
To put the plugin into operation, it must be activated for one or more desired automatic tasks in the workflow. This is done as shown in the following screenshot by selecting the plugin `intranda_step_archiveimagefolder` from the list of installed plugins.

![Integration of the plugin into the workflow](screen1_en.png)

The plugin copies the files from the configured folder to the SSH server or the S3 bucket, writes an XML file with the info where the files are located to the operation folder and then (if so configured) deletes the images on the Goobi storage and closes the step.

The images can be restored afterwards with the plugin `goobi-plugin-administration-restorearchivedimagefolder`.


## Configuration
The configuration of the plugin is done via the configuration file `plugin_intranda_step_archiveimagefolder.xml` and can be adjusted during operation. The following is an example configuration file:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<config_plugin>
        <!--
        order of configuration is:
          1.) project name and step name matches
          2.) step name matches and project is *
          3.) project name matches and step name is *
          4.) project name and step name are *
	-->

    <config>
        <!-- which projects to use for (can be more then one, otherwise use *) -->
        <project>*</project>
        <step>*</step>

        <!-- the folder to archive -->
        <folder>master</folder>
        <!-- delete local files after copying and close step -->
        <deleteAndCloseAfterCopy>false</deleteAndCloseAfterCopy>
        <method>
            <name>SSH</name>
            <host>bandspeicher.intranda.com</host>
            <user>intranda</user>
        </method>
    </config>

    <config>
        <project>*</project>
        <step>Copy to S3 storage</step>
        <folder>media</folder>
        <deleteAndCloseAfterCopy>false</deleteAndCloseAfterCopy>
        <method>
            <name>s3</name>
            <S3Endpoint>http://127.0.0.1:9000</S3Endpoint>
            <S3Bucket>storage-bucket</S3Bucket>
            <S3Prefix>archive</S3Prefix>
            <S3AccessKeyID>CHANGEME</S3AccessKeyID>
            <S3SecretAccessKey>CHANGEME</S3SecretAccessKey>
        </method>
    </config>

</config_plugin>
```

For authentication on the ssh server, public keys are searched for in the usual places (`$USER_HOME/.ssh`). Other authentication methods such as username/password are not yet provided.

When using s3, the s3 endpoint and the bucket name to be used must be specified. Optionally, a prefix can be set if archiving is not to take place directly in the root of the bucket. S3AccessKeyID and S3SecretAccessKey contain the credentials to access the bucket.

The setting `<deleteAndCloseAfterCopy>false</deleteAndCloseAfterCopy>` is intended for the case that the files on the SSH server are first stored in a buffer and then written to a tape. In this case, the step can remain open and be closed by a callback of the tape storage system. The callback has not yet been implemented for any tape storage, but can be mapped using the standard Goobi workflow REST API.

