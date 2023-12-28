package drblix.chatprefixes;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class EventListeners implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    private void onPlayerChat(AsyncChatEvent event) {
        // check if player possesses a tag in the config file
        // if not, return early and do nothing
        final ChatPrefixes.PrefixInfo info = ChatPrefixes.getInfoForUuid(event.getPlayer().getUniqueId());
        if (info == null) return;

        // cancels the event so the player's actual message isn't sent, the modified one is
        event.setCancelled(true);

        // gets the player's message
        final String serializedMsg = LegacyComponentSerializer.legacy(LegacyComponentSerializer.SECTION_CHAR).serialize(event.message());

        final String tag = String.format("%s%s%s", ChatPrefixes.getFrontBorder(), info.getTagName(), ChatPrefixes.getEndBorder());

        // creates a new text component with the player's tag at the front
        final TextComponent textComponent = Component.text(tag).color(TextColor.fromHexString(info.getColour()))
                .append(Component.text(" <" + event.getPlayer().getName() +"> " + serializedMsg, NamedTextColor.WHITE));

        for (Audience viewer : event.viewers()) {
            // send message w/ tag to all viewers
            viewer.sendMessage(textComponent);
        }
    }
}
