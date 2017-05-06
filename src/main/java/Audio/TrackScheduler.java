/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Audio;

import AISystem.AIVote;
import Audio.AudioTrackWrapper.TrackType;
import Constants.Emoji;
import Audio.PlayerMode;
import AISystem.AILogger;
import Utility.UtilNum;
import Utility.WebScraper;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import java.io.IOException;
import java.util.*;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

/**
 *
 * @author liaoyilin
 */

/**
 * This class schedules tracks for the audio PLAYER. It contains the queue of tracks.
 */
public class TrackScheduler extends AudioEventAdapter {

    private VoiceChannel vc;
    private TextChannel tc;

    /**
    * Track fields.
    */
    private final AudioPlayer player;
    private AudioTrackWrapper NowPlayingTrack;
    private final QueueList queue;
    private final QueueList preQueue;

    /**
    * Skip System fields.
    */
    private final AIVote skips;

    /**
    * FM fields.
    */
    private ArrayList<String> fmSongs = new ArrayList<>();
    private int auto = -1, previous = -1;

    /**
    * Enum type of the playing mode.
    */
    private PlayerMode Mode;

    /**
   * @param player The audio PLAYER this scheduler uses=
   */
    public TrackScheduler(AudioPlayer player) {
        this.player = player;
        this.Mode = Audio.PlayerMode.DEFAULT;

        this.queue = new QueueList();
        this.preQueue = new QueueList();
        this.NowPlayingTrack = new AudioTrackWrapper();

        this.skips = new AIVote() {
            @Override
            public int getRequiredVote() {
                return requiredVote();
            }
        };
  }

    /**
     * Check the mode and start the proper track.
     * If the PlayerMode is NORMAL, and there is no queue left, then
     */
    public void nextTrack() {
        clearVote();
        addToPreviousQueue(NowPlayingTrack);

        if(Mode == Audio.PlayerMode.FM) {
            autoFM();
        } else if(Mode == Audio.PlayerMode.REPEAT) {
            AudioTrackWrapper repeat = NowPlayingTrack.makeClone();
            queue.add(repeat);
            NowPlayingTrack = queue.peek();
            player.startTrack(queue.poll().getTrack(), false);
        } else if(Mode == Audio.PlayerMode.REPEAT_SINGLE) {
            NowPlayingTrack = NowPlayingTrack.makeClone();
            player.startTrack(NowPlayingTrack.getTrack(), false);
        } else if(Mode == Audio.PlayerMode.AUTO_PLAY) {
            if(!queue.isEmpty()) {
                NowPlayingTrack = queue.peek();
                player.startTrack(queue.poll().getTrack(), false);
            } else {
                try {
                    autoPlay();
                } catch (IOException e) {
                    tc.sendMessage(Emoji.ERROR + " Fail to load the next song.").queue();
                }
            }
        } else if(Mode == Audio.PlayerMode.NORMAL) {
            if(queue.isEmpty()) {
                stopPlayer();
            } else {
                player.startTrack(queue.peek().getTrack(), false);
                NowPlayingTrack = queue.poll();
            }
        }
        /*
        else if(queue.isEmpty()) {
            stopPlayer();
        }
         */
    }

    /**
     * Add the next track to queue or play right away if nothing is in the queue.
     * @param track The track to play or add to queue.
     * @param e
     */
    public void queue(AudioTrackWrapper track, MessageReceivedEvent e) {
        // Calling startTrack with the noInterrupt set to true will start the track only if nothing is currently playing. If
        // something is playing, it returns false and does nothing. In that case the PLAYER was already playing so this
        // track goes to the queue instead.

        if(this.Mode == Audio.PlayerMode.FM) {
            e.getChannel().sendMessage(Emoji.ERROR + " FM mode is ON! Only request radio or songs when FM is not playing.").queue();
            return;
        }

        if (!player.startTrack(track.getTrack(), true)) {
            queue.offer(track);
            e.getTextChannel().sendMessage(Emoji.SUCCESS + " Queued `" + track.getTrack().getInfo().title + "`").queue();
            return;
        }
        NowPlayingTrack = track;
    }

    /**
     * Add the play list to the queue
     * @param list
     * @param requester
     */
    public void addPlayList(AudioPlaylist list, String requester) {
        List<AudioTrack> tracklist = list.getTracks();

        for(AudioTrack track : tracklist) {
            AudioTrackWrapper wrapper = new AudioTrackWrapper(track, requester, TrackType.PLAYLIST);
            if (!player.startTrack(wrapper.getTrack(), true)) {
                queue.offer(wrapper);
                continue;
            }
            NowPlayingTrack = wrapper;
        }
    }

    /**
     * Automatically load a FM song from fmSongs.
     */
    public void autoFM() {
        Mode = PlayerMode.FM;

        while (auto == previous) {
            auto = UtilNum.randomNum(0, this.fmSongs.size()-1);
        }
        previous = auto;
        String url = this.fmSongs.get(auto);

        if (Music.urlPattern.matcher(url).find()) {
            Music.playerManager.loadItemOrdered(Music.playerManager, url, new LoadResultHandler(this) {
                @Override
                public void trackLoaded(AudioTrack track) {
                    NowPlayingTrack = new AudioTrackWrapper(track, "AIBot FM", AudioTrackWrapper.TrackType.FM);
                    player.startTrack(track, false);
                }
            });
        }
    }

