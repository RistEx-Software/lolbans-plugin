package com.ristexsoftware.lolbans.api.runnables;

import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import com.ristexsoftware.lolbans.api.LolBans;

public class CacheRunnable extends TimerTask {
    @Override
    public void run() {
        LolBans self = LolBans.getPlugin();
        FutureTask<Boolean> t = new FutureTask<>(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                self.getUserCache().getObjectExpiryTask().run();
                self.getPunishmentCache().getObjectExpiryTask().run();
                return true;
            }
        });
        if (!LolBans.getPlugin().getPool().isShutdown())
            LolBans.getPlugin().getPool().execute(t);
    }

}