cobar-with-id-node-mapping
=====

目前的cobar暂时无法平滑扩容，而平滑扩容最主要的难点在于很难找到一种算法使得使得节点的metadata改变后，路由算法依旧保持一致性。这里参考mongodb的id-generator算法，根据time、process-id、random number、machine-id生成的一个id序列作为table的id，cobar根据id序列获取到对应的machine-id从而实施路由。这样一来我们便可以实施平滑扩容和迁移。

##Features
* 简化cobar的schema.xml和rule.xml配置文件，将其整合为简单的servers.xml配置文件
* 简化cobar的功能为单一的仅支持根据table的id-sequence来解析出对应的物理节点位置，并实施路由
* 由于id-sequence和物理节点的一一对应的性质，因此可以做到平滑增减机器，不需要重新启动cobar
* 支持主从自动切换
* 支持物理节点平滑迁移

##Usage

配置conf目录下的以下几个配置文件
* servers.xml
* schema.xml
* server.xml

###server.xml

```
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE cluster SYSTEM "servers.dtd">
<cluster>
    <groups>
        <group>
            <!--物理数据库节点分组-->
            <name>group1</name>
            <servers>
                <server>
                    <id>1</id>
                    <description></description>
                    <host>localhost</host>
                    <port>3306</port>
                    <db_name>test_1</db_name>
                    <user>root</user>
                    <password></password>
                </server>
                <server>
                    <id>2</id>
                    <description></description>
                    <host>localhost</host>
                    <port>3306</port>
                    <db_name>test_2</db_name>
                    <user>root</user>
                    <password></password>
                </server>
                <server>
                    <id>3</id>
                    <description></description>
                    <host>localhost</host>
                    <port>3306</port>
                    <db_name>test_3</db_name>
                    <user>root</user>
                    <password></password>
                </server>
            </servers>
        </group>
    </groups>
    <tables>
        <!--分表配置-->
        <table>
            <table_name>user</table_name>
            <split_columns> <!-- 分表字段 -->
                <column>id</column>
            </split_columns>
            <master_group>group1</master_group><!--主分组-->
            <brother_group>group1</brother_group><!-- 热备份的group -->
        </table>
        <table>
            <table_name>account</table_name>
            <split_columns>
                <column>id</column>
            </split_columns>
            <master_group>group1</master_group>
        </table>
    </tables>
</cluster>
```

###schema.xml
```
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE cobar:schema SYSTEM "myschema.dtd">
<cobar:schema xmlns:cobar="http://cobar.alibaba.com/">
    <schema name="dbtest" /><!--对外的database name-->
</cobar:schema>
```

###server.xml
```
<?xml version="1.0" encoding="UTF-8"?>
<!--
 - Copyright 1999-2012 Alibaba Group.
 -  
 - Licensed under the Apache License, Version 2.0 (the "License");
 - you may not use this file except in compliance with the License.
 - You may obtain a copy of the License at
 -  
 -      http://www.apache.org/licenses/LICENSE-2.0
 -  
 - Unless required by applicable law or agreed to in writing, software
 - distributed under the License is distributed on an "AS IS" BASIS,
 - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 - See the License for the specific language governing permissions and
 - limitations under the License.
-->
<!DOCTYPE cobar:server SYSTEM "server.dtd">
<cobar:server xmlns:cobar="http://cobar.alibaba.com/">
  
  <!-- 系统参数定义，服务端口、管理端口，处理器个数、线程池等。 -->
  <!--
  <system>
    <property name="serverPort">8066</property>
    <property name="managerPort">9066</property>
    <property name="initExecutor">16</property>
    <property name="timerExecutor">4</property>
    <property name="managerExecutor">4</property>
    <property name="processors">4</property>
    <property name="processorHandler">8</property>
    <property name="processorExecutor">8</property>
    <property name="clusterHeartbeatUser">_HEARTBEAT_USER_</property>
    <property name="clusterHeartbeatPass">_HEARTBEAT_PASS_</property>
  </system>
  -->

  <!-- 用户访问定义，用户名、密码、schema等信息。 -->
  <user name="test">
    <property name="password">test</property>
    <property name="schemas">dbtest</property>
  </user>
  <!--
  <user name="root">
    <property name="password"></property>
  </user>
  -->

  <!-- 集群列表定义，指定集群节点的主机和权重，用于集群间的心跳和客户端负载均衡。 -->
  <!--<cluster>-->
    <!--<node name="cobar1">-->
      <!--<property name="host">127.0.0.1</property>-->
      <!--<property name="weight">1</property>-->
    <!--</node>-->
  <!--</cluster>-->
   
  <!-- 隔离区定义，可以限定某个主机上只允许某个用户登录。 -->
  <!--
  <quarantine>
    <host name="1.2.3.4">
      <property name="user">test</property>
    </host>
  </quarantine>
  -->

</cobar:server>

```

##id-generator id生成算法

参考mongodb的ID生成策略，位序为bigendian

12bytes位： |0|1|2|3|4|5|6|7|8|9|10|11|

最后以16进制字符串存储,也就是24个字符(如果将来有需要也可以直接12bytes存储,目前为了可读性使用24字符)

各位内容如下：
 * time: [0,3] 当前时间戳
 * inc: [4,6] 一个在[0,2^24)之间不断循环的数字
 * gid: [7,8] generator的id，由id-generator manager在创建各个generator子进程时进行分配，保证每一个generator子进程分配的id不同
 * machineId: [9,11] 在本系统中是serverId

例如:
     5413f8310000021ff4000001
     
具体machine-id如何选择等请参考id-generator项目：[https://github.com/nyankosama/id-generator](https://github.com/nyankosama/id-generator)


## Original Wiki

https://github.com/alibaba/cobar/wiki
