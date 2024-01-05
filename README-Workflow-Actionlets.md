## Workflow Actionlets

The plugin provides 4 workflow actionlets that can be attached to any workflow process in dotCMS.
These are

### AI Embeddings - DotEmbeddingsActionlet

This actionlet can automatically add or remove content from the `dot_embeddings` semantic search
index. You can specify one or more content types separated by line breaks or by commas. The
actionlet tries to intelligently select which field or fields should be read for indexing. In the
case of content, it will index the first StoryBlock, WYSIWYG or TextArea field. For pages, it will
try to render the
page and use the resultant HTML. For fileAssets and dotAssets, it will index the first binary field
it finds. You can also specify the content type's field that you wish to index if needed. For
example:

```
blog.blogContent
news
webPageContent
document.fileAsset
```

would mean that any `blog`, `news`, `webPageContent` or `document` passed through this workflow
actionlet would be added to the index. In the case of a `blog`, it would index the
field `blogcontent` and in the case of `document`, it would index the file found under
the `fileAsset` field.


### AI Content Prompt - OpenAIContentPromptActionlet

This actionlet can be used to automatically modify content based on a prompt and/or the properties in
the content itself. The prompt that is passed to OpenAI is a velocity template that gets merged with
the content that is passing through the workflow step.
The response returned by OpenAI can be stored into a field, or, if OpenAIs response is a json
object, can be used to update mutliple fields in the content itself. Take the following prompt as an
example:

> Generate an article for SEO. The article should describe  """\n$contentlet.topic\n""". Return this
> article as RFC8259 compliant JSON with 3 properties, "title", for the article title, "blog" for
> the article content, and "urlSlug" which would contain the article title value all
> lowercase with any special characters removed and dashes instead of spaces between the words. The
> article content should be in HTML. Try to use the keywords "$contentlet.keywords" as often as
> possible. Make the article at least 1500 words long with an informative, friendly tone of voice
> and try to write the article in such a way that it will not be detectable as having been written by
> AI.

This prompt will replace/include the content values for $topic and $keywords and send this as a
prompt to OpenAI. Because we ask OpenAI to response with a JSON object, we can use the values that
are returned in the object to populate the title, slug and blog fields of our content.

This action generally runs asynchronously in the background as generating content can take some time.

### AI Generate Image - OpenAIGenerateImageActionlet

This actionlet can be used to automatically generate an image based on the given the prompt. You can
use any value from the `$contentlet` object or you can use a special value `$contentletToString`
which tries to intelligently render a content object as a string value depending on its type.

### AI Auto-tag Content - OpenAIAutoTagActionlet

This actionlet converts the content in the workflow to a string and then submits it to OpenAI to
tag. You can "limit" the tags to what already exists in dotCMS (the top 1000 tags) which are sent to
OpenAI as suggestions. The results will be appended to your contents `tag` field.
