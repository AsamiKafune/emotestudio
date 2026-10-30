package moe.kfn.emotestudio;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(emotestudio.MODID)
public class emotestudio {
    public static final String MODID = "emotestudio";
    public static final Logger LOGGER = LogUtils.getLogger();

    public emotestudio(IEventBus modEventBus, ModContainer modContainer) {
        if (FMLEnvironment.dist.isClient()) moe.kfn.emotestudio.mcp.McpServer.start();
    }
}
