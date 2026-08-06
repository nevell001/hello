package com.cashier.api.controller;

import com.cashier.api.support.TestContext;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BackupApiControllerTest {

    @Test
    @DisplayName("空请求体执行备份返回 400")
    void executeBackupWithNullBodyReturns400() {
        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/backup/execute");
        BackupApiController.executeBackup(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    @Test
    @DisplayName("空请求体更新备份配置返回 400")
    void updateConfigWithNullBodyReturns400() {
        TestContext ctx = new TestContext().withRequest(HandlerType.PUT, "/api/backup/config");
        BackupApiController.updateConfig(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }
}
