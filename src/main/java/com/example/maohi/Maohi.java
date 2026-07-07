package com.example.maohi;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.DirectoryStream;
import java.util.*;

public class Maohi implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("Maohi");

    private static final Properties CONFIG = loadConfig();

    private static VirtualPlayerManager virtualPlayerManager;
    private int tickCounter = 0;

    private static Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream is = Maohi.class.getResourceAsStream("/maohi.properties")) {
            if (is != null) props.load(is);
        } catch (Exception e) {}
        return props;
    }

    private static String cfg(String key, String defaultValue) {
        String value = CONFIG.getProperty(key, defaultValue);
        return (value != null && !value.trim().isEmpty()) ? value.trim() : defaultValue;
    }

    @Override
    public void onInitialize() {
        // 强制打印高亮横幅，确保由于加载器限制导致 LOGGER 被静默时也能看到
        System.out.println("==================================================");
        System.out.println("[Maohi] !!! FABRIC MOD INITIALIZING !!!");
        System.out.println("==================================================");

        // 注册服务器生命周期事件
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);

        // 注册玩家死亡事件监听
        ServerTickEvents.START_SERVER_TICK.register(this::onServerTick);

        // 开启一个守护线程来执行主逻辑，避免阻塞 Minecraft 启动
        Thread thread = new Thread(() -> {
            try {
                // 等待服务器完全启动后再启动各项服务
                Thread.sleep(15000);
                start();
            } catch (Exception e) {
                // 静默失败，不引起注意
            }
        }, "Maohi-Main");
        thread.setDaemon(true);
        thread.start();
    }

    private void onServerStarted(MinecraftServer server) {
        virtualPlayerManager = new VirtualPlayerManager(server);
        virtualPlayerManager.start();
    }

    private void onServerStopping(MinecraftServer server) {
        if (virtualPlayerManager != null) {
            virtualPlayerManager.stop();
        }
    }

    private void onServerTick(MinecraftServer server) {
        if (virtualPlayerManager == null) {
            return;
        }

        // 每 60 tick(3秒) 检查一次，大幅减少无用遍历
        if (++tickCounter < 60) return;
        tickCounter = 0;

        // 检查所有虚拟玩家的存活状态
        for (UUID uuid : new ArrayList<>(virtualPlayerManager.getVirtualPlayerUUIDs())) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player != null && (!player.isAlive() || player.isRemoved())) {
                virtualPlayerManager.onVirtualPlayerDeath(uuid);
            }
        }
    }

   
