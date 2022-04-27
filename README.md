# Space Document Extractor

The aim of this repository is to help to generate stand-alone version of JetBrains Space documents. Those documents are written in MarkDown format and could include images. In order to do that one have to do several steps:

* Download a page as markdown to a directory.
* Download attached images to specific directory.
* Replace references to attachments in MarkDown files.

This project uses Space SDK to organize those steps.

## Setting up Space Application

In order to access data in Space, one needs to [create a Space Application](https://www.jetbrains.com/help/space/applications.html) and add appropriate permissions. I am not sure which permissions cover access to image, but here are those that I allowed:

* Provide external attachment unfurls
* Provide external inline unfurls
* View project data
* View book metadata
* View content

For restricted projects, one needs to manually add the project and its permission to allowed.

Then one needs to copy `clientId` and `clientSecret` for the application and use them as command line parameters.

## Downloading texts

Text and binary documents are processed recursively starting at given `folderId` or project root if it is not defined.
## Download images

The images in space documents are inserted in the following format: `![](/d/aaaabbbbcccc?f=0 "name.png")`. Our aim is to detect those links in files and download appropriate images. Those links could not be replaced directly, because access requires OAuth authentication. For that we need to use access token from Space SDK.

## Replace references

After file is successfully downloaded, the reference in file must be replaced with a local one.

## Command line interface

```commandline
Usage: space-document-extractor options_list
Options: 
    --spaceUrl -> Url of the space instance like 'https://mipt-npm.jetbrains.space' (always required) { String }
    --project -> The key of the exported project (always required) { String }
    --path -> Target directory. Default is './output/project-key'. { String }
    --folderId -> FolderId for the folder to export. By default uses project root. { String }
    --clientId -> Space application client ID (if not defined, use environment value 'space.clientId') { String }
    --clientSecret -> Space application client secret (if not defined, use environment value 'space.clientSecret') { String }
    --help, -h -> Usage info 
```

Typical application usage:

```commandline
.\space-document-extractor --spaceUrl "your space URL" --project "your project key" --clientId "your client ID" --clientSecret "your client secret"
```

It will download all documents and postprocess markdown files, replacing image links with downloaded image in `images` directory (each subdirectory will have its own `images`. 