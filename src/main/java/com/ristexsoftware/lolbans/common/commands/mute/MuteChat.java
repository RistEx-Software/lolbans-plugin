package com.ristexsoftware.lolbans.common.commands.mute;

import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.User;
import com.ristexsoftware.lolbans.api.command.AsyncCommand;
import com.ristexsoftware.lolbans.api.configuration.Messages;

public class MuteChat extends AsyncCommand {

    public MuteChat(String name, LolBans plugin) {
        super("mutechat", plugin);
        this.setDescription("Mute the chat for all players (toggleable)");
        this.setPermission("lolbans.mutechat");
        this.setAliases(Arrays.asList(new String[] { "mute-chat" }));
        setSyntax(Messages.getMessages().getConfig().getString("syntax.chat-mute"));
    }

    @Override
    public void onSyntaxError(User sender, String label, String[] args) {

    }

    @Override
    public List<String> onTabComplete(User sender, String[] args) {
        return null;
    }

    // TODO: Add timer ability
    @Override
    public boolean run(User sender, String commandLabel, String[] args) {
        if (!sender.hasPermission("lolbans.mutechat"))
        sender.permissionDenied("lolbans.mutechat");
        try {
            LolBans.getPlugin().setChatMute(!LolBans.getPlugin().isChatMute());

            //TODO: maybe add more info in here?
            if (LolBans.getPlugin().isChatMute()) 
                LolBans.getPlugin().broadcastMessage(Messages.translate("mute.global-muted", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
            else
                LolBans.getPlugin().broadcastMessage(Messages.translate("mute.global-unmuted", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
        } catch (Exception e) {
            e.printStackTrace();
            sender.sendMessage(Messages.serverError);
        }
        return false;
    }
    
}