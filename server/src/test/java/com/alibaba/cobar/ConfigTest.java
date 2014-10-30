package com.alibaba.cobar;

import com.alibaba.cobar.config.loader.xml.ServersXMLSchemaLoader;
import org.junit.Test;

/**
 * Created by hlr@superid.cn on 2014/9/9.
 */
public class ConfigTest {
    private static final String SCHEMA_XML_PATH = "/myschema.xml";
    private static final String RULE_XML_PATH = "/myrule.xml";
    private static final String SERVERS_XML_PATH = "/servers.xml";

    @Test
    public void testRuleXmlLoader(){
        ServersXMLSchemaLoader schemaLoader = new ServersXMLSchemaLoader(SCHEMA_XML_PATH, RULE_XML_PATH, SERVERS_XML_PATH);
    }
}
