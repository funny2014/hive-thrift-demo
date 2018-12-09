# hive-thrift-demo
hive-thrift-demo

最近在做一个大数据查询平台，后端引擎有部分用了hive，通过thrift的方式连接hiveServer2，由于集群加了kerberos，所以实现thrift连接hiveServer2的时候需要加上kerberos认证。网上查了很多文章，写的thrift连接hive都没有kerberos，分享一下，以供需要通过thrift连接hiveService2并需要开启Kerberos认证的同学一个参考，以便能够快速解决问题。

kerberos认证hive连接关键配置
```aidl
    saslProperties.put("javax.security.sasl.qop", "auth");
    saslProperties.put("javax.security.sasl.server.authentication","true");
```