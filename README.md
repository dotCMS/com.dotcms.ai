# README

This is plugin that a number of dotCMS specific tools that leverage AI tooling (Open AI specifically) for use in dotCMS.  
This includes REST apis, Workflow Actions and Viewtools that let dotCMS interact with AI in a variety of ways.  For examples on how to use these endpoints and to create content embedding indexes, see 
[this document](README-CURL.md). To see how OpenAI can be used in Velocity contexts, see this [this document](README-Velocity%20Tooling.md)

Out of the box, it provides:
### An App
  Where credentials and defaults can be configured and/or overridden
### dotAI Portlet/Tool
  - Search and Chat with Content
  - View/Update/Delete Content Embedding Indexes
  - View AI Plugin configuration values.  These are important because they can override and parameterize the prompts that we send to OpenAI.
### REST APIs
  There are 4 main REST resources provided by this plugin:
  - `/api/v1/ai` - Generative Resource
    - Generate new content from a prompt, e.g. "Write me a blog about the top 10 ski resorts in Canada".
    - Generate an image from a prompt, e.g.  "Create an image of a dog skiing in Canada".
  - `/api/v1/ai/embeddings` - Embeddings Resource
    - Drop/Recreate the `dot_embeddings` content embeddings database
    - CRUD Actions for the `dot_embeddings` content embeddings database (add content to index, delete content from index, list embedding indexes)
  - `/api/v1/ai/search` - Semantic Search Resource
    - Search the content embedding database for nearest matching content using 
  - `/api/v1/ai/completions` - Completion Resource
    - Using search results from querying the embeddings, generate a response based on the content in the embeddings (from content in dotCMS), e.g.  "What should I do when visiting Costa Rica?"
    - Generate a streaming response from a query, e.g.  "What should I do when visiting Costa Rica?"
    - Perform a `raw` chat request to OpenAIs completion endpoint
- `/api/v1/ai/image/generate` - Image Resource
    - Create an AI generated image based on a prompt.  The resulting image will be stored as a temp file in dotCMS"
### Workflow Actions
  - **OpenAI Embeddings** (`DotEmbeddingsActionlet`)  This actionlet uses OpenAI to generate and save (or delete) the embeddings for content.  This is used so that an embedding index can be kept up to date as new content is published and/or unpublished from dotCMS.
  - **OpenAI Content Prompt** (`OpenAIContentPromptActionlet`).  This actionlet can be called to automatically populate/update fields of content as the content is pushed through a workflow.  It works by expecting OpenAI to return its data/answer in a json format which will then be used to update the content.  The example usage is to post content to OpenAI and have OpenAI automatically write appropriate SEO title and SEO short description for the content.
  - **OpenAI Generate Image** (`OpenAIGenerateImageActionlet`).  This actionlet can automatically generate an image based on a content prompt.  This content prompt is a velocity template and can use the values of the content in it.  By default, this actionlet will add this image to the first binary field in the content.

### Velocity Viewtools
  - `$ai` can be used to generate content and/or images from a prompt.
  - `$ai.embeddings` - list embedding indexes, generate embeddings from a prompt, count the tokens in a prompt.  
  - `$ai.search` - semantically search an index of embedded content.  Also can be used to pull a list of content that is semantically related another piece of content.
  - `$ai.completions` - generate completions based on a content query.  Can also be used to do a raw OpenAI request.






## How to build/install this plugin
### Building
To build this plugin all you need to do is build the JAR. to do this run
```
./gradlew clean jar
```

### Installation
1. After building, upload the bundle jar found in `./build/libs/com.dotcms.ai***.jar` using the dotCMS UI `Dev Tools -> Plugins -> Upload Plugin`.  It should go ultimately go to the "Active" state.  If it is not "Active" it has not been successfully started.  Try to manually start it.  If it does not go to "Active", check your logs to see why the plugin is failing to start.  Trying tailing the logs while you try to start the plugin again.
3. Note that choosing to "undeploy" a plugin is somewhat destructive and is generally not needed before uploading a newer version of the plugin. If you undeploy the plugin, dotCMS will automatically remove the portlet from your tools and delete any associated workflow actions you might have configured. 

### Releasing
The plugin uses the maven release workflow found here: https://github.com/researchgate/gradle-release
If you are ready to tag/release a new version, all you have to do is run


```
./gradlew clean release
```
It will 
- prompt you for input
- make sure that you are on a clean branch
- increment the version number
- create a tag in git
- push the tag to GitHub

### Publishing to Artifactory
GitHub will pick up any new tag and will build and publish it to artifactory automatically.

If you need/want to publish to artifactory manually, set the environmental variables `ARTIFACTORY_USER` & `ARTIFACTORY_PASSWD` with your artifactory creds and then run:
```
./gradlew publish
```
which will build and publish your snapshot or release.


## Configuration
At the bare minimum, you will need an API Key from OpenAI. Go to `Settings >  Apps` and open the OpenAI app / System Host.  There you can set your OpenAI api key and other configuration values.

Once you have done that, add and check out the portlet to learn more about how Open AI can be used within dotCMS.
