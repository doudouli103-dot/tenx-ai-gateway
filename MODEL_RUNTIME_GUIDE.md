# 本地模型启动与访问指南

本文档记录 Mac Studio 上本地模型运行时的启动命令，以及 MacBook 和 `tenx-ai-gateway` 的访问地址。

## 主机与端口

Mac Studio 当前局域网信息：

```text
IP:       192.168.1.12
主机名:   lijunweideMac-Studio.local
```

| 服务 | 端口 | Mac Studio 本机地址 | MacBook 访问地址 |
| --- | ---: | --- | --- |
| `llama-server` | `4000` | `http://127.0.0.1:4000` | `http://192.168.1.12:4000` |
| OpenAI 兼容文本 API | `4000` | `http://127.0.0.1:4000/v1` | `http://192.168.1.12:4000/v1` |
| ComfyUI | `8188` | `http://127.0.0.1:8188` | `http://192.168.1.12:8188` |

MacBook 也可以通过主机名访问：

```text
http://lijunweideMac-Studio.local:4000
http://lijunweideMac-Studio.local:8188
```

局域网 IP 可能因 DHCP 发生变化。长期使用时优先使用 `.local` 主机名，或在路由器中为 Mac Studio 设置固定地址。

## 文本模型

文本模型由 `llama.cpp` 的 `llama-server` 提供 OpenAI 兼容 API。三个模型共用端口 `4000`，每次只启动一个。切换模型前，在原服务终端按 `Ctrl+C` 停止服务。

通用配置：

```text
API Key: local-dev-key
API Base URL: http://127.0.0.1:4000/v1
MacBook API Base URL: http://192.168.1.12:4000/v1
```

### Qwen3 8B

模型别名：`qwen-small`

```bash
/Users/lijunwei/ljwai/llama.cpp/build/bin/llama-server \
  --model /Users/lijunwei/ljwai/tenx-ai-models/llama-cpp/qwen-small/Qwen3-8B-Q4_K_M.gguf \
  --alias qwen-small \
  --host 0.0.0.0 \
  --port 4000 \
  --ctx-size 32768 \
  --parallel 1 \
  --n-gpu-layers all \
  --flash-attn on \
  --api-key local-dev-key
```

访问路径：

```text
Web UI: http://192.168.1.12:4000
API:    http://192.168.1.12:4000/v1
Model:  qwen-small
```

### Qwen3-Coder-Next

模型别名：`qwen3-coder-next`

```bash
/Users/lijunwei/ljwai/llama.cpp/build/bin/llama-server \
  --model /Users/lijunwei/ljwai/tenx-ai-models/llama-cpp/qwen3-coder-next/Qwen3-Coder-Next-Q4_K_M/Qwen3-Coder-Next-Q4_K_M-00001-of-00004.gguf \
  --alias qwen3-coder-next \
  --host 0.0.0.0 \
  --port 4000 \
  --ctx-size 32768 \
  --parallel 1 \
  --n-gpu-layers all \
  --flash-attn on \
  --api-key local-dev-key
```

只需指定第一片 GGUF，其余三片会自动加载。

访问路径：

```text
Web UI: http://192.168.1.12:4000
API:    http://192.168.1.12:4000/v1
Model:  qwen3-coder-next
```

### GPT-OSS 120B

模型别名：`gpt-oss-120b`

```bash
/Users/lijunwei/ljwai/llama.cpp/build/bin/llama-server \
  --model /Users/lijunwei/ljwai/tenx-ai-models/llama-cpp/gpt-oss-120b/gpt-oss-120b-MXFP4-00001-of-00002.gguf \
  --alias gpt-oss-120b \
  --host 0.0.0.0 \
  --port 4000 \
  --ctx-size 32768 \
  --parallel 1 \
  --n-gpu-layers all \
  --flash-attn on \
  --api-key local-dev-key
```

只需指定第一片 GGUF，第二片会自动加载。如果出现内存压力，将 `--ctx-size 32768` 调整为 `--ctx-size 16384`。

访问路径：

```text
Web UI: http://192.168.1.12:4000
API:    http://192.168.1.12:4000/v1
Model:  gpt-oss-120b
```

### 文本服务检查

健康检查：

```bash
curl http://192.168.1.12:4000/health \
  -H 'Authorization: Bearer local-dev-key'
```

查询当前模型：

```bash
curl http://192.168.1.12:4000/v1/models \
  -H 'Authorization: Bearer local-dev-key'
```

调用示例：

```bash
curl http://192.168.1.12:4000/v1/chat/completions \
  -H 'Authorization: Bearer local-dev-key' \
  -H 'Content-Type: application/json' \
  -d '{
    "model": "qwen-small",
    "messages": [
      {"role": "user", "content": "你好"}
    ]
  }'
```

调用其他模型时，将请求中的 `model` 修改为当前启动的模型别名。

## 图像和视频模型

Flux、Qwen-Image、Wan 2.2 和 HunyuanVideo 不分别启动。它们共用一个 ComfyUI 服务，并由工作流按需加载。

### 创建 Conda 环境

环境只需创建一次：

```bash
conda create -n comfyui python=3.12 pip -y
conda activate comfyui
cd /Users/lijunwei/ljwai/tenx-ai-models/comfyui/ComfyUI
python -m pip install --upgrade pip setuptools wheel
python -m pip install -r requirements.txt
```

