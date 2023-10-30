# README

This is plugin that a number of dotCMS specific tools that leverage AI tooling (Open AI specifically) for use in dotCMS.  
This includes REST apis, Workflow Actions and Viewtools that let dotCMS interact with AI in a variety of ways.  For examples on how to use these endpoints and to create content embedding indexes, see 
[ this document](README-CURL.md). 

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
  - **OpenAI Modify Content** (`OpenAIModifyContentActionlet`).  This actionlet can be called to automatically populate/update fields of content as the content is pushed through a workflow.  It works by expecting OpenAI to return its data/answer in a json format which will then be used to update the content.  The example usage is to post content to OpenAI and have OpenAI automatically write appropriate SEO title and SEO short description for the content.
### Velocity Viewtools
  - `$ai` can be used to generate content and/or images from a prompt.
  - `$ai.embeddings` - list embedding indexes, generate embeddings from a prompt, count the tokens in a prompt.  
  - `$ai.search` - semantically search an index of embedded content.  Also can be used to pull a list of content that is semantically related another piece of content.
  - `$ai.completions` - generate completions based on a content query.  Can also be used to do a raw OpenAI request.






### How to build/install this plugin

To install this plugin all you need to do is build the JAR. to do this run
`./gradlew jar`


### Installation

1. Upload the bundle jar files using the dotCMS UI `Dev Tools -> Plugins -> Upload Plugin`.  It should go ultimately go to the "Active" state.  If it is not "Active" it has not been successfully started.  Try to manually start it.  If it does not go to "Active", check your logs to see why the plugin is failing to start.  Trying tailing the logs while you try to start the plugin again.
2. Add the **dotAI** tool to your admin screen from the **Roles & Tools** tool. 
3. Note that choosing to "undeploy" a plugin is somewhat destructive and is generally not needed before uploading a newer version of the plugin. If you undeploy the plugin, dotCMS will automatically remove the portlet from your tools and delete any associated workflow actions you might have configured. 



## Configuration
At the bare minimum, you will need an API Key from OpenAI. Go to `Settings >  Apps` and open the OpenAI app / System Host.  There you can set your OpenAI api key and other configuration values.

---
## How to test

Once installed, you can access this resource by (this assumes you are on localhost)

`http://localhost:8080/api/ai/text/generate`

You can try the get and post resources by
`
curl --location 'http://localhost:8081/api/ai/text/generate' \
--header 'Content-Type: application/json' \
--data '{
"prompt": "What are the top 5 places to visit in Costa Rica?"
}'
`

`
curl --location 'http://localhost:8081/api/ai/text/generate?prompt=your%20prompt%20text'
`

---
## Components

### com.dotcms.ai.viewtool.AIToolInfo

For registering and initialization of our ViewTool implementation

### com.dotcms.ai.viewtool.AIViewTool

ViewTool implementation

## ChatGPT APIs

### Text generator


## Velocity usage

```
#set( $result = $ai.textGenerate("Some text") )
<ul>
<li>$result.httpStatus</li>
<li>$result.request</li>
<li>$result.response</li>
</ul>



#set( $result = $ai.imageGenerate($result.response) )
<ul>
<li>$result.httpStatus</li>
<li>$result.request</li>
<li>$result.response</li>
</ul>
```
