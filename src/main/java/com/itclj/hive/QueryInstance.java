package com.itclj.hive;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.security.sasl.SaslException;
import org.apache.hive.service.cli.RowSet;
import org.apache.hive.service.cli.RowSetFactory;
import org.apache.hive.service.cli.thrift.TCLIService;
import org.apache.hive.service.cli.thrift.TCancelOperationReq;
import org.apache.hive.service.cli.thrift.TColumn;
import org.apache.hive.service.cli.thrift.TColumnDesc;
import org.apache.hive.service.cli.thrift.TExecuteStatementReq;
import org.apache.hive.service.cli.thrift.TExecuteStatementResp;
import org.apache.hive.service.cli.thrift.TFetchOrientation;
import org.apache.hive.service.cli.thrift.TFetchResultsReq;
import org.apache.hive.service.cli.thrift.TFetchResultsResp;
import org.apache.hive.service.cli.thrift.TGetOperationStatusReq;
import org.apache.hive.service.cli.thrift.TGetOperationStatusResp;
import org.apache.hive.service.cli.thrift.TGetResultSetMetadataReq;
import org.apache.hive.service.cli.thrift.TGetResultSetMetadataResp;
import org.apache.hive.service.cli.thrift.TOpenSessionResp;
import org.apache.hive.service.cli.thrift.TOperationHandle;
import org.apache.hive.service.cli.thrift.TOperationState;
import org.apache.hive.service.cli.thrift.TProtocolVersion;
import org.apache.hive.service.cli.thrift.TRowSet;
import org.apache.hive.service.cli.thrift.TSessionHandle;
import org.apache.hive.service.cli.thrift.TTableSchema;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create by lujun.chen on 2018/11/15
 */
public class QueryInstance {

  private static Logger logger = LoggerFactory.getLogger(QueryInstance.class);

  private static String host = "10.250.80.43";
  private static int port = 10000;
  private static String username = "hive";
  private static String passsword = "hive";
  private static TTransport transport;
  private static TCLIService.Client client;
  private TOperationState tOperationState = null;
  private Map<String, Object> resultMap = new HashMap<String, Object>();

  static {
    try {
      transport = QueryTool.getSocketInstance(host, port);
      client = new TCLIService.Client(new TBinaryProtocol(transport));
      transport.open();

    } catch (TTransportException | IOException e) {
      logger.error("hive collection error!", e);
    }
  }

  /**
   * 提交查询
   *
   * @param command
   * @return
   * @throws Exception
   */
  public TOperationHandle submitQuery(String command) throws Exception {

    TOperationHandle tOperationHandle;
    TExecuteStatementResp resp = null;

    //TOpenSessionResp sessionResp = QueryTool.openSession(client, username, passsword) ;
    //TSessionHandle sessHandle = sessionResp.getSessionHandle();
    TOpenSessionResp sessionResp = QueryTool.openSession(client);
    TSessionHandle sessHandle = sessionResp.getSessionHandle();
    TExecuteStatementReq execReq = new TExecuteStatementReq(sessHandle, command);
    // 异步运行
    execReq.setRunAsync(true);
    // 执行sql
    resp = client.ExecuteStatement(execReq);// 执行语句

    tOperationHandle = resp.getOperationHandle();// 获取执行的handle

    if (tOperationHandle == null) {
      //语句执行异常时，会把异常信息放在resp.getStatus()中。
      throw new Exception(resp.getStatus().getErrorMessage());
    }
    return tOperationHandle;
  }


	/*
	//	获取查询日志 hive.version： 0.12.0-cdh5.0.1(5.3.10前，使用此方法获取查询日志)
    @Override
	public String getQueryLog(TOperationHandle tOperationHandle) throws Exception {
		if(tOperationHandle!=null){
			TGetLogReq tGetLogReq = new TGetLogReq(tOperationHandle);
			TGetLogResp logResp = client.GetLog(tGetLogReq);
			log = logResp.getLog();
		}

		return log;
	}*/

  /**
   * CDH5.4.0及之后版本 使用此方法获取查询日志
   *
   * @param tOperationHandle
   * @return
   * @throws Exception
   */
  public String getQueryLog(TOperationHandle tOperationHandle) throws Exception {
    String log = "";
    if (tOperationHandle != null) {
      StringBuffer sbLog = new StringBuffer();
      TFetchResultsReq fetchReq = new TFetchResultsReq(tOperationHandle,
          TFetchOrientation.FETCH_NEXT, 1000);
      fetchReq.setFetchType((short) 1); //主要需要设置为1
      TFetchResultsResp resp = client.FetchResults(fetchReq);
      TRowSet rs = resp.getResults();

      if (null != rs) {
        RowSet rowSet = RowSetFactory.create(rs, TProtocolVersion.HIVE_CLI_SERVICE_PROTOCOL_V7);
        for (Object[] row : rowSet) {
          sbLog.append(String.valueOf(row[0])).append("\n");
        }
      }
      log = sbLog.toString();
    }

    return log;
  }

