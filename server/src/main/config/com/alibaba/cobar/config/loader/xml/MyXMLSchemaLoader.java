/*
 * Copyright 1999-2012 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * (created at 2012-6-14)
 */
package com.alibaba.cobar.config.loader.xml;

import com.alibaba.cobar.config.loader.SchemaLoader;
import com.alibaba.cobar.config.model.DataNodeConfig;
import com.alibaba.cobar.config.model.DataSourceConfig;
import com.alibaba.cobar.config.model.SchemaConfig;
import com.alibaba.cobar.config.model.TableConfig;
import com.alibaba.cobar.config.model.rule.RuleAlgorithm;
import com.alibaba.cobar.config.model.rule.RuleConfig;
import com.alibaba.cobar.config.model.rule.TableRuleConfig;
import com.alibaba.cobar.config.util.ConfigException;
import com.alibaba.cobar.config.util.ConfigUtil;
import com.alibaba.cobar.util.SplitUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @author <a href="mailto:shuo.qius@alibaba-inc.com">QIU Shuo</a>
 */
@SuppressWarnings("unchecked")
public class MyXMLSchemaLoader implements SchemaLoader {
    private final static String DEFAULT_SCHEMA_DTD = "/myschema.dtd";
    private final static String DEFAULT_SCHEMA_XML = "/myschema.xml";
    private final static String DEFAULT_SERVERS_DTD = "/servers.dtd";
    private final static String DEFAULT_SERVERS_XML = "/servers.xml";
    private final static String DEFAULT_DS_TYPE = "mysql";
    private final static String DEFAULT_SQL_MODE = "STRICT_TRANS_TABLES";
    private final static String DEFAULT_HEARTBEAT_SQL = "SELECT 1";
    private final static String DEFAULT_SCHEMA_DATA_NODE = "";
    private final static String DEFAULT_SCHEMA_GROUP= "default";


    private final Map<String, TableRuleConfig> tableRules;
    private final Set<RuleConfig> rules;
    private final Map<String, RuleAlgorithm> functions;
    private final Map<String, DataSourceConfig> dataSources;
    private final Map<String, DataNodeConfig> dataNodes;
    private final Map<String, SchemaConfig> schemas;

    private final Map<String, List<DataSourceConfig>> groupDsMap; // groupName => dataSourceConfigList
    private final Map<String, List<DataNodeConfig>> tableNameDataNodeMap; // tableName => dataNodeConfigList
    private final Map<String, Map<Integer, Integer>> tableIndexMap; //从tableName => (id, dataNodeIndex) => dataNodeIndex

    public MyXMLSchemaLoader(String schemaFile, String ruleFile, String serversFile) {
        MyXMLRuleLoader ruleLoader = new MyXMLRuleLoader(ruleFile, serversFile);
        this.rules = ruleLoader.listRuleConfig();
        this.tableRules = ruleLoader.getTableRules();
        this.functions = ruleLoader.getFunctions();
        this.dataSources = new HashMap<String, DataSourceConfig>();
        this.dataNodes = new HashMap<String, DataNodeConfig>();
        this.schemas = new HashMap<String, SchemaConfig>();
        this.groupDsMap = new HashMap<String, List<DataSourceConfig>>();
        this.tableNameDataNodeMap = new HashMap<String, List<DataNodeConfig>>();
        this.tableIndexMap = new HashMap<String, Map<Integer, Integer>>();
        this.load(DEFAULT_SCHEMA_DTD, schemaFile == null ? DEFAULT_SCHEMA_XML : schemaFile,
                  DEFAULT_SERVERS_DTD, serversFile == null ? DEFAULT_SERVERS_XML : serversFile);
    }

    public MyXMLSchemaLoader() {
        this(null, null, null);
    }

    @Override
    public Map<String, TableRuleConfig> getTableRules() {
        return tableRules;
    }

    @Override
    public Map<String, RuleAlgorithm> getFunctions() {
        return functions;
    }

    @Override
    public Map<String, DataSourceConfig> getDataSources() {
        return (Map<String, DataSourceConfig>) (dataSources.isEmpty() ? Collections.emptyMap() : dataSources);
    }

    @Override
    public Map<String, DataNodeConfig> getDataNodes() {
        return (Map<String, DataNodeConfig>) (dataNodes.isEmpty() ? Collections.emptyMap() : dataNodes);
    }

    @Override
    public Map<String, SchemaConfig> getSchemas() {
        return (Map<String, SchemaConfig>) (schemas.isEmpty() ? Collections.emptyMap() : schemas);
    }

    @Override
    public Set<RuleConfig> listRuleConfig() {
        return rules;
    }

    @Override
    public Map<String, Map<Integer, Integer>> getTableIndex() {
        return tableIndexMap;
    }

