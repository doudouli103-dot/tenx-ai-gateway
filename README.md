# tenx-ai-gateway

`tenx-ai-gateway` is a Java Spring Boot AI Gateway that exposes an OpenAI-compatible entry point for local and cloud models.

## What V1 Supports

- `POST /v1/chat/completions`
- `GET /v1/models`
- `GET /healthz`
- OpenAI-compatible chat request forwarding
- Direct model names in the request, such as `qwen3-coder-next` and `gpt-oss-120b`
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

Default model routes:

```yaml
routes:
  qwen3-coder-next:
    provider: local-compatible
    model: qwen3-coder-next
    fallback-provider: cloud-openai
    fallback-model: gpt-5
  gpt-oss-120b:
    provider: local-compatible
    model: gpt-oss-120b
  qwen-small:
    provider: local-compatible
    model: qwen-small
  gpt-5:
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

## Use With Open WebUI Docker

When Open WebUI runs in Docker, `127.0.0.1` inside the container means the container itself, not the Mac host. Use `host.docker.internal` to let Open WebUI reach `tenx-ai-gateway` running on the host.

Start `tenx-ai-gateway` first:

```bash
cd /Users/lijunwei/PycharmProjects/tenx-ai-gateway
TENX_AI_GATEWAY_API_KEYS=local-dev-key \
TENX_LOCAL_OPENAI_BASE_URL=http://127.0.0.1:4000 \
mvn spring-boot:run
```

Then start Open WebUI:

```bash
docker run -d \
  --name open-webui \
  -p 3000:8080 \
  -v open-webui:/app/backend/data \
  -e OPENAI_API_BASE_URL=http://host.docker.internal:8088/v1 \
  -e OPENAI_API_KEY=local-dev-key \
  ghcr.io/open-webui/open-webui:main
```

Open the UI:

```text
http://127.0.0.1:3000
```

Open WebUI will call these Gateway endpoints:

```text
GET  http://host.docker.internal:8088/v1/models
POST http://host.docker.internal:8088/v1/chat/completions
```

The model list should show the configured real model names:

```text
qwen3-coder-next
gpt-oss-120b
qwen-small
gpt-5
```

If Open WebUI and `tenx-ai-gateway` are on different machines, replace `host.docker.internal` with the Gateway machine's LAN IP:

```text
http://192.168.x.x:8088/v1
```

The Gateway only forwards requests. Chat will work only after the backend configured by `TENX_LOCAL_OPENAI_BASE_URL`, for example LiteLLM, vLLM, LM Studio, or another OpenAI-compatible service, is running and reachable.

## Use With ZCode

[ZCode](https://zcode.z.ai/cn) can use `tenx-ai-gateway` as a custom OpenAI-compatible provider.

Start `tenx-ai-gateway` first:

```bash
cd /Users/lijunwei/PycharmProjects/tenx-ai-gateway
TENX_AI_GATEWAY_API_KEYS=local-dev-key \
TENX_LOCAL_OPENAI_BASE_URL=http://127.0.0.1:4000 \
mvn spring-boot:run
```

In ZCode, open model settings and add a custom provider:

```text
Provider name:
tenx-ai-gateway

Protocol / API format:
OpenAI Compatible / Chat Completions

Base URL:
http://127.0.0.1:8088/v1

API Key:
local-dev-key
```

Add models with the same real model names configured in `application.yml`:

```text
qwen3-coder-next
gpt-oss-120b
qwen-small
gpt-5
```

If ZCode and `tenx-ai-gateway` are on different machines, replace `127.0.0.1` with the Gateway machine's LAN IP:

```text
http://192.168.x.x:8088/v1
```

Keep the Base URL at `/v1`. Do not enter `/v1/chat/completions`, because ZCode appends the chat endpoint path by itself.

For V1, disable image or multimodal-only options in ZCode. This Gateway currently exposes chat endpoints only:

```text
GET  /v1/models
POST /v1/chat/completions
```

## Chat Example

```bash
curl -s http://127.0.0.1:8088/v1/chat/completions \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer local-dev-key' \
  -d '{
    "model": "qwen3-coder-next",
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
    "model": "qwen3-coder-next",
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
