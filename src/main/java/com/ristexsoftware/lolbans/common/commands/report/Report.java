package com.ristexsoftware.lolbans.common.commands.report;

import java.util.List;  

import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.User;
import com.ristexsoftware.lolbans.api.command.AsyncCommand;

public class Report extends AsyncCommand {

    public Report(String name, LolBans plugin) {
        super(name, plugin);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void onSyntaxError(User sender, String label, String[] args) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<String> onTabComplete(User sender, String[] args) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean run(User sender, String commandLabel, String[] args) throws Throwable {
        // TODO Auto-generated method stub
        return false;
    }

}