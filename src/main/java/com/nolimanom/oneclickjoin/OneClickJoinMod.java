package com.nolimanom.oneclickjoin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ConnectingScreen;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.text.StringTextComponent;
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

                // Java 8 совместимость — собираем вручную
                List<Widget> widgets = new java.util.ArrayList<>();
                for (Object o : gui.children()) {
                    if (o instanceof Widget) {
                        widgets.add((Widget) o);
                    }
                }

                // Отключаем все кроме своей
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
                // Запускаем notepad.exe с файлом конфига
                Runtime.getRuntime().exec(new String[]{"notepad.exe", configFile.getAbsolutePath()});
            } else {
                // Если файла нет, открываем папку config через проводник
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

    public static class Config {
        public static ForgeConfigSpec.ConfigValue<String> joinButtonText;
        public static ForgeConfigSpec.ConfigValue<String> serverIpPort;
        public static ForgeConfigSpec.ConfigValue<Boolean> disableSettings;

        public static void init() {
            ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
            builder.push("general");

            joinButtonText = builder
                    .comment("Текст кнопки Join")
                    .define("joinButtonText", "Join");

            serverIpPort = builder
                    .comment("IP:порт сервера (например: 127.0.0.1:25565)")
                    .define("serverIpPort", "127.0.0.1:25565");

            disableSettings = builder
                    .comment("Если true, при правом клике кнопки Join настройки не откроются")
                    .define("disableSettings", false);

            builder.pop();

            COMMON_CONFIG = builder.build();
            CONFIG = new Config();
        }
    }
}
