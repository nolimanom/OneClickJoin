package com.nolimanom.oneclickjoin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ConnectingScreen;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.MultiplayerScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

import java.awt.*;
import java.io.File;
import java.util.List;

@Mod(OneClickJoinMod.MODID)
public class OneClickJoinMod {
    public static final String MODID = "oneclickjoin";

    public static ForgeConfigSpec COMMON_CONFIG;
    public static Config CONFIG;

    private Button joinButton;

    public OneClickJoinMod() {
        Config.init();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_CONFIG);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.getGui() instanceof MainMenuScreen) {
            try {
                Screen gui = event.getGui();

                joinButton = new Button(
                        gui.width / 2 - 100,
                        gui.height / 2 - 10,
                        200,
                        20,
                        new StringTextComponent(CONFIG.joinButtonText.get()),
                        btn -> connectToServer(gui)
                );

                event.addWidget(joinButton);

                // Java 8 compatibility — manually collect widgets
                List<Widget> widgets = new java.util.ArrayList<>();
                for (Object o : gui.children()) {
                    if (o instanceof Widget) {
                        widgets.add((Widget) o);
                    }
                }

                // Disable all buttons except our joinButton
                for (Widget w : widgets) {
                    if (w != joinButton) {
                        w.active = false;
                        w.visible = false;
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void openConfigFile() {
        File configFile = new File("config/oneclickjoin-common.toml");
        try {
            if (configFile.exists()) {
                // Open the config file using notepad.exe
                Runtime.getRuntime().exec(new String[]{"notepad.exe", configFile.getAbsolutePath()});
            } else {
                // If config file does not exist, open the config folder
                File configFolder = new File("config");
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(configFolder);
                } else {
                    Runtime.getRuntime().exec(new String[]{"explorer.exe", configFolder.getAbsolutePath()});
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onMouseClicked(GuiScreenEvent.MouseClickedEvent.Pre event) {
        if (event.getGui() instanceof MainMenuScreen && joinButton != null) {
            // Right click on join button opens config file
            if (joinButton.isMouseOver(event.getMouseX(), event.getMouseY()) && event.getButton() == 1) {
                if (!CONFIG.disableSettings.get()) {
                    openConfigFile();
                }
                event.setCanceled(true);
            }
        }
    }

    private void connectToServer(Screen parent) {
        String ipPort = CONFIG.serverIpPort.get();
        String[] parts = ipPort.split(":");
        String ip = parts[0];
        String port = (parts.length > 1) ? parts[1] : "25565";

        ServerData data = new ServerData("OneClickJoin Server", ip + ":" + port, false);
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new ConnectingScreen(parent, mc, data));
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        // Redirect the multiplayer screen to the main menu to prevent going back to server list after manual exit
        if (event.getGui() instanceof MultiplayerScreen) {
            event.setGui(new MainMenuScreen());
        }
        // Do not touch DisconnectedScreen so that kick/errors show properly
    }

    public static class Config {
        public static ForgeConfigSpec.ConfigValue<String> joinButtonText;
        public static ForgeConfigSpec.ConfigValue<String> serverIpPort;
        public static ForgeConfigSpec.ConfigValue<Boolean> disableSettings;

        public static void init() {
            ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
            builder.push("general");

            joinButtonText = builder
                    .comment("Join button text")
                    .define("joinButtonText", "Join");

            serverIpPort = builder
                    .comment("Server IP:Port (e.g. 127.0.0.1:25565)")
                    .define("serverIpPort", "127.0.0.1:25565");

            disableSettings = builder
                    .comment("If true, right-clicking the Join button won't open the config file")
                    .define("disableSettings", false);

            builder.pop();

            COMMON_CONFIG = builder.build();
            CONFIG = new Config();
        }
    }
}
