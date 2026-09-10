# -*- coding: utf-8 -*-
"""
W4D3 压测结果分析：
1. 通用指标：按 label 统计 请求数 / 成功率 / 平均 / P99 / 最大 / 吞吐(QPS)
2. 超卖验证：统计响应体含 orderNo 的成功单数、含"库存不足"的拒绝数，
   输出 DB 库存核对 SQL 供下一步执行

用法：python scripts/bench/analyze.py results_qps.jtl
      python scripts/bench/analyze.py results_oversell.jtl --oversell
"""
import argparse
import csv
import statistics
from collections import defaultdict


def read_jtl(path):
    """JTL 首行是列名，逐行解析（responseData 里可能含换行，csv 模块按引号规则处理）"""
    with open(path, encoding="utf-8", newline="") as f:
        rows = list(csv.DictReader(f))
    # 去掉空列名行（JMeter 偶发）
    return [r for r in rows if "label" in r and r["label"]]


def metrics(rows, label_filter=None):
    rows = [r for r in rows if label_filter is None or r["label"] == label_filter]
    if not rows:
        return None
    times = sorted(int(r["elapsed"]) for r in rows)
    n = len(times)
    err = sum(1 for r in rows if r["success"] != "true")
    return {
        "requests": n,
        "errors": err,
        "error_rate": f"{err / n * 100:.2f}%",
        "avg_ms": statistics.mean(times),
        "p99_ms": times[int(n * 0.99) - 1],
        "max_ms": times[-1],
        "min_ms": times[0],
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("jtl")
    ap.add_argument("--oversell", action="store_true", help="超卖验证模式：统计成功/拒绝单数")
    args = ap.parse_args()

    rows = read_jtl(args.jtl)

    # 总时长（从第一个请求到最后一个请求）
    t0, t1 = min(int(r["timeStamp"]) for r in rows), max(int(r["timeStamp"]) + int(r["elapsed"]) for r in rows)
    duration = (t1 - t0) / 1000

    print(f"=== {args.jtl} ===")
    print(f"总请求: {len(rows)}  总时长: {duration:.1f}s")
    labels = sorted({r["label"] for r in rows})
    for label in labels:
        m = metrics(rows, label)
        if not m:
            continue
        qps = m["requests"] / duration
        print(f"[{label}] 请求 {m['requests']}  错误 {m['errors']}({m['error_rate']})  "
              f"avg {m['avg_ms']:.1f}ms  p99 {m['p99_ms']}ms  min {m['min_ms']}ms  max {m['max_ms']}ms  QPS {qps:.1f}")

    if args.oversell:
        succ = sum(1 for r in rows if "orderNo" in r.get("responseData", ""))
        insufficient = sum(1 for r in rows if "库存不足" in r.get("responseData", ""))
        other = len(rows) - succ - insufficient
        print(f"\n=== 超卖验证 ===")
        print(f"成功下单(响应含 orderNo): {succ}")
        print(f"库存不足拒绝: {insufficient}")
        print(f"其他(异常/错误): {other}")
        print("期望：成功 = 100（SKU 预置 100 库存），拒绝 = 900，其他 = 0")
        print("下一步核对 DB：SELECT stock FROM t_product_sku WHERE id = <SKU>;  → 期望 0")
        print("核对 Redis：GET stock:sku:<SKU>  → 期望 0")


if __name__ == "__main__":
    main()
