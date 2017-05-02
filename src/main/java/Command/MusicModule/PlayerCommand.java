/*
 * 
 * AIBot, a Discord bot made by AlienIdeology
 * 
 * 
 * 2017 (c) AIBot
 */
package Command.MusicModule;

import AISystem.Selector.EmojiSelection;
import Audio.Music;
import Command.Command;
import Constants.Emoji;
import Listener.SelectorListener;
import Main.Main;
import Setting.Prefix;
import Utility.UtilBot;
import Utility.UtilString;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import java.util.Arrays;
import java.util.List;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

/**
 *
 * @author Alien Ideology <alien.ideology at alien.org>
 */
public class PlayerCommand extends Command {

    public final static String HELP = "This command is for contolling the music player.\n"
                                    + "Command Usage: `"+ Prefix.getDefaultPrefix() +"player` or `"+ Prefix.getDefaultPrefix() +"pl`\n"
                                    + "Parameter: `-h | null`";
    
    private final List<String> reactions = Arrays.asList(Emoji.PLAYER, Emoji.NEXT_TRACK, Emoji.SHUFFLE, Emoji.REPEAT);

    @Override
    public EmbedBuilder help(MessageReceivedEvent e) {
        EmbedBuilder embed = super.help(e);
        embed.setTitle("Music Module", null);
        embed.addField("Player -Help", HELP, true);
        return embed;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent e) {
        if(args.length == 1 && "-h".equals(args[0])) {
            e.getChannel().sendMessage(help(e).build()).queue();
            return;
        }
        
        if(args.length == 0)
        {
            if(!Main.getGuild(e.getGuild()).getScheduler().getNowPlayingTrack().isEmpty())
            {
                AudioPlayer player = Main.getGuild(e.getGuild()).getPlayer();
                AudioTrack track = player.getPlayingTrack();
                String state = player.isPaused() ? "Paused" : UtilString.VariableToString("_", player.getPlayingTrack().getState().toString());
                String playing = "**["+track.getInfo().title+"]("+track.getInfo().uri+")**\n";

                String progress = Music.positionToString(e) + Emoji.STOP;
                String volume = Music.volumeToString(e);
                String posdur = "[`"+UtilString.formatDurationToString(track.getPosition())
                        +"`/`"+UtilString.formatDurationToString(track.getDuration())+"`]";
                String skips = Emoji.NEXT_TRACK+" "+Main.getGuild(e.getGuild()).getScheduler().getVote().size()+
                        "/"+Main.getGuild(e.getGuild()).getScheduler().getRequiredVote();
                
                EmbedBuilder embedplayer = new EmbedBuilder();
                embedplayer.addField(state+"...", playing+"  "+posdur+"\n"+progress+"\n"+volume+"  "+skips, true);
                embedplayer.setColor(UtilBot.randomColor());
                e.getChannel().sendMessage(embedplayer.build()).queue((Message msg) -> {
                    SelectorListener.addEmojiSelection(e.getAuthor().getId(), 
                            new EmojiSelection(msg, e.getMember(), reactions) {
                        @Override
                        public void action(int chose) {
                            switch(chose) {
                                case 0:
                                    PauseCommand pc = new PauseCommand();
                                    pc.action(args, e);
                                    break;
                                case 1:
                                    SkipCommand sc = new SkipCommand();
                                    sc.action(args, e);
                                    break;
                                case 2:
                                    ShuffleCommand shc = new ShuffleCommand();
                                    shc.action(args, e);
                                    break;
                                case 3:
                                    RepeatCommand rc = new RepeatCommand();
                                    rc.action(args, e);
                                    break;
                                default:
                                    break;
                            }
                        }
                    });
                });
            }
            else
                e.getChannel().sendMessage(Emoji.ERROR + " No song is playing!").queue();
        }
    }
    
}