如果不接受 Anaconda 默认仓库条款，可使用 `conda-forge` 创建环境：

```bash
conda create \
  -n comfyui \
  --override-channels \
  -c conda-forge \
  python=3.12 \
  pip \
  -y
```

验证 Apple GPU：

```bash
conda activate comfyui
python -c "import torch; print(torch.__version__); print('MPS:', torch.backends.mps.is_available())"
```

预期输出中应包含：

```text
MPS: True
```

### 启动 ComfyUI

先激活环境：

```bash
conda activate comfyui
cd /Users/lijunwei/ljwai/tenx-ai-models/comfyui/ComfyUI

PYTORCH_ENABLE_MPS_FALLBACK=1 \
caffeinate -dimsu \
python main.py \
  --listen 0.0.0.0 \
  --port 8188 \
  --reserve-vram 8 \
  --input-directory /Volumes/LJW/tenx-ai/comfyui/input \
  --output-directory /Volumes/LJW/tenx-ai/comfyui/output
```

也可以不激活环境，直接使用 `conda run`：

```bash
cd /Users/lijunwei/ljwai/tenx-ai-models/comfyui/ComfyUI

PYTORCH_ENABLE_MPS_FALLBACK=1 \
caffeinate -dimsu \
conda run --no-capture-output -n comfyui \
  python main.py \
  --listen 0.0.0.0 \
  --port 8188 \
  --reserve-vram 8 \
  --input-directory /Volumes/LJW/tenx-ai/comfyui/input \
  --output-directory /Volumes/LJW/tenx-ai/comfyui/output
```

以上命令将上传的输入素材和工作流直接生成的结果保存到外接 HDD。启动前确保 `/Volumes/LJW` 已挂载，并提前创建相关目录：

```bash
mkdir -p \
  /Volumes/LJW/tenx-ai/comfyui/input \
  /Volumes/LJW/tenx-ai/comfyui/output \
  /Volumes/LJW/tenx-ai/comfyui/adapter-results \
  /Volumes/LJW/tenx-ai/comfyui/archive
```

`adapter-results` 供 adapter 或客户端下载结果使用，`archive` 用于长期存档；两者不是 ComfyUI 原生命令行参数。

访问路径：

```text
Mac Studio: http://127.0.0.1:8188
MacBook:    http://192.168.1.12:8188
主机名:     http://lijunweideMac-Studio.local:8188
```

首次启动时，如果 macOS 防火墙询问是否允许网络连接，选择“允许”。不要将端口 `8188` 直接映射到公网，因为 ComfyUI 默认没有登录认证。

### ComfyUI 模型组合

| 模型 | 主模型 | 配套文件 |
| --- | --- | --- |
| Flux.1 Dev | `models/checkpoints/flux1-dev-fp8.safetensors` | 由 Flux 工作流加载；如果提示缺少 CLIP、T5 或 VAE，需要补齐对应组件 |
| Qwen-Image | `models/diffusion_models/qwen_image_fp8_e4m3fn.safetensors` | `models/text_encoders/qwen_2.5_vl_7b_fp8_scaled.safetensors`、`models/vae/qwen_image_vae.safetensors` |
| Wan 2.2 TI2V 5B | `models/diffusion_models/wan2.2_ti2v_5B_fp16.safetensors` | `models/text_encoders/umt5_xxl_fp8_e4m3fn_scaled.safetensors`、`models/vae/wan2.2_vae.safetensors` |
| HunyuanVideo 1.5 | `models/diffusion_models/hunyuanvideo1.5_720p_t2v_fp16.safetensors` | `models/text_encoders/qwen_2.5_vl_7b_fp8_scaled.safetensors`、`models/vae/hunyuanvideo15_vae_fp16.safetensors` |

`models/text_encoders/byt5_small_glyphxl_fp16.safetensors` 是辅助文本编码器，不能作为独立生成服务启动。

## BGE-M3

模型目录：

```text
/Users/lijunwei/ljwai/tenx-ai-models/bge-m3
```

BGE-M3 是向量嵌入模型，不属于 `llama-server` 或 ComfyUI。目前尚未配置独立 HTTP 服务，因此没有现成访问地址。需要使用单独的 embedding 服务框架后，再接入 Gateway。

## Gateway 配置

当 `tenx-ai-gateway` 与模型服务运行在同一台 Mac Studio 时，推荐使用回环地址，避免依赖局域网 IP：

```bash
export TENX_LOCAL_OPENAI_BASE_URL=http://127.0.0.1:4000
export TENX_LOCAL_OPENAI_API_KEY=local-dev-key
```

ComfyUI 不直接向 Gateway 暴露 OpenAI 兼容图片或视频接口。Gateway 应通过对应 adapter 访问：

```bash
export TENX_IMAGE_OPENAI_BASE_URL=http://127.0.0.1:4010
export TENX_VIDEO_OPENAI_BASE_URL=http://127.0.0.1:4020
```

运行关系：

```text
Chat:
tenx-ai-gateway -> llama-server:4000

Image:
tenx-ai-gateway -> image-adapter:4010 -> ComfyUI:8188

Video:
tenx-ai-gateway -> video-adapter:4020 -> ComfyUI:8188
```

## 停止服务

前台运行的 `llama-server` 或 ComfyUI 都可以在对应终端按 `Ctrl+C` 停止。
