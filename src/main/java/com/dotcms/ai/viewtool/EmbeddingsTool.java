package com.dotcms.ai.viewtool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;

import com.dotcms.ai.api.EmbeddingsAPI;
import com.dotcms.ai.api.EmbeddingsAPIImpl;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.ai.util.Constants;
import com.dotcms.ai.util.EncodingUtil;
import com.dotcms.ai.util.OpenAIModel;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.knuddels.jtokkit.api.Encoding;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;
import com.dotcms.security.apps.AppSecrets;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import io.vavr.Lazy;
import io.vavr.control.Try;

public class EmbeddingsTool implements ViewTool {

    final private HttpServletRequest request;
    final private Host host;
	final private AppConfig app ;


	EmbeddingsTool(Object initData){
		this.request = ((ViewContext) initData).getRequest();
		this.host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(this.request);
		this.app = ConfigService.INSTANCE.config(this.host);
	}



	@Override
    public void init(Object initData) {
        /* unneeded because of constructor */
	}


	public Map<String, String> getConfig() {
		return Map.of(
				AppKeys.EMBEDDINGS_MAX_TOKENS.key,
				this.app.getConfig(AppKeys.EMBEDDINGS_MAX_TOKENS, "4000"),
				AppKeys.EMBEDDINGS_MODEL.key,
				this.app.getConfig(AppKeys.EMBEDDINGS_MODEL, "4000"),
				AppKeys.EMBEDDINGS_SPLIT_AT_WORDS.key,
				this.app.getConfig(AppKeys.EMBEDDINGS_SPLIT_AT_WORDS, "65"),
				AppKeys.EMBEDDINGS_MINIMUM_FILE_SIZE_TO_INDEX.key,
				this.app.getConfig(AppKeys.EMBEDDINGS_MINIMUM_FILE_SIZE_TO_INDEX, "1024"),
				AppKeys.EMBEDDINGS_THREADS.key,
				this.app.getConfig(AppKeys.EMBEDDINGS_THREADS, "3"),
				AppKeys.EMBEDDINGS_THREADS_QUEUE.key,
				this.app.getConfig(AppKeys.EMBEDDINGS_THREADS_QUEUE, "10_000"),
				AppKeys.EMBEDDINGS_THREADS_MAX.key,
				this.app.getConfig(AppKeys.EMBEDDINGS_THREADS_MAX, "10"),
				AppKeys.EMBEDDINGS_CACHE_TTL_SECONDS.key,
				this.app.getConfig(AppKeys.EMBEDDINGS_CACHE_TTL_SECONDS, "1000"),
				AppKeys.EMBEDDINGS_CACHE_SIZE.key,
				this.app.getConfig(AppKeys.EMBEDDINGS_CACHE_SIZE, "1000"),
				AppKeys.EMBEDDINGS_FILE_EXTENSIONS_TO_EMBED.key,
				this.app.getConfig(AppKeys.EMBEDDINGS_FILE_EXTENSIONS_TO_EMBED, "\"pdf\", \"doc\", \"docx\", \"txt\", \"html\"")



		);

	}

	public int countTokens(String prompt){
		Optional<Encoding> optionalEncoding= EncodingUtil.registry.getEncodingForModel(app.getModel());
		if(optionalEncoding.isPresent()){
			return optionalEncoding.get().countTokens(prompt);
		}
		return -1;
	}

	public List<Float> generateEmbeddings(String prompt){
		int tokens = countTokens(prompt);
		int maxTokens = OpenAIModel.resolveModel(ConfigService.INSTANCE.config(host).getConfig(AppKeys.EMBEDDINGS_MODEL,OpenAIModel.TEXT_EMBEDDING_ADA_002.name())).maxTokens;
		if (tokens > maxTokens) {
			Logger.warn(EmbeddingsTool.class, "Prompt is too long.  Maximum prompt size is "+maxTokens+" tokens (roughly ~" + maxTokens*.75 +" words).  Your prompt was " + tokens + " tokens ");
		}
		return EmbeddingsAPI.impl().pullOrGenerateEmbeddings(prompt)._2;
	}

	public Map<String,Long> indexCount(){
		return EmbeddingsAPI.impl().countEmbeddingsByIndex();
	}








}
