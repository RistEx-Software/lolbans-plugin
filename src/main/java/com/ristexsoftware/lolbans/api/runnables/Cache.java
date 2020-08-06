package com.ristexsoftware.lolbans.api.runnables;

import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.configuration.Messages;
import com.ristexsoftware.lolbans.common.utils.CacheUtil;

public class Cache extends TimerTask {
    @Override
    public void run() {
        FutureTask<Boolean> t = new FutureTask<>(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                int i = 0;
                CacheUtil.getUserExpiryRunnable().run();
                CacheUtil.getPunishExpiryRunnable().run();
                return true;
            }
        });
        if (!LolBans.pool.isShutdown())
            LolBans.pool.execute(t);
    }

}