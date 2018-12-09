package com.itclj;

import com.itclj.hive.QueryInstance;
import com.itclj.kerberos.KerberosLogin;
import java.util.List;
import org.apache.hive.service.cli.thrift.TOperationHandle;
import org.apache.hive.service.cli.thrift.TOperationState;

/**
 * Create by lujun.chen on 2018/11/15
 */
public class ItcljApplication {

  public static void main(String[] args) {
    try {

      KerberosLogin kerberosLogin = new KerberosLogin();
      kerberosLogin.login();
      QueryInstance base = new QueryInstance();

      TOperationHandle handle = base.submitQuery(
          " select * from dbusdest.trade_info limit 2  ");//show databases //select count(*) from cdm_test1

      String log = base.getQueryLog(handle);
      System.out.println("LOG : " + log);

      while (base.getQueryHandleStatus(handle) == TOperationState.RUNNING_STATE) {
        Thread.sleep(5000);
        System.out.println("LOG : " + base.getQueryLog(handle));
      }

      System.out.println(base.getColumns(handle));
      List<Object> listRets = base.getResults(handle);
      base.toResults(listRets);
      for (Object obj : listRets) {
        System.out.println(obj);
      }
    } catch (Throwable e) {

      e.printStackTrace();
    }

  }
}
