package com.example.maohi;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Maohi - Virtual Player System
 * 
 * A Fabric mod that spawns and manages virtual (dummy) players on the Minecraft server.
 * These virtual players appear as real players to other clients, simulating server activity.
 * 
 * Features:
 * - Automatically spawn virtual players on server startup
 * - Automatically respawn dead virtual players
 * - Random but realistic player names
 * - Natural player behavior (looking around, jumping, sprinting)
 * - Proper network connection simulation with fake IPs
 */
public class Maohi implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("Maohi");

    private static VirtualPlayerManager virtualPlayerManager;
    private int tickCounter = 0;

    @Override
    public void onInitialize() {
        System.out.println("==================================================");
        System.out.println("[Maohi] Virtual Player System Initializing");
        System.out.println("==================================================");

        // Register server lifecycle events to manage virtual player lifecycle
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);

        // Register server tick event to monitor player health and respawn dead ones
        ServerTickEvents.START_SERVER_TICK.register(this::onServerTick);
    }

    /**
     * Called when the server fully starts
     * Initialize the virtual player manager
     */
    private void onServerStarted(MinecraftServer server) {
        virtualPlayerManager = new VirtualPlayerManager(server);
        virtualPlayerManager.start();
        LOGGER.info("[Maohi] Virtual player manager started");
    }

    /**
     * Called when the server is shutting down
     * Stop the virtual player manager and remove all virtual players
     */
    private void onServerStopping(MinecraftServer server) {
        if (virtualPlayerManager != null) {
            virtualPlayerManager.stop();
            LOGGER.info("[Maohi] Virtual player manager stopped");
        }
    }

    /**
     * Called on each server tick
     * Monitor the health of virtual players and trigger respawn if needed
     */
    private void onServerTick(MinecraftServer server) {
        if (virtualPlayerManager == null) {
            return;
        }

        // Check every 60 ticks (3 seconds) to reduce unnecessary iteration overhead
        if (++tickCounter < 60) return;
        tickCounter = 0;

        // Check health status of all virtual players
        for (UUID uuid : new ArrayList<>(virtualPlayerManager.getVirtualPlayerUUIDs())) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player != null && (!player.isAlive() || player.isRemoved())) {
                // Player is dead or removed, trigger respawn
                virtualPlayerManager.onVirtualPlayerDeath(uuid);
            }
        }
    }
}
