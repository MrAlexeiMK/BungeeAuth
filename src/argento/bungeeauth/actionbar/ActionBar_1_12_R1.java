package argento.bungeeauth.actionbar;

import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import net.minecraft.server.v1_12_R1.ChatMessageType;
import net.minecraft.server.v1_12_R1.IChatBaseComponent;
import net.minecraft.server.v1_12_R1.IChatBaseComponent.ChatSerializer;
import net.minecraft.server.v1_12_R1.PacketPlayOutChat;

public class ActionBar_1_12_R1 implements ActionBar {

    @Override
    public void sendActionbar(Player p, String ch1, String ch2) {
        IChatBaseComponent comp = ChatSerializer.a("{\"text\":\""+ch1+" \",\"extra\":[{\"text\":\""+ch2+"\",\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"§fReset password\"},\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/changepassword\"}}]}");
        PacketPlayOutChat packet = new PacketPlayOutChat(comp, ChatMessageType.a((byte) 100));
        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
    }
}
