# tenx-ai-gateway

`tenx-ai-gateway` is a Java Spring Boot AI Gateway that exposes an OpenAI-compatible entry point for local and cloud models.

## What V1 Supports

- `POST /v1/chat/completions`
- `GET /v1/models`
- `GET /healthz`
- OpenAI-compatible chat request forwarding
- Model aliases such as `coder`, `general`, `fast`, and `cloud`
- Provider routing through configuration
- One-level fallback for non-streaming and streaming calls
- API Key authentication with `Authorization: Bearer <key>`
- Streaming forwarding when request body contains `"stream": true`

## Runtime

- Java 8+
- Maven 3.6+
- Spring Boot 2.7.x
- Default port: `8088`

## Configuration

The default configuration is in `src/main/resources/application.yml`.

Important environment variables:

```bash
export TENX_AI_GATEWAY_API_KEYS=local-dev-key
export TENX_LOCAL_OPENAI_BASE_URL=http://127.0.0.1:4000
export TENX_LOCAL_OPENAI_API_KEY=
export TENX_CLOUD_OPENAI_BASE_URL=https://api.openai.com
export TENX_CLOUD_OPENAI_API_KEY=your-cloud-key
```

Default model aliases:

```yaml
routes:
  coder:
    provider: local-compatible
    model: qwen3-coder-next
    fallback-provider: cloud-openai
    fallback-model: gpt-5
  general:
    provider: local-compatible
    model: gpt-oss-120b
  fast:
    provider: local-compatible
    model: qwen-small
  cloud:
    provider: cloud-openai
    model: gpt-5
```

Any backend that exposes an OpenAI-compatible `/v1/chat/completions` endpoint can be placed behind this Gateway, including LiteLLM, vLLM, LM Studio, Ollama-compatible proxy services, or cloud providers.

## Start

```bash
cd /Users/lijunwei/PycharmProjects/tenx-ai-gateway
mvn spring-boot:run
```

Health check:

```bash
curl http://127.0.0.1:8088/healthz
```

## Chat Example

```bash
curl -s http://127.0.0.1:8088/v1/chat/completions \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer local-dev-key' \
  -d '{
    "model": "coder",
    "messages": [
      {"role": "system", "content": "You are a senior Java engineer."},
      {"role": "user", "content": "分析这个 Spring Boot 项目"}
    ],
    "temperature": 0.2
  }'
```

## Streaming Example

```bash
curl -N http://127.0.0.1:8088/v1/chat/completions \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer local-dev-key' \
  -d '{
    "model": "coder",
    "stream": true,
    "messages": [
      {"role": "user", "content": "写一个 Java Controller 示例"}
    ]
  }'
```

## Test

```bash
mvn test
```

## Suggested Next Versions

V2:

- Request log table
- Token usage statistics
- Timeout and retry configuration
- Provider health check
- More explicit fallback policy

V3:

- `model=auto`
- Rate limiting
- Multi-client keys
- Cost tracking
- Dashboard

V4:

- Image generation routing
- Video generation task API
- RAG routing
- Prompt and context cache
