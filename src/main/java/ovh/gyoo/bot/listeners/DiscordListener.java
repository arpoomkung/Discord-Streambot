package ovh.gyoo.bot.listeners;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.MessageBuilder;
import net.dv8tion.jda.entities.*;
import net.dv8tion.jda.events.guild.GuildJoinEvent;
import net.dv8tion.jda.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import net.dv8tion.jda.utils.InviteUtil;
import ovh.gyoo.bot.commands.*;
import ovh.gyoo.bot.data.DiscordInstance;
import ovh.gyoo.bot.data.MessageItem;
import ovh.gyoo.bot.handlers.TwitchChecker;
import ovh.gyoo.bot.data.LocalServer;
import ovh.gyoo.bot.data.ServerList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiscordListener extends ListenerAdapter {
    JDA api;
    List<String> options = new ArrayList<>();
    Map<String, Command> commandMap = new HashMap<>();

    public DiscordListener(JDA api){
        this.api = api;

        commandMap.put(CStreams.name, new CStreams());
        commandMap.put(CAdd.name, new CAdd());
        commandMap.put(CRemove.name, new CRemove());
        commandMap.put(CPermissions.name, new CPermissions());
        commandMap.put(CList.name, new CList());
        commandMap.put(CEnable.name, new CEnable());
        commandMap.put(CDisable.name, new CDisable());
        commandMap.put(CInvite.name, new CInvite());
        commandMap.put(CMove.name, new CMove());
        commandMap.put(CQueue.name, new CQueue());
        commandMap.put(CDonate.name, new CDonate());
        commandMap.put(CServers.name, new CServers());
        commandMap.put(CAnnounce.name, new CAnnounce());

        options.add("`game` : Game name based on Twitch's list (must be the exact name to work !)");
        options.add("`channel` : Twitch channel name");
        options.add("`tag` : Word or group of words that must be present in the stream's title");
        options.add("`manager` : Discord user (must use the @ alias when using this option !)");
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent e){
        if (e.getChannel().getId().equals("131483070464393216") && e.getMessage().getContent().startsWith("!invite")){
            invite(e);
        }
        if (e.getMessage().getContent().startsWith("!streambot")){
            commands(e, e.getMessage().getContent().substring(11));
        }
    }

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent e){
        if (e.getMessage().getContent().equals("!streambot servers")){
            commandMap.get("servers").execute(new MessageReceivedEvent(api, e.getResponseNumber(), e.getMessage()), "");
        }
        if (e.getMessage().getContent().startsWith("!streambot announce")){
            commandMap.get("announce").execute(new MessageReceivedEvent(api, e.getResponseNumber(), e.getMessage()), e.getMessage().getContent().substring(20));
        }
    }

    @Override
    public void onGuildJoin(GuildJoinEvent e){
        DiscordInstance.getInstance().addToQueue(new MessageItem(ServerList.getInstance().getServer(e.getGuild().getId()).getId(), MessageItem.Type.GUILD, new MessageBuilder()
                    .appendString("Hello ! I'm StreamBot ! Type `!streambot commands` to see the available commands !")
                    .build()));
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent e){
        ServerList.getInstance().removeServer(e.getGuild().getId());
    }

    private void invite(GuildMessageReceivedEvent e){
        String[] strings = e.getMessage().getContent().split(" ");
        InviteUtil.Invite i = InviteUtil.resolve(strings[1]);
        InviteUtil.join(i, api);
        if(null == ServerList.getInstance().getServer(i.getGuildId())){
            LocalServer ls = new LocalServer(i.getChannelId(), i.getGuildId());
            ls.addManager(e.getAuthor().getId());
            ServerList.getInstance().addServer(i.getGuildId(), ls);
            DiscordInstance.getInstance().addToQueue(new MessageItem(e.getChannel().getId(), MessageItem.Type.GUILD, new MessageBuilder()
                    .appendString("Added Streambot to server " + i.getGuildName() + " in channel #" + i.getChannelName() + " !")
                    .build()));
            Role userRole = e.getGuild().getRoles().stream().filter(role -> role.getName().equals("User")).findFirst().get();
            e.getGuild().getManager().addRoleToUser(e.getAuthor(), userRole);
        }
        else e.getChannel().sendMessage(new MessageBuilder()
                .appendString("Error : Streambot already settled on channel #" + api.getTextChannelById(ServerList.getInstance().getServer(i.getGuildId()).getId()).getName() + " for this server")
                .build());
    }

    private void commands(GuildMessageReceivedEvent e, String command){
        String[] split = command.split(" ");
        String content = command.substring(command.indexOf(" ") + 1);
        if(split[0].equals("commands")){
            MessageBuilder builder = new MessageBuilder();
            builder.appendString("`!streambot <command>`\n");
            builder.appendString("`commands` : List of available commands\n");
            commandMap.entrySet().stream().filter(c -> c.getValue().isAllowed(e.getGuild().getId(), e.getAuthor().getId())).forEach(c -> builder.appendString(c.getValue().getDescription() + "\n"));
            builder.appendString("\n*Options* :\n");
            for (String option : options){
                builder.appendString(option + "\n");
            }
            DiscordInstance.getInstance().addToQueue(new MessageItem(e.getChannel().getId(), MessageItem.Type.GUILD, builder.build()));
            return;
        }
        if(commandMap.containsKey(split[0]))
            commandMap.get(split[0]).execute(new MessageReceivedEvent(api, e.getResponseNumber(), e.getMessage()), content);
        else
            DiscordInstance.getInstance().addToQueue(new MessageItem(e.getChannel().getId(), MessageItem.Type.GUILD, new MessageBuilder()
                    .appendString("Unknown command")
                    .build()));
    }

}