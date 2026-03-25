package divinity.utils;

import divinity.ClientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ClientUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static void sendChatMessage(String text) {
        String messageFormat = "{WHITE}[{CLIENTTHEME}%s{WHITE}] {WHITE}%s";

        Map<String, Color> colorMap = new HashMap<>();
        colorMap.put("{CLIENTTHEME}", ClientManager.getInstance().getMainColor());
        colorMap.put("{WHITE}", new Color(255, 255, 255));

        String formattedMessage = ChatColorUtils.formatWithColors(messageFormat, colorMap);
        formattedMessage = String.format(formattedMessage, ClientManager.getInstance().getName(), text);

        mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(formattedMessage));
    }
}
