## Semantic Search
### SearchTool

The search tool can be accessed using `$ai.search`

### Search an index with a content/query 
This example searches the blog index for matching content:
```vtl
## Run a semantic query
#set($results = $ai.search.query("Where can I find the Best beaches?", "blogIndex"))
total: $results.count
limit: $results.limit
offset: $results.offset
threshold: $results.threshold

$results.query
#foreach($result in $results.dotCMSResults)
    $result.title
    #foreach($m in $result.matches)
        - $m.distance : $m.extractedText
    #end
#end

```

### Search with parameters
You can also build a search map by hand:

```vtl
## A semantic quer using a Map of Parameters
#set($query = {})
$!query.put("query", "Where can I find the Best beaches?")
$!query.put("contentType", "Blog")
$!query.put("indexName", "blogIndex")
$!query.put("limit", "100")
$!query.put("offset", "0")
## $!query.put("host", "SYSTEM_HOST")
## $!query.put("language", "1")
## $!query.put("fieldVar", "content")
#set($results = $ai.search.query($query ))
total: $results.count
limit: $results.limit
offset: $results.offset
threshold: $results.threshold

$results.query
#foreach($result in $results.dotCMSResults)
    $result.title
    #foreach($m in $result.matches)
        - $m.distance : $m.extractedText
    #end
#end
```

### Find Related Content
You can easily use this to automatically find related content based on an existing content. 
The method will try to find the most "content-rich" field from the content you have passed 
in and use it to find other content that is semantically related to the original.

```vtl

##  Finding related Content
#set($content = $dotcontent.find("48ec3192-5f04-466b-b7cd-7134f3ea4d67"))

## send in a contentlet to find related content in the index "blog"
#set($relatedContent = $ai.search.related($content, "blogIndex"))

#foreach($result in $relatedContent.dotCMSResults)
    $result.title : $result.inode
    #foreach($m in $result.matches)
        - $m.distance : $m.extractedText
    #end
#end

```


## Embedding Utils

### Count the tokens in a string 
```vtl
$ai.embeddings.countTokens("this should be about 7 tokens")
```

### Generate the Embeddings
This checks cache to see if we have already generated the embeddings for 
this string and if not, makes a call to openai to get the embeddings 
```vtl
$ai.embeddings.generateEmbeddings("this should be about 7 tokens")
```

### See existing content index information
```vtl
$ai.embeddings.indexCount
```


## Completions


### Config
Show the current config for completions, including the model and the prompt templates

$ai.completions.config

### Summarize

This method takes a prompt, and using the configured model and prompt templates, will return a summary object as json.

```vtl
#set($summary = $ai.completions.summarize("Where can I find the Best beaches?", "blogIndex")))

model: $summary.model
prompt: $summary.usage.prompt_tokens
tokens: $summary.usage.total_tokens
$summary.choices.get(0).message.content

```

### Prompt

This method takes an arbitrary  prompt, and will return the openai response object as json.




#set($summary = $ai.completions.summarize("Where can I find the Best beaches?", "blogIndex")))