  /*
   * 获取查询状态
   * 执行状态在TOperationState 类中，包括：
   * INITIALIZED_STATE(0),
   * RUNNING_STATE(1),
   * FINISHED_STATE(2),
   * CANCELED_STATE(3),
   * CLOSED_STATE(4),
   * ERROR_STATE(5),
   * UKNOWN_STATE(6),
   * PENDING_STATE(7);
   */
  public TOperationState getQueryHandleStatus(
      TOperationHandle tOperationHandle) throws Exception {

    if (tOperationHandle != null) {
      TGetOperationStatusReq statusReq = new TGetOperationStatusReq(
          tOperationHandle);
      TGetOperationStatusResp statusResp = client
          .GetOperationStatus(statusReq);

      tOperationState = statusResp.getOperationState();

    }
    return tOperationState;
  }

  /**
   * 获取查询字段名
   *
   * @param tOperationHandle
   * @return
   * @throws Throwable
   */
  public List<String> getColumns(TOperationHandle tOperationHandle)
      throws Throwable {
    TGetResultSetMetadataResp metadataResp;
    TGetResultSetMetadataReq metadataReq;
    TTableSchema tableSchema;
    metadataReq = new TGetResultSetMetadataReq(tOperationHandle);
    metadataResp = client.GetResultSetMetadata(metadataReq);
    List<TColumnDesc> columnDescs;
    List<String> columns = null;
    tableSchema = metadataResp.getSchema();
    if (tableSchema != null) {
      columnDescs = tableSchema.getColumns();
      columns = new ArrayList<String>();
      for (TColumnDesc tColumnDesc : columnDescs) {
        columns.add(tColumnDesc.getColumnName());
      }
    }
    return columns;
  }

  /**
   * 获取执行结果 select语句 得到的结果为以列的形式返回
   */

  public List<Object> getResults(TOperationHandle tOperationHandle) throws Throwable {
    TFetchResultsReq fetchReq = new TFetchResultsReq();
    fetchReq.setOperationHandle(tOperationHandle);
    fetchReq.setMaxRows(1000);
    TFetchResultsResp re = client.FetchResults(fetchReq);
    TRowSet rowSet = re.getResults();
    if (rowSet == null) {
      return null;
    }
    List<TColumn> list = re.getResults().getColumns();
    List<Object> list_row = new ArrayList<Object>();
    for (TColumn field : list) {
      if (field.isSetStringVal()) {
        list_row.add(field.getStringVal().getValues());
      } else if (field.isSetDoubleVal()) {
        list_row.add(field.getDoubleVal().getValues());
      } else if (field.isSetI16Val()) {
        list_row.add(field.getI16Val().getValues());
      } else if (field.isSetI32Val()) {
        list_row.add(field.getI32Val().getValues());
      } else if (field.isSetI64Val()) {
        list_row.add(field.getI64Val().getValues());
      } else if (field.isSetBoolVal()) {
        list_row.add(field.getBoolVal().getValues());
      } else if (field.isSetByteVal()) {
        list_row.add(field.getByteVal().getValues());
      }
    }
		/*for(Object obj:list_row){
			System.out.println(obj);
		}*/
    return list_row;
  }

  /**
   * 转换查询结果，由原来的列转为行
   *
   * @param objs
   * @return
   */
  public List<Object> toResults(List<Object> objs) {
    List<Object> rets = new ArrayList<Object>();

    if (objs != null) {
      List row = null;
      List list = (List) objs.get(0);
      int rowCnt = list.size();
      for (int i = 0; i < rowCnt; i++) {
        rets.add(new ArrayList());
      }
      for (int i = 0; i < objs.size(); i++) {
        list = (List) objs.get(i);
        for (int j = 0; j < rowCnt; j++) {
          ((List) rets.get(j)).add(list.get(j));
        }
      }

      System.out.println("---------------------------------");
      System.out.println(rets);
      System.out.println("---------------------------------");
    }

    return rets;
  }

  /**
   * 取消查询
   *
   * @param tOperationHandle
   * @throws Throwable
   */
  public void cancelQuery(TOperationHandle tOperationHandle) throws Throwable {
    if (tOperationState != TOperationState.FINISHED_STATE) {
      TCancelOperationReq cancelOperationReq = new TCancelOperationReq();
      cancelOperationReq.setOperationHandle(tOperationHandle);
      client.CancelOperation(cancelOperationReq);
    }
  }

}
