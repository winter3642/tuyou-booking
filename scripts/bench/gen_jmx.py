# -*- coding: utf-8 -*-
"""
W4D3 JMeter 计划生成器：生成两个 jmx（GUI 里也能直接打开复用/截图）

场景一 qps.jmx：下单接口压测
    - 200 线程 / Ramp-up 20s / 每线程循环 50 次
    - 每轮「加购 → 查购物车(提取cartId) → 下单」三步，模拟真实用户下单闭环
    - 用户 token 参数化自 users_qps.csv

场景二 oversell.jmx：超卖验证（简历"超卖率 0"核心证据）
    - 1000 线程各下单 1 次（购物车已由 gen_users.py --cart 预置）
    - 断言在 analyze.py 里做：成功单数=100、DB 库存=0、无负库存

用法：python scripts/bench/gen_jmx.py [--base-url http://localhost:8082] [--sku 1]
"""
import argparse
from pathlib import Path

HEADER_MANAGER = """
        <HeaderManager guiclass="HeaderPanel" testclass="HeaderManager" testname="请求头" enabled="true">
          <collectionProp name="HeaderManager.headers">
            <elementProp name="" elementType="Header">
              <stringProp name="Header.name">Authorization</stringProp>
              <stringProp name="Header.value">Bearer ${token}</stringProp>
            </elementProp>
            <elementProp name="" elementType="Header">
              <stringProp name="Header.name">Content-Type</stringProp>
              <stringProp name="Header.value">application/json</stringProp>
            </elementProp>
          </collectionProp>
        </HeaderManager>
        <hashTree/>
"""

CSV_DATASET = """
        <CSVDataSet guiclass="TestBeanGUI" testclass="CSVDataSet" testname="用户CSV" enabled="true">
          <stringProp name="filename">scripts/bench/{csv}</stringProp>
          <stringProp name="fileEncoding">UTF-8</stringProp>
          <stringProp name="variableNames">{vars}</stringProp>
          <boolProp name="ignoreFirstLine">true</boolProp>
          <stringProp name="delimiter">,</stringProp>
          <boolProp name="quotedData">false</boolProp>
          <boolProp name="recycle">true</boolProp>
          <boolProp name="stopThread">false</boolProp>
          <stringProp name="shareMode">shareMode.all</stringProp>
        </CSVDataSet>
        <hashTree/>
"""


def sampler(name, path, method, body=None, extract_cart=False):
    body_xml = ""
    if body is not None:
        body_xml = f"""
          <boolProp name="HTTPSampler.postBodyRaw">true</boolProp>
          <elementProp name="HTTPsampler.Arguments" elementType="Arguments">
            <collectionProp name="Arguments.arguments">
              <elementProp name="" elementType="HTTPArgument">
                <boolProp name="HTTPArgument.always_encode">false</boolProp>
                <stringProp name="Argument.value">{body}</stringProp>
                <stringProp name="Argument.metadata">=</stringProp>
              </elementProp>
            </collectionProp>
          </elementProp>"""
    # 每个 sampler 后面必须跟一个 <hashTree/> 作为其子容器（JMX 树结构要求），
    # 需要提取器时把 JSONPostProcessor 放进这个子容器
    child_xml = ""
    if extract_cart:
        child_xml = f"""
          <JSONPostProcessor guiclass="JSONPostProcessorGui" testclass="JSONPostProcessor" testname="提取购物车ID" enabled="true">
            <stringProp name="JSONPostProcessor.referenceNames">cartId</stringProp>
            <stringProp name="JSONPostProcessor.jsonPathExprs">$.data[0].id</stringProp>
            <stringProp name="JSONPostProcessor.match_numbers">1</stringProp>
          </JSONPostProcessor>
          <hashTree/>"""
    return f"""
        <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="{name}" enabled="true">{body_xml}
          <stringProp name="HTTPSampler.domain"></stringProp>
          <stringProp name="HTTPSampler.port"></stringProp>
          <stringProp name="HTTPSampler.protocol"></stringProp>
          <stringProp name="HTTPSampler.contentEncoding">UTF-8</stringProp>
          <stringProp name="HTTPSampler.path">${{BASE_URL}}{path}</stringProp>
          <stringProp name="HTTPSampler.method">{method}</stringProp>
          <boolProp name="HTTPSampler.follow_redirects">true</boolProp>
          <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
        </HTTPSamplerProxy>
        <hashTree>{child_xml}
        </hashTree>"""


