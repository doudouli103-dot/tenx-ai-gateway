package com.tenx.ai.gateway.api;

import com.tenx.ai.gateway.runtime.ModelRuntimeOperationResult;
import com.tenx.ai.gateway.runtime.ModelRuntimeService;
import com.tenx.ai.gateway.runtime.ModelRuntimeStatus;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 管理员运行时控制入口，处理 {@code /admin/models*} 三个接口。
 *
 * <p>底层 service 内部会执行阻塞操作（HTTP 健康检查、进程命令），
 * 因此这里统一用 {@code boundedElastic} 调度器把阻塞调用移出 reactor 事件循环线程，
 * 避免拖慢其它请求。
 */
@RestController
public class ModelRuntimeController {

    /** 运行时管理服务，承载列表/启动/停止逻辑。 */
    private final ModelRuntimeService service;

    /** 构造运行时控制入口。 */
    public ModelRuntimeController(ModelRuntimeService service) {
        this.service = service;
    }

    /** 返回所有模型的运行时状态列表。 */
    @GetMapping("/admin/models")
    public Mono<ResponseEntity<List<ModelRuntimeStatus>>> models() {
        return blocking(() -> service.listModels()).map(ResponseEntity::ok);
    }

    /** 启动指定模型的运行时。 */
    @PostMapping("/admin/models/{model:.+}/start")
    public Mono<ResponseEntity<ModelRuntimeOperationResult>> start(@PathVariable String model) {
        return blocking(() -> service.start(model)).map(ResponseEntity::ok);
    }

    /** 停止指定模型的运行时。 */
    @PostMapping("/admin/models/{model:.+}/stop")
    public Mono<ResponseEntity<ModelRuntimeOperationResult>> stop(@PathVariable String model) {
        return blocking(() -> service.stop(model)).map(ResponseEntity::ok);
    }

    /** 把阻塞调用包装成在 boundedElastic 调度器上执行的 Mono。 */
    private <T> Mono<T> blocking(java.util.concurrent.Callable<T> callable) {
        return Mono.fromCallable(callable).subscribeOn(Schedulers.boundedElastic());
    }
}
