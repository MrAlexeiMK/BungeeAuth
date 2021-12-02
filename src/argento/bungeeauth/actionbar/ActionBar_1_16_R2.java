package argento.bungeeauth.actionbar;

import net.minecraft.server.v1_16_R2.IChatBaseComponent;
import net.minecraft.server.v1_16_R2.IChatBaseComponent.ChatSerializer;
import net.minecraft.server.v1_16_R2.PacketPlayOutChat;
import net.minecraft.server.v1_16_R2.ChatMessageType;

import org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer;

import org.bukkit.entity.Player;

public class ActionBar_1_16_R2 implements ActionBar {

    @Override
    public void sendActionbar(Player p, String ch1, String ch2) {
        IChatBaseComponent comp = ChatSerializer.a("{\"text\":\""+ch1+" \",\"extra\":[{\"text\":\""+ch2+"\",\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"§fReset password\"},\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/changepassword\"}}]}");
        PacketPlayOutChat packet = new PacketPlayOutChat(comp, ChatMessageType.a((byte) 100), p.getUniqueId());
        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
    }
}