package com.alibaba.cobar;

import com.alibaba.cobar.config.model.SystemConfig;
import com.alibaba.cobar.parser.ast.expression.primary.function.datetime.Sysdate;
import org.apache.log4j.helpers.LogLog;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by hlr@superid.cn on 2014/10/11.
 */
public class StartupTest {
    private static final String dateFormat = "yyyy-MM-dd HH:mm:ss";

    @Test
    public void testServer(){
        try {
            // init
            CobarServer server = CobarServer.getInstance();
            server.serverPostInit();
            server.beforeStart(dateFormat);

            // startup
            server.startup();
            System.in.read();
        } catch (Throwable e) {
            SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
            LogLog.error(sdf.format(new Date()) + " startup error", e);
            System.exit(-1);
        }
    }
}