    private void load(String schemaDtdFile, String schemaXmlFile,
                      String serversDtdFile, String serversXmlFile) {
        InputStream schemaDtd = null;
        InputStream schemaXml = null;
        InputStream serversDtd = null;
        InputStream serversXml = null;
        try {
            schemaDtd = XMLSchemaLoader.class.getResourceAsStream(schemaDtdFile);
            schemaXml = XMLSchemaLoader.class.getResourceAsStream(schemaXmlFile);
            serversDtd = XMLSchemaLoader.class.getResourceAsStream(serversDtdFile);
            serversXml = XMLSchemaLoader.class.getResourceAsStream(serversXmlFile);
            Element schemaRoot = ConfigUtil.getDocument(schemaDtd, schemaXml).getDocumentElement();
            Element serversRoot = ConfigUtil.getDocument(serversDtd, serversXml).getDocumentElement();
            loadDataSources(serversRoot);
            loadDataNodes(serversRoot);
            loadSchemas(schemaRoot);
        } catch (ConfigException e) {
            throw e;
        } catch (Throwable e) {
            throw new ConfigException(e);
        } finally {
            if (schemaDtd != null) {
                try {
                    schemaDtd.close();
                } catch (IOException e) {
                }
            }
            if (schemaXml != null) {
                try {
                    schemaXml.close();
                } catch (IOException e) {
                }
            }
            if (serversDtd != null) {
                try {
                    serversDtd.close();
                } catch (IOException e) {
                }
            }
            if (serversXml != null) {
                try {
                    serversXml.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private void loadSchemas(Element schemaRoot) {
        String schemaName = ConfigUtil.findFirstElementByTag(schemaRoot, "schema").getAttribute("name");
        String dataNode = DEFAULT_SCHEMA_DATA_NODE;
        String group = DEFAULT_SCHEMA_GROUP;
        Map<String, TableConfig> tables = loadTables();
        boolean keepSqlSchema = false;
        schemas.put(schemaName, new SchemaConfig(schemaName, dataNode, group, keepSqlSchema, tables));
    }

    private String generateTableDataNodeStr(List<DataNodeConfig> configs, String tableName){
        if (configs.size() == 1){
            return tableName + "_dn[0]";
        }else{
            return tableName + "_dn$0-" + (configs.size() - 1);
        }
    }

    private Map<String, TableConfig> loadTables(){
        Map<String, TableConfig> tables = new HashMap<String, TableConfig>();
        for (Map.Entry<String, List<DataNodeConfig>> entry : tableNameDataNodeMap.entrySet()){
            String tableName = entry.getKey();
            String name = tableName.toUpperCase();
            String dataNode = generateTableDataNodeStr(entry.getValue(), tableName);
            TableRuleConfig tableRule = tableRules.get(tableName + "_rule");
            boolean ruleRequired = false;
            TableConfig table = new TableConfig(name, dataNode, tableRule, ruleRequired);
            tables.put(table.getName(), table);
        }
        return tables;
    }

    private Map<String, TableConfig> loadTables(Element node) {
        Map<String, TableConfig> tables = new HashMap<String, TableConfig>();
        NodeList nodeList = node.getElementsByTagName("table");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element tableElement = (Element) nodeList.item(i);
            String name = tableElement.getAttribute("name").toUpperCase();
            String dataNode = tableElement.getAttribute("dataNode");
            TableRuleConfig tableRule = null;
            if (tableElement.hasAttribute("rule")) {
                String ruleName = tableElement.getAttribute("rule");
                tableRule = tableRules.get(ruleName);
                if (tableRule == null) {
                    throw new ConfigException("rule " + ruleName + " is not found!");
                }
            }
            boolean ruleRequired = false;
            if (tableElement.hasAttribute("ruleRequired")) {
                ruleRequired = Boolean.parseBoolean(tableElement.getAttribute("ruleRequired"));
            }

            String[] tableNames = SplitUtil.split(name, ',', true);
            for (String tableName : tableNames) {
                TableConfig table = new TableConfig(tableName, dataNode, tableRule, ruleRequired);
                checkDataNodeExists(table.getDataNodes());
                if (tables.containsKey(table.getName())) {
                    throw new ConfigException("table " + tableName + " duplicated!");
                }
                tables.put(table.getName(), table);
            }
        }
        return tables;
    }

    private void checkDataNodeExists(String... nodes) {
        if (nodes == null || nodes.length < 1) {
            return;
        }
        for (String node : nodes) {
            if (!dataNodes.containsKey(node)) {
                throw new ConfigException("dataNode '" + node + "' is not found!");
            }
        }
    }

    private void loadDataNodes(Element root) {
        NodeList tableList = root.getElementsByTagName("table");
        List<DataNodeConfig> configList = new ArrayList<DataNodeConfig>();
        for (int i = 0; i < tableList.getLength(); i++){
            Element tableElement = (Element) tableList.item(i);
            String tableName = ConfigUtil.getFirstContentByTag(tableElement, "table_name");
            String dataNodeName = tableName + "_dn";
            String masterGroupName = ConfigUtil.getFirstContentByTag(tableElement, "master_group");
            tableElement.getElementsByTagName("brother_group");
            String brotherGroupName = ConfigUtil.getFirstContentByTag(tableElement, "brother_group");
            List<DataSourceConfig> masterDsConfigs = groupDsMap.get(masterGroupName);
            List<DataSourceConfig> brotherDsConfigs = null;
            List<DataNodeConfig> tableDataNodeList = new ArrayList<DataNodeConfig>();
            if (hasBrotherGroupConfig(tableElement)){
                brotherDsConfigs = groupDsMap.get(brotherGroupName);
            }

            if (brotherDsConfigs != null && masterDsConfigs.size() != brotherDsConfigs.size()){
                throw new ConfigException("masterGroup brotherGroup server number not equals!");
            }
            Map<Integer, Integer> tableIndex = new HashMap<Integer, Integer>();
            for (int k = 0; k < masterDsConfigs.size(); k++){
                DataNodeConfig nodeConfig = new DataNodeConfig();
                StringBuilder dsString = new StringBuilder();
                DataSourceConfig masterSource = masterDsConfigs.get(k);
                DataSourceConfig brotherSource = brotherDsConfigs.get(k);
                if (brotherDsConfigs == null){
                    dsString.append(masterSource.getName());
                }else{
                    dsString.append(masterSource.getName());
                    dsString.append(",");
                    dsString.append(brotherSource.getName());
                }
                nodeConfig.setName(dataNodeName + "[" + k + "]");
                nodeConfig.setDataSource(dsString.toString());
                nodeConfig.setHeartbeatSQL(DEFAULT_HEARTBEAT_SQL);
                configList.add(nodeConfig);
                tableDataNodeList.add(nodeConfig);
                tableIndex.put(masterSource.getId(), k);//brother和master具有一一对应关系，master的index就是brother的index
            }
            tableIndexMap.put(tableName, tableIndex);
            tableNameDataNodeMap.put(tableName, tableDataNodeList);
        }

        for (DataNodeConfig conf : configList) {
            if (dataNodes.containsKey(conf.getName())) {
                throw new ConfigException("dataNode " + conf.getName() + " duplicated!");
            }
            dataNodes.put(conf.getName(), conf);
        }
    }

    private void loadDataSources(Element root) {
        NodeList groupList = root.getElementsByTagName("group");
        List<DataSourceConfig> dscList = new ArrayList<DataSourceConfig>();
        Map<String, Integer> dsNameIndexMap = new HashMap<String, Integer>();

        for (int k = 0; k < groupList.getLength(); k++){
            Element groupEle = (Element) groupList.item(k);
            String groupName = ConfigUtil.getFirstContentByTag(groupEle, "name");
            //TODO 这里暂时忽略migrations
            NodeList serverList = groupEle.getElementsByTagName("server");
            if (serverList.getLength() == 0){
                throw new ConfigException("the size of servers in group can not be zero!");
            }
            String slashName = new String();
            List<DataSourceConfig> groupDsList = new ArrayList<DataSourceConfig>();
            try {
                for (int i = 0; i < serverList.getLength(); i++ ){
                    Element element = (Element) serverList.item(i);
                    int id = Integer.parseInt(ConfigUtil.getFirstContentByTag(element, "id"));
                    String host = ConfigUtil.getFirstContentByTag(element, "host");
                    int port = Integer.parseInt(ConfigUtil.getFirstContentByTag(element, "port"));
                    slashName = ConfigUtil.getFirstContentByTag(element, "db_name");
                    String user = ConfigUtil.getFirstContentByTag(element, "user");
                    String password = ConfigUtil.getFirstContentByTag(element, "password");

                    setDsNameIndex(dsNameIndexMap, slashName);
                    final int nameIndex = getDsNameIndex(dsNameIndexMap, slashName);
                    //TODO 后期会支持name$0-xxx这种写法，需要对相关属性字符串进行解析
                    DataSourceConfig config = new DataSourceConfig();
                    config.setName(slashName + "_ds" + "[" + nameIndex + "]");
                    config.setType(DEFAULT_DS_TYPE);
                    config.setHost(host);
                    config.setPort(port);
                    config.setUser(user);
                    config.setPassword(password);
                    config.setDatabase(slashName);
                    config.setSqlMode(DEFAULT_SQL_MODE);
                    config.setId(id);
                    dscList.add(config);
                    groupDsList.add(config);
                }
                if (groupDsList.size() != 0){
                    groupDsMap.put(groupName, groupDsList); // 确保不为0
                }
            }catch (Exception e){
                throw new ConfigException("dataSource " + slashName + " define error", e);
            }
        }

        for (DataSourceConfig dsConf : dscList) {
            if (dataSources.containsKey(dsConf.getName())) {
                throw new ConfigException("dataSource name " + dsConf.getName() + "duplicated!");
            }
            dataSources.put(dsConf.getName(), dsConf);
        }
    }

    private boolean hasBrotherGroupConfig(Element tableElement){
        return tableElement.getElementsByTagName("brother_group").getLength() == 0 ? false : true;
    }

    private int getDsNameIndex(Map<String, Integer> dsNameIndexMap, String name){
        if (dsNameIndexMap.containsKey(name)){
            return dsNameIndexMap.get(name);
        }
        return 0;
    }

    private void setDsNameIndex(Map<String, Integer> dsNameIndexMap, String name){
        if (dsNameIndexMap.containsKey(name)){
            dsNameIndexMap.put(name, dsNameIndexMap.get(name) + 1);
        }else{
            dsNameIndexMap.put(name, 0);
        }
    }
}
