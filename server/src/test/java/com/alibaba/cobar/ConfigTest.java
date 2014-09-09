package com.alibaba.cobar;

import com.alibaba.cobar.config.loader.xml.MyXMLRuleLoader;
import com.alibaba.cobar.config.loader.xml.XMLRuleLoader;
import org.junit.Test;

/**
 * Created by hlr@superid.cn on 2014/9/9.
 */
public class ConfigTest {
    private static final String RULE_XML_PATH = "/myrule.xml";
    private static final String SERVERS_XML_PATH = "/servers.xml";

    @Test
    public void testRuleXmlLoader(){
        MyXMLRuleLoader loader = new MyXMLRuleLoader(RULE_XML_PATH, SERVERS_XML_PATH);
    }
}
