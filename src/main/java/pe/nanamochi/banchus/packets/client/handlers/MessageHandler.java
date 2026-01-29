package pe.nanamochi.banchus.packets.client.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.comands.CommandProcessor;
import pe.nanamochi.banchus.entities.PacketBundle;
import pe.nanamochi.banchus.entities.db.Channel;
import pe.nanamochi.banchus.entities.db.Session;
import pe.nanamochi.banchus.packets.AbstractPacketHandler;
import pe.nanamochi.banchus.packets.PacketWriter;
import pe.nanamochi.banchus.packets.Packets;
import pe.nanamochi.banchus.packets.client.MessagePacket;
import pe.nanamochi.banchus.services.ChannelMembersRedisService;
import pe.nanamochi.banchus.services.ChannelService;
import pe.nanamochi.banchus.services.PacketBundleService;
import pe.nanamochi.banchus.services.SessionService;

@Component
public class MessageHandler extends AbstractPacketHandler<MessagePacket> {

  private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

  @Value("${banchus.command-prefix}")
  private String commandPrefix;

  @Autowired private PacketWriter packetWriter;
  @Autowired private PacketBundleService packetBundleService;
  @Autowired private ChannelService channelService;
  @Autowired private ChannelMembersRedisService channelMembersService;
    @Autowired private SessionService sessionService;
  @Autowired private CommandProcessor commandProcessor;

  @Override
  public Packets getPacketType() {
    return Packets.OSU_MESSAGE;
  }

  @Override
  public Class<MessagePacket> getPacketClass() {
    return MessagePacket.class;
  }

  @Override
  public void handle(MessagePacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    logger.debug("Handling packet: {}", getPacketType());
    // TODO: Validate silence

    String channelName = null;
    if (packet.getTarget().equals("#multiplayer")) {
      // TODO: Handle multiplayer chat
    } else if (packet.getTarget().equals("#spectator")) {
      // TODO: Handle spectator chat
    } else {
      channelName = packet.getTarget();
    }

    Channel channel = channelService.findByName(channelName);
    if (channel == null) {
      logger.warn(
          "User {} attempted to send a message to non-existent channel {}.",
          session.getUser().getUsername(),
          channelName);
      return;
    }

    if (!channelService.canWriteChannel(channel, session.getUser().getPrivileges())) {
      logger.warn(
          "User {} attempted to send a message to channel {} without sufficient privileges.",
          session.getUser().getUsername(),
          channelName);
      return;
    }

    if (packet.getContent().length() > 2000) {
      packet.setContent(packet.getContent().substring(0, 2000) + "...");
    }

    // If the user is restricted, they cannot send messages
    if (session.getUser().isRestricted()) return;

    // Send message to everyone else
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    packetWriter.writePacket(
        stream,
        new pe.nanamochi.banchus.packets.server.MessagePacket(
            session.getUser().getUsername(),
            packet.getContent(),
            packet.getTarget(),
            session.getUser().getId()));

    Set<UUID> targetSessions = new HashSet<>();

    if (!packet.getContent().startsWith("!help")) {
      targetSessions = channelMembersService.getMembers(channel.getId());
    }

    for (UUID targetSessionId : targetSessions) {
      if (targetSessionId.equals(session.getId())) continue; // Already sent to self
      packetBundleService.enqueue(targetSessionId, new PacketBundle(stream.toByteArray()));
    }

    // Handle np
      handleNp(session, packet);

    // Handle commands
      handleCommands(session, packet, targetSessions, channel);
  }

  private void handleNp(Session session, MessagePacket packet) {
      if (!packet.getContent().startsWith("\u0001ACTION")) return;

      Pattern pattern = Pattern.compile(
              "\u0001ACTION is (playing|editing|watching|listening to) " +
                      "\\[(?<beatmapUrl>[^ ]+) (?<beatmapText>.+)\\]\u0001"
      );

      Matcher matcher = pattern.matcher(packet.getContent());

      if (!matcher.matches()) return;

      String beatmapUrl = matcher.group("beatmapUrl");

      Integer lastNpBeatmapId = null;
      try {
          URI uri = new URI(beatmapUrl);

          String fragment = uri.getFragment();
          if (fragment != null && fragment.startsWith("/")) {
              fragment = fragment.substring(1);
          }

          if (fragment != null && !fragment.isEmpty()) {
              lastNpBeatmapId = Integer.parseInt(fragment);
          }
      } catch (URISyntaxException | NumberFormatException ignored) {
          lastNpBeatmapId = null;
      }

      if (lastNpBeatmapId != null) {
          session.setLastNpBeatmapId(lastNpBeatmapId);
          sessionService.updateSession(session);
      }
  }

  private void handleCommands(Session session, MessagePacket packet, Set<UUID> targetSessions, Channel channel)
          throws IOException {
      String result = commandProcessor.handle(packet.getContent(), session.getUser().getPrivileges());

      if (result == null || result.trim().isEmpty()) return;

      if (packet.getContent().startsWith("!help")) {
          targetSessions = Set.of(session.getId());
      } else {
          targetSessions = channelMembersService.getMembers(channel.getId());
      }

      for (UUID targetSessionId : targetSessions) {
          ByteArrayOutputStream commandStream = new ByteArrayOutputStream();
          packetWriter.writePacket(
                  commandStream,
                  new pe.nanamochi.banchus.packets.server.MessagePacket(
                          "BanchoBot", result, packet.getTarget(), 0));
          packetBundleService.enqueue(targetSessionId, new PacketBundle(commandStream.toByteArray()));
      }
  }
}
