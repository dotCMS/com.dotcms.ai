```
curl -XPOST -k -H"Authorization: Bearer $AUTH_TOK" https://auth.dotcms.com/api/v1/ai/embeddings \
-H "Content-Type: application/json" \
-d '{
"query": "+contentType:(Blog OR Partners OR course OR DotcmsDocumentation OR Testimonial OR feature OR caseStudy OR Dotcmsbuilds)",
"indexName": "testing"
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


```
