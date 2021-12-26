# Space Document Extractor

The aim of this repository is to help to generate stand-alone version of JetBrains Space documents. Those documents are written in MarkDown format and could include images. In order to do that one have to do several steps:

* Download a page as markdown to a directory.
* Download attached images to specific directory.
* Replace references to attachments in MarkDown files.

This project uses Space SDK to organize those steps.

## Setting up Space Application

In order to access data in Space, one needs to [create a Space Application](https://www.jetbrains.com/help/space/applications.html) and add appropriate permissions. I am not sure which permissions cover access to images, but here are those that I allowed:

* Provide external attachment unfurls
* Provide external inline unfurls
* View project data
* View book metadata
* View content

Then one needs to copy `clientId` and `clientSecret` for the application and use them as command line parameters.

## Downloading texts

Right now Space SDK does not have methods to access documents, so the only way is to copy the markdown and paste it directly to a file. I hope it will change in the future.

## Download images

The images in space documents are inserted in the following format: `![](/d/aaaabbbbcccc?f=0 "name.png")`. Our aim is to detect those links in files and download appropriate images. Those links could not be replaced directly, because access requires OAuth authentication. For that we need to use access token from Space SDK.

## Replace references

After file is successfully downloaded, the reference in file must be replaced with a local one.

## Command line interface

Typical application usage:

```commandline
.\space-document-extractor --spaceUrl https://mipt-npm.jetbrains.space --path D:\Work\report\ --clientId "your client ID" --clientSecret "your client secret"
```

It will search the directory (and subdirectories) and replace image links with downloaded image in `./images` directory. 