## Semantic Search
### SearchTool

The search tool can be accessed using `$ai.search`


This example searches the blog index for matching content:
```vtl
## Run a semantic query
#set($searchResults = $ai.search.query("Where can I find the Best beaches?", "blogIndex"))
total: $searchResults.count
limit: $searchResults.limit
offset: $searchResults.offset
threshold: $searchResults.threshold

$searchResults.query
#foreach($result in $searchResults.results)
    $result.title
    #foreach($m in $result.matches)
        - $m.distance : $m.extractedText
    #end
#end

```

### Semantic Search with a bunch of parameters
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
#set($searchResults = $ai.search.query($query ))
total: $searchResults.count
limit: $searchResults.limit
offset: $searchResults.offset
threshold: $searchResults.threshold

$searchResults.query
#foreach($result in $searchResults.results)
    $result.title : $result.inode
    #foreach($m in $result.matches)
        - $m.distance : $m.extractedText
    #end
#end
```

### Find Related Content
You can easily use this to automatically find related content
```vtl

##  Finding related Content
#set($content = $dotcontent.find("48ec3192-5f04-466b-b7cd-7134f3ea4d67"))

## send in a contentlet to find related content in the index "blog"
#set($relatedContent = $ai.search.related($content, "blogIndex"))

#foreach($result in $relatedContent.results)
    $result.title : $result.inode
    #foreach($m in $result.matches)
        - $m.distance : $m.extractedText
    #end
#end

```