def plan(name, threads, ramp, loops, csv, vars_, samplers, base_url, sku, result_file):
    s = "".join(samplers)
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2" properties="5.0" jmeter="5.6.3">
  <hashTree>
    <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="{name}" enabled="true">
      <stringProp name="TestPlan.comments">途游预订 W4D3 压测（生成于 gen_jmx.py）</stringProp>
      <boolProp name="TestPlan.functional_mode">false</boolProp>
      <boolProp name="TestPlan.tearDown_on_shutdown">true</boolProp>
      <boolProp name="TestPlan.serialize_threadgroups">false</boolProp>
      <elementProp name="TestPlan.user_defined_variables" elementType="Arguments" guiclass="ArgumentsPanel" testclass="Arguments" testname="用户定义的变量" enabled="true">
        <collectionProp name="Arguments.arguments">
          <elementProp name="BASE_URL" elementType="Argument">
            <stringProp name="Argument.name">BASE_URL</stringProp>
            <stringProp name="Argument.value">{base_url}</stringProp>
            <stringProp name="Argument.metadata">=</stringProp>
          </elementProp>
          <elementProp name="SKU_ID" elementType="Argument">
            <stringProp name="Argument.name">SKU_ID</stringProp>
            <stringProp name="Argument.value">{sku}</stringProp>
            <stringProp name="Argument.metadata">=</stringProp>
          </elementProp>
        </collectionProp>
      </elementProp>
    </TestPlan>
    <hashTree>
      <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="压测线程组" enabled="true">
        <stringProp name="ThreadGroup.on_sample_error">continue</stringProp>
        <elementProp name="ThreadGroup.main_controller" elementType="LoopController" guiclass="LoopControlPanel" testclass="LoopController" testname="循环控制器" enabled="true">
          <boolProp name="LoopController.continue_forever">false</boolProp>
          <stringProp name="LoopController.loops">{loops}</stringProp>
        </elementProp>
        <stringProp name="ThreadGroup.num_threads">{threads}</stringProp>
        <stringProp name="ThreadGroup.ramp_time">{ramp}</stringProp>
        <boolProp name="ThreadGroup.scheduler">false</boolProp>
        <stringProp name="ThreadGroup.duration"></stringProp>
        <stringProp name="ThreadGroup.delay"></stringProp>
        <boolProp name="ThreadGroup.same_user_on_next_iteration">true</boolProp>
      </ThreadGroup>
      <hashTree>
{HEADER_MANAGER}
{CSV_DATASET.format(csv=csv, vars=vars_)}{s}
        <ResultCollector guiclass="SimpleDataWriter" testclass="ResultCollector" testname="结果JTL" enabled="true">
          <boolProp name="ResultCollector.error_logging">false</boolProp>
          <objProp>
            <name>saveConfig</name>
            <value class="SampleSaveConfiguration">
              <time>true</time>
              <latency>true</latency>
              <timestamp>true</timestamp>
              <success>true</success>
              <label>true</label>
              <code>true</code>
              <message>true</message>
              <threadName>true</threadName>
              <dataType>true</dataType>
              <encoding>false</encoding>
              <assertions>true</assertions>
              <subresults>false</subresults>
              <responseData>true</responseData>
              <samplerData>false</samplerData>
              <xml>false</xml>
              <fieldNames>true</fieldNames>
              <responseHeaders>false</responseHeaders>
              <requestHeaders>false</requestHeaders>
              <responseDataOnError>false</responseDataOnError>
              <saveAssertionResultsFailureMessage>true</saveAssertionResultsFailureMessage>
              <assertionsResultsToSave>0</assertionsResultsToSave>
              <bytes>true</bytes>
              <sentBytes>true</sentBytes>
              <url>true</url>
              <threadCounts>true</threadCounts>
              <idleTime>true</idleTime>
              <connectTime>true</connectTime>
            </value>
          </objProp>
          <stringProp name="filename">scripts/bench/{result_file}</stringProp>
        </ResultCollector>
        <hashTree/>
      </hashTree>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
"""


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--base-url", default="http://localhost:8082")
    ap.add_argument("--sku", type=int, default=1)
    args = ap.parse_args()

    bench = Path(__file__).parent

    # 场景一：下单 QPS（200 线程 × 50 轮，每轮 加购→查车→下单）
    qps = plan(
        name="下单接口压测（200并发×50轮）",
        threads=200, ramp=20, loops=50,
        csv="users_qps.csv", vars_="token",
        base_url=args.base_url, sku=args.sku, result_file="results_qps.jtl",
        samplers=[
            sampler("加购", "/api/cart", "POST", '{"skuId":${SKU_ID},"quantity":1}'),
            sampler("查购物车", "/api/cart", "GET", extract_cart=True),
            sampler("下单", "/api/orders", "POST", '{"cartIds":[${cartId}]}'),
        ])
    (bench / "qps.jmx").write_text(qps, encoding="utf-8")

    # 场景二：超卖验证（1000 线程 × 1 次，购物车预置，只调下单）
    oversell = plan(
        name="超卖验证（1000并发抢100库存）",
        threads=1000, ramp=10, loops=1,
        csv="users_oversell.csv", vars_="token,cartId",
        base_url=args.base_url, sku=args.sku, result_file="results_oversell.jtl",
        samplers=[
            sampler("下单(超卖验证)", "/api/orders", "POST", '{"cartIds":[${cartId}]}'),
        ])
    (bench / "oversell.jmx").write_text(oversell, encoding="utf-8")
    print("已生成 qps.jmx 与 oversell.jmx")


if __name__ == "__main__":
    main()
