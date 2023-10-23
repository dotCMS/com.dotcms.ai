```
curl -XPOST -k -H"Authorization: Bearer $AUTH_TOK" https://auth.dotcms.com/api/v1/ai/embeddings \
-H "Content-Type: application/json" \
-d '{
"query": "+contentType:(Blog OR Partners OR course OR DotcmsDocumentation OR Testimonial OR feature OR caseStudy OR Dotcmsbuilds)",
"indexName": "contentIndex"
}'

curl -XPOST -k -H"Authorization: Bearer $AUTH_TOK" https://auth.dotcms.com/api/v1/ai/search \
-H "Content-Type: application/json" \
-d '{
"query": "how do I create a template?",
"fields":"title,inode",
"threshold":".2",
"searchLimit":50
}'

curl -XPOST -k -H"Authorization: Bearer $AUTH_TOK" https://auth.dotcms.com/api/v1/ai/summarize \
-H "Content-Type: application/json" \
-d '{
"query": "how can I build a template",
"threshold":".2",
"searchLimit":500,
"stream":true
}'


curl  -k -H"Authorization: Bearer $AUTH_TOK" https://auth.dotcms.com/api/v1/ai/embeddings/count \
-H "Content-Type: application/json" 


curl -XDELETE -k -H"Authorization: Bearer $AUTH_TOK" https://auth.dotcms.com/api/v1/ai/embeddings/ \
-H "Content-Type: application/json" \
-d '{
"contentType" : "DotcmsDocumentation",
"fieldVar": "DotcmsDocumentation"
}'

curl -XPOST -k -H"Authorization: Bearer $AUTH_TOK" https://auth.dotcms.com/api/v1/ai/embeddings \
-H "Content-Type: application/json" \
-d '{\n"query": "+contentType:(Blog OR Partners OR course OR DotcmsDocumentation OR Testimonial OR feature OR caseStudy OR Dotcmsbuilds)",
"indexName": "testing"
}'


curl  -k -u"admin@dotcms.com:admin" https://local.dotcms.site:8443/api/v1/ai/embeddings/count \
-H "Content-Type: application/json"

curl -XDELETE -k -u"admin@dotcms.com:admin" https://local.dotcms.site:8443/api/v1/ai/embeddings/db \
-H "Content-Type: application/json"



curl -XPOST -k -u"admin@dotcms.com:admin" https://localhost:8443/api/v1/ai/embeddings \
-H "Content-Type: application/json" \
-d '{
"query": "+variant:default +live:true"
}'


curl -XPOST -k -u"admin@dotcms.com:admin" https://localhost:8443/api/v1/ai/search \
-H "Content-Type: application/json" \
-d '{
"query": "what is the best beach?",
"fields":"title,inode"
}'

curl -XPOST -k -u"admin@dotcms.com:admin" https://localhost:8443/api/v1/ai/embeddings/count \
-H "Content-Type: application/json" 


```


select count(*) as test, index_name from dot_embeddings where true  and host='48190c8c-42c4-46af-8d1a-0cd5db894797'  and index_name='default'  group by index_name  


curl -XDELETE -k -H"Authorization: Bearer $AUTH_TOK" https://auth.dotcms.com/api/v1/ai/embeddings/db \
-H "Content-Type: application/json"

