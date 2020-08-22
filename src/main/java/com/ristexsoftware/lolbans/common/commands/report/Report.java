package com.ristexsoftware.lolbans.common.commands.report;

import java.util.List;  

import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.User;
import com.ristexsoftware.lolbans.api.command.AsyncCommand;

public class Report extends AsyncCommand {

    public Report(LolBans plugin) {
        super("report", plugin);
    }

    @Override
    public void onSyntaxError(User sender, String label, String[] args) {

    }

    @Override
    public List<String> onTabComplete(User sender, String[] args) {
        return null;
    }

    @Override
    public boolean run(User sender, String commandLabel, String[] args) throws Throwable {
        return false;
    }
}