    private void autoPlay() throws IOException {
        Mode = PlayerMode.AUTO_PLAY;

        String url = WebScraper.getYouTubeAutoPlay(NowPlayingTrack.getTrack().getInfo().uri);

        if (Music.urlPattern.matcher(url).find()) {
            Music.playerManager.loadItemOrdered(Music.playerManager, url, new LoadResultHandler(this) {
                @Override
                public void trackLoaded(AudioTrack track) {
                    NowPlayingTrack = new AudioTrackWrapper(track, "YouTube AutoPlay", TrackType.NORMAL_REQUEST);
                    player.startTrack(track, false);
                }
            });
        }
    }

    /**
     * Play the previous track and add the current one to queue.
     */
    public void playPrevious()
    {
        if(preQueue.isEmpty())
            return;

        queue.add(0, NowPlayingTrack.makeClone());
        NowPlayingTrack = preQueue.get(0).makeClone();
        player.startTrack(preQueue.get(0).getTrack(), false);
        preQueue.removeFirst();
    }

    /**
     * Add the finished song to previous queue
     * @param track
     */
    private void addToPreviousQueue(AudioTrackWrapper track)
    {
        preQueue.add(0, track.makeClone());
        if(preQueue.size() > 5)
            preQueue.removeLast();
    }

    public boolean addSkip(User vote) {
        return skips.addVote(vote);
    }

    public int requiredVote() {
        double mem = 0;
        //Only count non-Bot Users
        List<Member> members = vc.getMembers();
        for(Member m : members) {
            if(!m.getUser().isBot())
                mem++;
        }

        //Check if majority of the members agree to skip
        return (int) Math.ceil(mem / 2);
    }

    /**
     * Show now playing message
     * @param player
     * @param track
     */
    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        if(tc!=null)
            tc.sendMessage(Emoji.NOTES + " Now playing `" + track.getInfo().title + "`").queue();

        System.out.println("Track Started: " + track.getInfo().title);
    }

    /**
     * Determine the PLAYER mode and start the next track
     * @param player
     * @param track
     * @param endReason
     */
    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        // Only start the next track if the end reason is suitable for it (FINISHED or LOAD_FAILED)
        clearVote();

        if (endReason == AudioTrackEndReason.REPLACED)
            tc.sendMessage(Emoji.NEXT_TRACK + " Skipped the current song `" + track.getInfo().title + "`").queue();
        else if (endReason == AudioTrackEndReason.STOPPED)
            tc.sendMessage(Emoji.STOP + " Stopped the player.").queue();

        if (endReason.mayStartNext) {
            nextTrack();
        }
        System.out.println("Track Ended: " + track.getInfo().title + " By reason: " + endReason.toString());
    }

    /**
     * Inform the user that track has stuck
     * @param player
     * @param track
     * @param thresholdMs
     */
    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        tc.sendMessage(Emoji.ERROR + " Track stuck! Skipping to the next track...").queue();
        nextTrack();
    }

    /**
     * Inform the user that track has thrown an exception
     * @param player
     * @param track
     * @param exception
     */
    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        tc.sendMessage(Emoji.ERROR + " An error occurred! (Informed the owner)\n```\n\n"+exception.getLocalizedMessage()+"\n\n...```").queue();
        nextTrack();
        AILogger.errorLog(exception, this.getClass(), "TrackException(FriendlyException)", "Probably track decoding problem");
    }

    /**
     * Inform the user when the player is paused
     * @param player
     */
    @Override
    public void onPlayerPause(AudioPlayer player) {
        tc.sendMessage(Emoji.PAUSE + " Player paused.").queue();
    }

    /**
     * Inform the user when the player is resumed
     * @param player
     */
    @Override
    public void onPlayerResume(AudioPlayer player) {
        tc.sendMessage(Emoji.RESUME + " Player resumed.").queue();
    }

    /**
    * Clear methods
    * @return TrackScheduler, easier for chaining
    */
    public void stopPlayer() {
        clearNowPlayingTrack()
        .clearQueue()
        .clearVote()
        .clearMode()
        .player.stopTrack();
    }

    public TrackScheduler clearQueue() {
        queue.clear();
        fmSongs.clear();
        return this;
    }

    private TrackScheduler clearMode() {
        Mode = Audio.PlayerMode.DEFAULT;
        return this;
    }

    private TrackScheduler clearNowPlayingTrack() {
        NowPlayingTrack = new AudioTrackWrapper();
        return this;
    }

    public TrackScheduler clearVote() {
        skips.clear();
        return this;
    }

    /**
     * Getter and Setter
     */
    public TextChannel getTc() {
        return tc;
    }

    public void setTc(TextChannel tc) {
        this.tc = tc;
    }

    public VoiceChannel getVc() {
        return vc;
    }

    public void setVc(VoiceChannel vc) {
        this.vc = vc;
    }

    public PlayerMode getMode() {
        return Mode;
    }

    public void setMode(PlayerMode Mode) {
        this.Mode = Mode;
    }

    public QueueList getQueue() {
        return queue;
    }

    public QueueList getPreQueue() {
        return preQueue;
    }

    public ArrayList<String> getFmSongs() {
        return fmSongs;
    }

    public void setFmSongs(ArrayList<String> fmSongs) {
        this.fmSongs = fmSongs;
    }

    public void addFMSong(String song) {
        this.fmSongs.add(song);
    }

    public AudioTrackWrapper getNowPlayingTrack() {
        return NowPlayingTrack;
    }

    public List<User> getVote() {
        return skips.getVotes();
    }

}

