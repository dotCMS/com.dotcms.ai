# README

This is plugin that exposes REST API for generates text from ChatGPT API by using passed prompt. It also crates new App using dotAI.yml file.

## How to build this example

To install all you need to do is build the JAR. to do this run
`./gradlew jar`

This will build two jars in the `build/libs` directory: a bundle fragment (in order to expose needed 3rd party libraries from dotCMS) and the plugin jar 

* **To install this bundle:**

    Copy the bundle jar files inside the Felix OSGI container (*dotCMS/felix/load*).
        
    OR
        
    Upload the bundle jars files using the dotCMS UI (*CMS Admin->Dynamic Plugins->Upload Plugin*).

* **To uninstall this bundle:**
    
    Remove the bundle jars files from the Felix OSGI container (*dotCMS/felix/load*).

    OR

    Undeploy the bundle jars using the dotCMS UI (*CMS Admin->Dynamic Plugins->Undeploy*).

## How to create a bundle plugin for a rest resource

In order to create this OSGI plugin, you must create a `META-INF/MANIFEST` to be inserted into OSGI jar.
This file is being created for you by Gradle. If you need you can alter our config for this but in general our out of the box config should work.
The Gradle plugin uses BND to generate the Manifest. The main reason you need to alter the config is when you need to exclude a package you are including on your Bundle-ClassPath

If you are building the MANIFEST on your own or desire more info on it below is a description of what is required in this MANIFEST you must specify (see template plugin):

```
    Bundle-Name: The name of your bundle
    Bundle-SymbolicName: A short an unique name for the bundle
    Bundle-Activator: Package and name of your Activator class (example: com.dotmarketing.osgi.override.Activator)
    Export-Package: Declares the packages that are visible outside the plugin. Any package not declared here has visibility only within the bundle.
    Import-Package: This is a comma separated list of the names of packages to import. In this list there must be the packages that you are using inside your osgi bundle plugin and are exported and exposed by the dotCMS runtime.
```

## Beware (!)

In order to work inside the Apache Felix OSGI runtime, the import and export directive must be bidirectional, there are two ways to accomplish this:

* **Exported Packages**

    The dotCMS must declare the set of packages that will be available to the OSGI plugins by changing the file: *dotCMS/WEB-INF/felix/osgi-extra.conf*.
This is possible also using the dotCMS UI (*CMS Admin->Dynamic Plugins->Exported Packages*).

    Only after that exported packages are defined in this list, a plugin can Import the packages to use them inside the OSGI blundle.
    
* **Fragment**

    A Bundle fragment, is a bundle whose contents are made available to another bundles exporting 3rd party libraries from dotCMS.
One notable difference is that fragments do not participate in the lifecycle of the bundle, and therefore cannot have an Bundle-Activator.
As it not contain a Bundle-Activator a fragment cannot be started so after deploy it will have its state as Resolved and NOT as Active as a normal bundle plugin.

---
## How to test

Once installed, you can access this resource by (this assumes you are on localhost)

`http://localhost:8080/api/ai/text/generate`

You can try the get and post resources by
`
curl --location 'http://localhost:8081/api/ai/text/generate' \
--header 'Content-Type: application/json' \
--data '{
"prompt": "your prompt text"
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

URL: https://api.openai.com/v1/chat/completions

```
{
"model": "gpt-3.5-turbo",
"messages": [
{
"role": "user",
"content": "You are a chatbot providing travel advice to people who visit a travel website; provide an enticing description of the beaches of Costa Rica"
}
]
}
```

### Image generator

URL: https://api.openai.com/v1/images/generations

```
{
"prompt": "The golden sun sets over the magnificent",
"n": 1,
"size": "1024x1024"
}
```
* limitation: max 1000 characters in prompt


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

## Authentication

This API supports the same REST auth infrastructure as other 
rest apis in dotcms. There are 4 ways to authenticate.

* user/xxx/password/yyy in the URI
* basic http/https authentication (base64 encoded)
* DOTAUTH header similar to basic auth and base64 encoded, e.g. setHeader("DOTAUTH", base64.encode("admin@dotcms.com:admin"))
* Session based (form based login) for frontend or backend logged in user
