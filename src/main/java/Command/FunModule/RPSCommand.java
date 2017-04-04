/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Command.FunModule;

import Command.Command;
import Config.Emoji;
import Config.Info;
import Config.Prefix;
import java.awt.Color;
import java.time.Instant;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

/**
 *
 * @author Alien Ideology <alien.ideology at alien.org>
 */
public class RPSCommand implements Command {
    
    public final static String HELP = "Play Rock Paper Scissors with the bot!\n"
                                    + "Command Usage: `" + Prefix.getDefaultPrefix() + "rockpaperscissors` or `" + Prefix.getDefaultPrefix() + "rps`\n"
                                    + "Parameter: `-h | rock | paper | scissors | null`\n";
    
    private String emoji2 = "";

    @Override
    public boolean called(String[] args, MessageReceivedEvent e) {
        return true;
    }

    @Override
    public void help(MessageReceivedEvent e) {
        embed.setColor(Color.red);
        embed.setTitle("Miscellaneous Module", null);
        embed.addField("Rock Paper Scissors -Help", HELP, true);
        embed.setFooter("Command Help/Usage", Info.I_help);
        embed.setTimestamp(Instant.now());

        MessageEmbed me = embed.build();
        e.getChannel().sendMessage(me).queue();
        embed.clearFields();
    }

    @Override
    public void action(String[] args, MessageReceivedEvent e) {
        if(args.length == 0 || "-h".equals(args[0])) 
        {
            help(e);
        }
        
        else if(args.length > 0)
        {
            String hand = "", emoji = "";
            String hand2 = getHand();
            if("rock".equals(args[0]) || "rocks".equals(args[0]) || "r".equals(args[0]) || "stone".equals(args[0]))
            {
                emoji = Emoji.rock;
                hand = "rock";
            }
            else if("paper".equals(args[0]) || "papers".equals(args[0]) || "p".equals(args[0]))
            {
                emoji = Emoji.paper;
                hand = "paper";
            }
            else if("scissor".equals(args[0]) || "scissors".equals(args[0]) || "s".equals(args[0]))
            {
                emoji = Emoji.scissors;
                hand = "scissors";
            }
            else
            {
                e.getChannel().sendMessage(Emoji.error + " Please enter a valid choice.").queue();
                return;
            }
            
            String output = compare(hand, hand2);
            
            e.getChannel().sendMessage(output + "\n You: " + emoji + " Me: " + emoji2).queue();
        }
    }

    @Override
    public void executed(boolean success, MessageReceivedEvent e) {
        
    }
    
    public String getHand()
    {
        String hand = "";
        int choice = (int) Math.floor(Math.random() * 4);
        switch(choice)
        {
            case 1: hand = "rock";
            emoji2 = Emoji.rock;
            break;
            case 2: hand = "paper";
            emoji2 = Emoji.paper;
            break;
            case 3: hand = "scissors";
            emoji2 = Emoji.scissors;
            break;
            default: hand = "no hand";
            break;
        }
        return hand;
    }
    
    public String compare(String hand, String hand2)
    {
        String result = "";
        if(hand.equals(hand2))
            result = Emoji.tie + " It's a tie!";
        else if(hand.equals("rock"))
        {
            if(hand2.equals("paper"))
                result = "I won!";
            if(hand2.equals("scissors"))
                result = "You won!";
        }
        else if(hand.equals("paper"))
        {
            if(hand2.equals("scissor"))
                result = "I won!";
            if(hand2.equals("rock"))
                result = "You won!";
        }
        else if(hand.equals("scissors"))
        {
            if(hand2.equals("rock"))
                result = "I won!";
            if(hand2.equals("paper"))
                result = "You won!";
        }
        
        return result;
    }
}