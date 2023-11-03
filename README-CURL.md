### Create embeddings index for content 

Before you can use semantic searching you need to generate an "embeddings" index for the content you want to search.  Embeddings are dense numerical representation of content (text,images or anything that can be broken down into numbers) expressed as vectors.  Each vector describes some category of the content  and can be used to provide semantic similarity between different content objects.

To generate embeddings in dotCMS you can pass a content query and dotCMS will pull matching content and pass that content to OpenAI to generate the embeddings for that content.  These embeddings are stored in the dotCMS database under a table called `dot_embeddings`.  Th


You can specify the following:
- `indexName` for the content embedding index
- lucene query for the content to embed
- fieldVar to use as the source of content embeddings.  If this is not specified, the plugin will try to use its best guess as to which field to use - WYSIWYG, Block Editor, Large Text Area.  Will also work for file asset based content. See value for `com.dotcms.ai.embeddings.build.for.file.extensions`
- limit/offset for the content queries


If no contentType/field is specified, AI Plugin will try to select the "best" content field to use 
### Indexing Content Embeddings
`POST /api/v1/ai/embeddings`

This endpoint is for managing the local database/index of Searchable Embeddings
You can fire new "indexing" jobs by sending it a query of content that you want to generate embeddings for. The `indexName` can be used to segment search indexes into different/arbitrary buckets:

```
curl -XPOST -k -H"Authorization: Bearer $TOK" https://local.dotcms.site:8443/api/v1/ai/embeddings \
-H "Content-Type: application/json" \
-d '{
"query": "+contentType:blog +variant:default +live:true",
"fieldVar": "blogContent",
"indexName": "blogIndex",
"site": "SYSTEM_HOST"
}'
```

#### Deleting Indexed Embeddings
`DELETE /api/v1/ai/embeddings`

You can also delete existing embeddings - this will delete any embeddings made using the content documentation.blogContent field
```
curl -XDELETE -k -H"Authorization: Bearer $TOK" https://local.dotcms.site:8443/api/v1/ai/embeddings \
-H "Content-Type: application/json" \
-d '{
"contentType":"documentation",
"fieldVar": "blogContent"
}'
```


#### Deleting and Recreating the Embeddings DB
`DELETE /api/v1/ai/embeddings/db`

You can also delete and recreate the whole embeddings table -
```
curl -XDELETE -k -H"Authorization: Bearer $TOK" https://local.dotcms.site:8443/api/v1/ai/embeddings/db \
-H "Content-Type: application/json" 

```




### Semantically Searching Content 
You can search by the following:
- query -  (string) the semantic query you want to match against.
- searchLimit - (int) the number of dotCMS results to return
- offset - (int) the offset of the dotCMS results
- site - (string) the site id for the matching content (defaults to any)
- contentType -  (string) the contentType for matching content (defaults to any)
- fieldVar -  (string) the fieldVar for matching content (defaults to any)
- language - (int) the language for the matching content (defaults to default lang)
- indexName - (string) the content embedding index you want to search in (defaults to `default`)
- threshold - (float) how semantically close should the matching content be to your search query?  The lower the threshold, the closer semantically the content matches (zero being there are zero semantic differences).  `.2` or `.25` should suffice. 
- operator -  (string) the database vector operator to use. Should not really be changed, as cosine generally returns the best results.  For a good write up on the diffences between vector similarity operators, see: https://www.pinecone.io/learn/vector-similarity 



```bash
curl -XPOST -k -H"Authorization: Bearer $TOK" https://local.dotcms.site:8443/api/v1/ai/search \
-H "Content-Type: application/json" \
-d '{
"query": "how do I create a template in dotCMS?",
"threshold":".2",
"searchLimit":50
}'
```
```
curl -XPOST -k -H"Authorization: Bearer $TOK" https://local.dotcms.site:8443/api/v1/ai/completions \
-H "Content-Type: application/json" \
-d '{
"prompt": "how do I create a template in dotCMS?",
"threshold":".2",
"searchLimit":50,
"stream": true
}'
```





```bash
curl -XPOST -k -u"admin@dotcms.com:admin" https://local.dotcms.site:8443/api/v1/ai/search \
-H "Content-Type: application/json" \
-d '{
"query": "what is the best beach?",
}'
```


### Searching for content related to another piece of content:

```bash
curl -XPOST -k -u"admin@dotcms.com:admin" https://local.dotcms.site:8443/api/v1/ai/search/related \
-H "Content-Type: application/json" \
-d '{
"inode": "d7741a84-6050-4b9b-9c09-26759a833741",
}'
```






### Content Chatting and Summarization

#### Have ChatGTP summarize the results of your search

Chatting about content involves 
1. doing a semantic content search, 
2. then passing those results to OpenAI to summarize.  

You can see the chat prompt that will be passed to OpenAI in the configuration value:
`com.dotcms.ai.completion.text.prompt`.  It defaults to:

```
Answer this question
"$!{prompt}?"

by using only the information in the following text:
"""
$!{supportingContent}
"""
```
Where the `$prompt` will be the search query and the `$supportingContent` will be replaced with the closest semanticially matching content in dotCMS.


Results can be return as a chat stream or they can be returned and also included as a batch, which also includes the supporting content that drove the chat response.


```bash
curl -XPOST -k -H"Authorization: Bearer $TOK" https://local.dotcms.site:8443/api/v1/ai/completions \
-H "Content-Type: application/json" \
-d '{
"query": "how do I create a template in dotCMS?",
"threshold":".2",
"searchLimit":500,
"stream":true
}'

```



#### Have AI generate an image for you based on a prompt

```bash
curl  -k -H"Authorization: Bearer $TOK" -H "Content-Type: application/json" \
https://local.dotcms.site:8443/api/ai/image/generate -d '
{
"prompt": "The golden sun sets over the magnificent lake",
"n": 1,
"size": "1024x1024"
}'
```

#### Delete and recreate the entire embeddings index/DB
```bash
curl -XDELETE -k -u"admin@dotcms.com:admin" https://local.dotcms.site:8443/api/v1/ai/embeddings/db \
-H "Content-Type: application/json"
```


```bash
curl -XPOST -k -u"admin@dotcms.com:admin" https://local.dotcms.site:8443/api/v1/ai/embeddings/count \
-H "Content-Type: application/json" 

```
