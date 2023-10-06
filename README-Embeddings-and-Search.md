

experiments.enabled=true
experiments.auto.js.injection=true

## Semantic Search
### Embeddings

Before you can use semantic searching you need to generate "embeddings" for the content you want to search.  Embeddings are dense numerical representation of content (text,images or anything that can be broken down into numbers) expressed as vectors.  Each vector describes some category of the content  and can be used to provide semantic similarity between different content objects.

To generate embeddings in dotCMS you can pass a content query and dotCMS will pull matching content and pass that content to OpenAI to generate the embeddings for that content.  These embeddings are stored in the dotCMS database under a table called `dot_embeddings`.  Th

When we do a semantic search of content in dotCMS, we are passing the query to OpenAI to generating the embeddings for that query.   


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
"indexName": "blog",
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



### Searching/Querying Content
`GET|POST  /v1/ai/search`

This endpoint allows you to semantically search the embeddings.  It will return a list of matching content AND the specific parts of the content that matched your query.


https://local.dotcms.site:8443/api/v1/ai/search?query=where+can+I+find+the+best+beaches&indexName=blog

```
curl -XPOST -k -H"Authorization: Bearer $TOK" https://local.dotcms.site:8443/api/v1/ai/search \
-H "Content-Type: application/json" \
-d '{
"query": "where can I find the best beaches",
"indexName": "blog",
"operator": "cosine"
}'
```


### Summarizing Content
Summary / Chat Bot Completions
Finally, you can use ChatGPT to "answer" the query as a question. This takes the results from the semantic search and has ChatGPT summarize them for us.  You can see the results below.

Summarize
https://auth.dotcms.com/api/v1/ai/summarize/query?query=how+do+I+use+markdown
```
curl -XPOST -k -H"Authorization: Bearer $TOK" https://local.dotcms.site:8443/api/v1/ai/summarize \
-H "Content-Type: application/json" \
-d '{
"query": "what are some of the the best beaches?",
"indexName": "blog",
"operator": "cosine"
}'
```
